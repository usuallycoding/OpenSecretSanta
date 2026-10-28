package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.ui.groups.changedTime
import com.usuallycoding.opensecretsanta.ui.groups.formatInstant

/** Who gives to whom, with which side is the administrator themselves. */
data class AdminPair(
    val santa: String,
    val receiver: String,
    val santaIsMe: Boolean,
    val receiverIsMe: Boolean,
    /** The person's profile lacks a name or an address, so a gift cannot yet be posted to them. */
    val santaIncomplete: Boolean = false,
    val receiverIncomplete: Boolean = false,
)

/** The draw on a group's detail: absent while the group has no draw, otherwise the pairs or the reason they are missing. */
sealed interface DrawState {
    data object None : DrawState
    data class Pairs(val pairs: List<AdminPair>) : DrawState
    data object Failed : DrawState
}

internal fun AssignmentDto.toPair(me: String, participants: List<AdminParticipant>): AdminPair {
    val incomplete = participants.filter { !it.profileComplete }.map { it.username }.toSet()
    return AdminPair(santa, receiver, santa == me, receiver == me, santa in incomplete, receiver in incomplete)
}

/** One item of a participant's list as the owner reads it; [changed] is set only when it was edited after it was added. */
data class OversightItem(val title: String, val note: String?, val url: String?, val added: String, val changed: String?)

internal fun SharedWishlistItemDto.toOversightItem() = OversightItem(
    title = title,
    note = note?.takeIf { it.isNotBlank() },
    url = url?.takeIf { it.isNotBlank() },
    added = formatInstant(createdAt),
    changed = changedTime(createdAt, updatedAt),
)

/** A question with its answer; [answerChanged] is set only when the answer was changed after it was first given. */
data class OversightQuestion(
    val number: Int,
    val text: String,
    val asked: String,
    val answer: String?,
    val answered: String?,
    val answerChanged: String?,
)

data class OversightThread(val santa: String, val receiver: String, val santaIsMe: Boolean, val receiverIsMe: Boolean, val questions: List<OversightQuestion>)

internal fun QuestionDto.toOversightQuestion() = OversightQuestion(
    number = number,
    text = text,
    asked = formatInstant(askedAt),
    answer = answer,
    answered = answeredAt?.let(::formatInstant),
    answerChanged = answerUpdatedAt?.let { changedTime(answeredAt, it) },
)

internal fun AdminQuestionThreadDto.toThread(me: String) = OversightThread(
    santa, receiver, santa == me, receiver == me, questions.map { it.toOversightQuestion() },
)
