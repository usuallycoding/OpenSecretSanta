package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.Letter
import com.usuallycoding.opensecretsanta.domain.gift.LetterRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.Username

/** In-memory letters that apply the same rules as the real repository (Active only, numbering, cap). */
class InMemoryLetters(private val groups: InMemoryGroups) : LetterRepository {
    private data class Row(val groupId: Long, val receiver: Username, val letter: Letter)

    private val rows = mutableListOf<Row>()

    override fun send(groupId: Long, receiver: Username, title: String, text: String, atMillis: Long, maxLetters: Int): Letter {
        val group = groups.find(groupId) ?: throw DomainException.NotFound("Group not found.")
        if (group.state != GroupState.ACTIVE) throw DomainException.InvalidState("Letters can only be sent while the group is active.")
        if (groups.santaOf(groupId, receiver) == null) throw DomainException.NotFound("Group not found.")
        val mine = rows.filter { it.groupId == groupId && it.receiver == receiver }
        if (mine.size >= maxLetters) throw DomainException.InvalidState("The letter limit has been reached.")
        val letter = Letter((mine.maxOfOrNull { it.letter.number } ?: 0) + 1, title, text, atMillis)
        rows += Row(groupId, receiver, letter)
        return letter
    }

    override fun sentBy(groupId: Long, receiver: Username) =
        rows.filter { it.groupId == groupId && it.receiver == receiver }.sortedBy { it.letter.number }.map { it.letter }

    override fun sentToSanta(groupId: Long, santa: Username): List<Letter> {
        val receiver = groups.receiverOf(groupId, santa) ?: return emptyList()
        return sentBy(groupId, receiver)
    }
}
