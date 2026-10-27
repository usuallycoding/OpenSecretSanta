package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.AskedQuestion
import com.usuallycoding.opensecretsanta.domain.gift.QuestionRepository
import com.usuallycoding.opensecretsanta.domain.gift.ReceivedQuestion
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.Username

/** In-memory questions that apply the same rules as the real repository (Active only, numbering, cap). */
class InMemoryQuestions(private val groups: InMemoryGroups) : QuestionRepository {
    private data class Row(
        val groupId: Long,
        val santa: Username,
        val number: Int,
        val text: String,
        val askedAt: Long,
        var answer: String? = null,
        var answeredAt: Long? = null,
        var answerUpdatedAt: Long? = null,
    )

    private val rows = mutableListOf<Row>()

    private fun requireActive(groupId: Long) {
        val group = groups.find(groupId) ?: throw DomainException.NotFound("Group not found.")
        if (group.state != GroupState.ACTIVE) throw DomainException.InvalidState("Questions can only be changed while the group is active.")
    }

    override fun ask(groupId: Long, santa: Username, text: String, atMillis: Long, maxQuestions: Int): AskedQuestion {
        requireActive(groupId)
        if (groups.receiverOf(groupId, santa) == null) throw DomainException.NotFound("Group not found.")
        val mine = rows.filter { it.groupId == groupId && it.santa == santa }
        if (mine.size >= maxQuestions) throw DomainException.InvalidState("The question limit has been reached.")
        val row = Row(groupId, santa, number = (mine.maxOfOrNull { it.number } ?: 0) + 1, text = text, askedAt = atMillis)
        rows += row
        return row.asked()
    }

    override fun answer(groupId: Long, receiver: Username, number: Int, text: String, atMillis: Long): ReceivedQuestion {
        requireActive(groupId)
        val santa = groups.santaOf(groupId, receiver) ?: throw DomainException.NotFound("Question not found.")
        val row = rows.firstOrNull { it.groupId == groupId && it.santa == santa && it.number == number }
            ?: throw DomainException.NotFound("Question not found.")
        if (row.answeredAt == null) row.answeredAt = atMillis
        row.answer = text
        row.answerUpdatedAt = atMillis
        return row.received()
    }

    override fun askedBy(groupId: Long, santa: Username) =
        rows.filter { it.groupId == groupId && it.santa == santa }.sortedBy { it.number }.map { it.asked() }

    override fun askedTo(groupId: Long, receiver: Username): List<ReceivedQuestion> {
        val santa = groups.santaOf(groupId, receiver) ?: return emptyList()
        return rows.filter { it.groupId == groupId && it.santa == santa }.sortedBy { it.number }.map { it.received() }
    }

    private fun Row.asked() = AskedQuestion(number, text, askedAt, answer, answeredAt, answerUpdatedAt)
    private fun Row.received() = ReceivedQuestion(number, text, askedAt, answer, answeredAt, answerUpdatedAt)
}
