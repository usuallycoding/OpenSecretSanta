package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

/** The person a participant gives a gift to: just what is needed to post it, as currently stored. */
@Serializable
data class ReceiverDto(
    val username: String,
    val fullName: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val deliveryInfo: String? = null,
)

/** Who gave a participant their gift, available only once the group is archived. */
@Serializable
data class SantaRevealDto(
    val username: String,
    val fullName: String? = null,
)

/**
 * One question and its answer. The same shape serves both sides of the thread and has no field
 * naming anyone, so the receiver's view cannot reveal the Santa. Times are ISO-8601 UTC instants.
 */
@Serializable
data class QuestionDto(
    val number: Int,
    val text: String,
    val askedAt: String,
    val answer: String? = null,
    val answeredAt: String? = null,
    val answerUpdatedAt: String? = null,
)

@Serializable
data class AskQuestionRequest(
    val text: String,
)

@Serializable
data class AnswerRequest(
    val text: String,
)
