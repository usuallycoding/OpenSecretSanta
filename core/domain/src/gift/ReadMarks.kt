package com.usuallycoding.opensecretsanta.domain.gift

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistRepository

/** The three things of a receiver that a Santa keeps track of reading. */
enum class ReadKind(val key: String) {
    WISHLIST("wishlist"),
    ANSWERS("answers"),
    LETTERS("letters"),
    ;

    companion object {
        /** The kind named in a route, or null for a name that is not one. */
        fun of(raw: String?): ReadKind? = entries.firstOrNull { it.key == raw }
    }
}

/** How much of one kind is unread, and the newest time seen of it (null when the Santa never marked it). */
data class ReadMark(val unread: Int, val seenUpToMillis: Long?)

/** A Santa's marks for the three kinds. Nothing in it can name anyone. */
data class ReadMarks(val wishlist: ReadMark, val answers: ReadMark, val letters: ReadMark)

/** Storage for the newest time a Santa has seen of each kind, per group; it goes with the group. */
interface ReadMarkRepository {
    fun seenUpTo(groupId: Long, santa: Username): Map<ReadKind, Long>

    /** Keeps the later of the stored time and [atMillis], and returns the time that is stored afterwards. */
    fun markSeen(groupId: Long, santa: Username, kind: ReadKind, atMillis: Long): Long
}

/**
 * Counts what a Santa has not seen, from the receiver's wishlist, the Santa's questions and the letters, by the times in them: an
 * item counts once however often it changed, an answer counts when it was given or changed after the time seen, a letter when it
 * was sent after it, and with no time seen everything counts. What was deleted is not there to count.
 */
internal class ReadMarkCounter(
    private val groups: GroupRepository,
    private val marks: ReadMarkRepository,
    private val wishlists: WishlistRepository,
    private val questions: QuestionRepository,
    private val letters: LetterRepository,
) {
    fun marksOf(group: Group, santa: Username): ReadMarks {
        val receiver = groups.receiverOf(group.id, santa) ?: throw DomainException.NotFound("Group not found.")
        val seen = marks.seenUpTo(group.id, santa)
        fun after(kind: ReadKind): Long = seen[kind] ?: Long.MIN_VALUE
        return ReadMarks(
            wishlist = ReadMark(
                wishlists.list(group.id, receiver).count { it.createdAtMillis > after(ReadKind.WISHLIST) || it.updatedAtMillis > after(ReadKind.WISHLIST) },
                seen[ReadKind.WISHLIST],
            ),
            answers = ReadMark(
                questions.askedBy(group.id, santa).count { (it.answerUpdatedAtMillis ?: Long.MIN_VALUE) > after(ReadKind.ANSWERS) },
                seen[ReadKind.ANSWERS],
            ),
            letters = ReadMark(
                letters.sentToSanta(group.id, santa).count { it.sentAtMillis > after(ReadKind.LETTERS) },
                seen[ReadKind.LETTERS],
            ),
        )
    }
}

private fun Group.requireDrawn() {
    if (state != GroupState.ACTIVE && state != GroupState.ARCHIVED) {
        throw DomainException.InvalidState("Reading starts once the group is active.")
    }
}

/** The participant, as the Santa of their receiver, reads what they have not yet seen of it. Only their own marks come back. */
class GetMyReadMarks(
    private val loadParticipantGroup: ParticipantGroupLoader,
    groups: GroupRepository,
    marks: ReadMarkRepository,
    wishlists: WishlistRepository,
    questions: QuestionRepository,
    letters: LetterRepository,
) {
    private val counter = ReadMarkCounter(groups, marks, wishlists, questions, letters)

    operator fun invoke(user: User, groupId: Long): ReadMarks {
        val group = loadParticipantGroup(user, groupId)
        group.requireDrawn()
        return counter.marksOf(group, user.username)
    }
}

/**
 * The Santa says they have seen everything of one kind up to a time. The later of that and what is stored is kept, and never a time
 * later than now, so a mark can neither go back nor hide what has not happened yet. Works in an Archived group too: it is the
 * Santa's own state, not the group's.
 */
class MarkRead(
    private val loadParticipantGroup: ParticipantGroupLoader,
    groups: GroupRepository,
    private val marks: ReadMarkRepository,
    wishlists: WishlistRepository,
    questions: QuestionRepository,
    letters: LetterRepository,
    private val clock: Clock,
) {
    private val counter = ReadMarkCounter(groups, marks, wishlists, questions, letters)

    operator fun invoke(user: User, groupId: Long, kind: ReadKind, seenUpToMillis: Long): ReadMark {
        val group = loadParticipantGroup(user, groupId)
        group.requireDrawn()
        counter.marksOf(group, user.username) // a participant with no receiver has nothing to mark
        marks.markSeen(group.id, user.username, kind, minOf(seenUpToMillis, clock.nowMillis()))
        val all = counter.marksOf(group, user.username)
        return when (kind) {
            ReadKind.WISHLIST -> all.wishlist
            ReadKind.ANSWERS -> all.answers
            ReadKind.LETTERS -> all.letters
        }
    }
}
