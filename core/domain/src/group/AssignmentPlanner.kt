package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * Draws who gives to whom: the participants are shuffled and each one gives to the next in that
 * order, the last giving to the first. That always yields one closed chain through everybody, so
 * nobody draws themself and nobody is stuck in a pair or a small side circle.
 */
class AssignmentPlanner(private val shuffler: Shuffler) {

    fun plan(participants: List<Username>): List<Assignment> {
        require(participants.size >= 2) { "At least two participants are needed to assign gifts" }
        require(participants.toSet().size == participants.size) { "Participants must be distinct" }

        val order = shuffler.shuffled(participants)
        check(order.size == participants.size && order.toSet() == participants.toSet()) {
            "The shuffler must return a permutation of the participants"
        }
        return order.indices.map { index -> Assignment(santa = order[index], receiver = order[(index + 1) % order.size]) }
    }
}
