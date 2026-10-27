package com.usuallycoding.opensecretsanta.domain.gift

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User

object QuestionLimits {
    const val MAX_QUESTION_LENGTH = 1000
    const val MAX_ANSWER_LENGTH = 2000
    const val MAX_QUESTIONS_PER_SANTA = 30
}

private fun cleanText(value: String, maxLength: Int): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > maxLength) {
        throw DomainException.ValidationFailed("text", "Text must be 1 to $maxLength characters long.")
    }
    return trimmed
}

private fun Group.requireActive() {
    if (state != GroupState.ACTIVE) {
        throw DomainException.InvalidState("Questions and answers can only be written while the group is active.")
    }
}

private fun Group.requireThread() {
    if (state != GroupState.ACTIVE && state != GroupState.ARCHIVED) {
        throw DomainException.InvalidState("Questions start once the group is active.")
    }
}

/** A Santa asks their receiver a question; each Santa has a thread of up to 30, numbered from 1. */
class AskQuestion(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val questions: QuestionRepository,
    private val clock: Clock,
) {
    operator fun invoke(user: User, groupId: Long, text: String): AskedQuestion {
        val group = loadParticipantGroup(user, groupId)
        group.requireActive()
        val clean = cleanText(text, QuestionLimits.MAX_QUESTION_LENGTH)
        return questions.ask(group.id, user.username, clean, clock.nowMillis(), QuestionLimits.MAX_QUESTIONS_PER_SANTA)
    }
}

/** The questions the participant asked their receiver, with whatever has been answered. */
class ListMyQuestions(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val questions: QuestionRepository,
) {
    operator fun invoke(user: User, groupId: Long): List<AskedQuestion> {
        val group = loadParticipantGroup(user, groupId)
        group.requireThread()
        return questions.askedBy(group.id, user.username)
    }
}

/** The questions the participant's Santa asked them. The result type has no field that names the Santa. */
class ListQuestionsFromMySanta(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val questions: QuestionRepository,
) {
    operator fun invoke(user: User, groupId: Long): List<ReceivedQuestion> {
        val group = loadParticipantGroup(user, groupId)
        group.requireThread()
        return questions.askedTo(group.id, user.username)
    }
}

/** The receiver sets or changes their answer to one of their Santa's questions. */
class AnswerQuestion(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val questions: QuestionRepository,
    private val clock: Clock,
) {
    operator fun invoke(user: User, groupId: Long, number: Int, text: String): ReceivedQuestion {
        val group = loadParticipantGroup(user, groupId)
        group.requireActive()
        val clean = cleanText(text, QuestionLimits.MAX_ANSWER_LENGTH)
        return questions.answer(group.id, user.username, number, clean, clock.nowMillis())
    }
}
