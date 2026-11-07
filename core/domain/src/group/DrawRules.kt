package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Username

/** What a draw rule asks: that [giver] gives to [receiver] ([MUST]), or does not ([NEVER]). */
enum class RuleKind(val storedName: String) {
    MUST("must"),
    NEVER("never");

    companion object {
        fun fromStored(name: String): RuleKind? = entries.firstOrNull { it.storedName == name }
    }
}

/** A rule as the owner asks for it, before it is stored. */
data class NewDrawRule(val giver: Username, val receiver: Username, val kind: RuleKind) {
    fun describe(): String = when (kind) {
        RuleKind.MUST -> "$giver must give to $receiver"
        RuleKind.NEVER -> "$giver must not give to $receiver"
    }
}

/** A stored rule of a group. Rules are one-way: a rule about A giving to B says nothing about B giving to A. */
data class DrawRule(val id: Long, val giver: Username, val receiver: Username, val kind: RuleKind) {
    fun describe(): String = NewDrawRule(giver, receiver, kind).describe()
}

/** The rules of one group, with the checks that keep them satisfiable. */
object DrawRules {
    /** Fewer draws than this count as "few". */
    const val FEW_DRAWS = 10

    /**
     * Checks that [added] can join [existing] for a group of [participants]: the names are participants, nobody
     * gives to themself, there are no repeats or contradictions, and some draw still satisfies everything.
     * Throws `ValidationFailed` for bad names and `RulesConflict`, naming the rules involved, for the rest.
     */
    fun checkAddition(participants: List<Username>, existing: List<DrawRule>, added: List<NewDrawRule>) {
        for (rule in added) {
            if (rule.giver !in participants) throw DomainException.ValidationFailed("giver", "${rule.giver} is not a participant of the group.")
            if (rule.receiver !in participants) throw DomainException.ValidationFailed("receiver", "${rule.receiver} is not a participant of the group.")
            if (rule.giver == rule.receiver) throw DomainException.ValidationFailed("receiver", "A rule needs two different people.")
        }
        val all = existing.map { NewDrawRule(it.giver, it.receiver, it.kind) } + added
        val seen = mutableSetOf<NewDrawRule>()
        for (rule in all) {
            if (!seen.add(rule)) throw DomainException.RulesConflict("The rule \"${rule.describe()}\" is already there.")
        }
        val musts = all.filter { it.kind == RuleKind.MUST }
        val nevers = all.filter { it.kind == RuleKind.NEVER }.map { it.giver to it.receiver }.toSet()
        musts.firstOrNull { (it.giver to it.receiver) in nevers }?.let { must ->
            throw DomainException.RulesConflict("\"${must.describe()}\" contradicts \"${NewDrawRule(must.giver, must.receiver, RuleKind.NEVER).describe()}\".")
        }
        musts.groupBy { it.giver }.values.firstOrNull { it.size > 1 }?.let { pair ->
            throw DomainException.RulesConflict("${pair[0].giver} cannot give to two people: \"${pair[0].describe()}\" and \"${pair[1].describe()}\".")
        }
        musts.groupBy { it.receiver }.values.firstOrNull { it.size > 1 }?.let { pair ->
            throw DomainException.RulesConflict("${pair[0].receiver} cannot receive from two people: \"${pair[0].describe()}\" and \"${pair[1].describe()}\".")
        }
        if (all.isEmpty()) return
        if (DrawSolver(participants, all).solve(DrawShuffles.None) == null) {
            val circle = DrawSolver(participants, all).mustCircle()
            throw DomainException.RulesConflict(
                if (circle != null) {
                    "These rules would make ${circle.joinToString(", ")} only give to each other: " + describeCircle(musts, circle)
                } else {
                    "No draw can satisfy all the rules: " + all.joinToString("; ") { it.describe() } + "."
                },
            )
        }
    }

    private fun describeCircle(musts: List<NewDrawRule>, circle: List<Username>): String =
        musts.filter { it.giver in circle }.joinToString("; ") { it.describe() } + "."

    /** True when rules exist and fewer than [FEW_DRAWS] different draws satisfy them. */
    fun fewDraws(participants: List<Username>, rules: List<DrawRule>): Boolean =
        rules.isNotEmpty() && DrawSolver(participants, rules.map { NewDrawRule(it.giver, it.receiver, it.kind) }).count(FEW_DRAWS) < FEW_DRAWS
}

/** How the solver picks among candidates; [None] tries them in the given order, for checking and counting. */
internal sealed interface DrawShuffles {
    data object None : DrawShuffles
    class With(val shuffler: Shuffler) : DrawShuffles
}

/**
 * Finds one closed chain through all [participants] that has every `must` edge and no `never` edge.
 * The musts are joined first into fixed pieces, and the search orders the pieces round a circle,
 * trying the candidates in the order the shuffler gives, and giving up after [STEP_LIMIT] steps.
 */
