package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * Draws who gives to whom: the participants are put in a random order and each one gives to the next,
 * the last giving to the first. That always yields one closed chain through everybody, so nobody
 * draws themself and nobody is stuck in a pair or a small side circle. With draw [rules] the order is
 * searched for instead: people who must give to each other stay together, and nobody is put before
 * someone they must not give to. Without rules the first order the shuffler gives is the draw.
 */
class AssignmentPlanner(private val shuffler: Shuffler) {

    fun plan(participants: List<Username>, rules: List<DrawRule> = emptyList()): List<Assignment> {
        require(participants.size >= 2) { "At least two participants are needed to assign gifts" }
        require(participants.toSet().size == participants.size) { "Participants must be distinct" }

        if (rules.isEmpty()) return chainOf(shuffler.shuffled(participants), participants)

        val chain = DrawSolver(participants, rules.map { NewDrawRule(it.giver, it.receiver, it.kind) })
            .solve(DrawShuffles.With(shuffler))
            ?: throw DomainException.RulesConflict("No draw can satisfy the rules of this group: " + rules.joinToString("; ") { it.describe() } + ".")
        return chain
    }

    private fun chainOf(order: List<Username>, participants: List<Username>): List<Assignment> {
        check(order.size == participants.size && order.toSet() == participants.toSet()) {
            "The shuffler must return a permutation of the participants"
        }
        return order.indices.map { index -> Assignment(santa = order[index], receiver = order[(index + 1) % order.size]) }
    }
}