internal class DrawSolver(private val participants: List<Username>, rules: List<NewDrawRule>) {
    private val nextOf: Map<Username, Username> = rules.filter { it.kind == RuleKind.MUST }.associate { it.giver to it.receiver }
    private val never: Set<Pair<Username, Username>> = rules.filter { it.kind == RuleKind.NEVER }.map { it.giver to it.receiver }.toSet()

    /** A run of people who must give one to the next. */
    private class Piece(val members: List<Username>) {
        val head get() = members.first()
        val tail get() = members.last()
    }

    /** The people of a circle that the musts close without including everybody, or null when there is none. */
    fun mustCircle(): List<Username>? {
        val hasIncoming = nextOf.values.toSet()
        val inPieces = mutableSetOf<Username>()
        for (start in participants.filter { it !in hasIncoming }) {
            var current: Username? = start
            while (current != null) {
                inPieces += current
                current = nextOf[current]
            }
        }
        val leftover = participants.firstOrNull { it !in inPieces } ?: return null
        val circle = mutableListOf<Username>()
        var current = leftover
        do {
            circle += current
            current = nextOf.getValue(current)
        } while (current != leftover)
        return circle.takeIf { it.size < participants.size }
    }

    private fun pieces(): List<Piece>? {
        val hasIncoming = nextOf.values.toSet()
        val pieces = participants.filter { it !in hasIncoming }.map { start ->
            val members = mutableListOf<Username>()
            var current: Username? = start
            while (current != null) {
                members += current
                current = nextOf[current]
            }
            Piece(members)
        }
        // somebody outside every piece is on a circle of musts: that is only a draw when the circle is everybody (no pieces
        // at all, and an empty list stands for that), and otherwise no draw exists (null)
        if (pieces.sumOf { it.members.size } == participants.size) return pieces
        val circle = mustCircle()
        return if (circle == null) emptyList() else null
    }

    fun solve(shuffles: DrawShuffles): List<Assignment>? {
        if (participants.size < 2) return null
        val pieces = pieces()
        if (pieces == null) return null
        if (pieces.isEmpty()) {
            // the musts alone make the one chain through everybody
            val chain = participants.first().let { first -> generateSequence(first) { nextOf[it]?.takeIf { next -> next != first } } }.toList()
            return chain.indices.map { Assignment(chain[it], chain[(it + 1) % chain.size]) }
        }
        var steps = 0
        val order = mutableListOf<Piece>()
        val used = BooleanArray(pieces.size)

        fun candidates(): List<Int> {
            val free = pieces.indices.filter { !used[it] }
            return when (shuffles) {
                DrawShuffles.None -> free
                is DrawShuffles.With -> {
                    val shuffled = shuffles.shuffler.shuffled(free)
                    check(shuffled.size == free.size && shuffled.toSet() == free.toSet()) { "The shuffler must return a permutation of the participants" }
                    shuffled
                }
            }
        }

        fun fits(from: Piece, to: Piece) = (from.tail to to.head) !in never

        fun extend(): Boolean {
            if (order.size == pieces.size) return fits(order.last(), order.first())
            for (index in candidates()) {
                if (++steps > STEP_LIMIT) return false
                if (!fits(order.last(), pieces[index])) continue
                used[index] = true
                order += pieces[index]
                if (extend()) return true
                order.removeAt(order.lastIndex)
                used[index] = false
            }
            return false
        }

        val firstChoices = when (shuffles) {
            DrawShuffles.None -> listOf(0)
            is DrawShuffles.With -> candidates().take(1)
        }
        val first = firstChoices.first()
        used[first] = true
        order += pieces[first]
        if (!extend()) return null
        val chain = order.flatMap { it.members }
        return chain.indices.map { Assignment(chain[it], chain[(it + 1) % chain.size]) }
    }

    /** How many different draws satisfy the rules, counting no further than [limit]. */
    fun count(limit: Int): Int {
        if (participants.size < 2) return 0
        val pieces = pieces() ?: return 0
        if (pieces.isEmpty()) return 1
        var found = 0
        var steps = 0
        val order = mutableListOf(pieces[0])
        val used = BooleanArray(pieces.size).also { it[0] = true }

        fun fits(from: Piece, to: Piece) = (from.tail to to.head) !in never

        fun extend() {
            if (found >= limit || steps > STEP_LIMIT) return
            if (order.size == pieces.size) {
                if (fits(order.last(), order.first())) found++
                return
            }
            for (index in pieces.indices) {
                if (used[index] || !fits(order.last(), pieces[index])) continue
                steps++
                used[index] = true
                order += pieces[index]
                extend()
                order.removeAt(order.lastIndex)
                used[index] = false
                if (found >= limit || steps > STEP_LIMIT) return
            }
        }
        extend()
        return found
    }

    companion object {
        const val STEP_LIMIT = 200_000
    }
}
