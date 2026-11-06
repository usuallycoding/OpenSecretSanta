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
    val email: String? = null,
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

/**
 * One Santa's thread as the group owner sees it. Unlike the participant views, both people are named;
 * this type is only ever returned by the owner's admin endpoint.
 */
@Serializable
data class AdminQuestionThreadDto(
    val santa: String,
    val receiver: String,
    val questions: List<QuestionDto>,
)

@Serializable
data class AskQuestionRequest(
    val text: String,
)

@Serializable
data class AnswerRequest(
    val text: String,
)

/**
 * One letter from a receiver to their Santa. The same shape serves both sides and has no field naming anyone, so reading letters
 * cannot reveal the Santa. [sentAt] is an ISO-8601 UTC instant.
 */
@Serializable
data class LetterDto(
    val number: Int,
    val title: String,
    val text: String,
    val sentAt: String,
)

/** A letter to send: both the title (1 to 100 characters) and the text (1 to 2000) are required. */
@Serializable
data class SendLetterRequest(
    val title: String,
    val text: String,
)

/** One receiver's letters to their Santa as the group owner sees them: both people are named, only in the owner's endpoint. */
@Serializable
data class AdminLetterBoxDto(
    val santa: String,
    val receiver: String,
    val letters: List<LetterDto>,
)

/** How much of one kind a Santa has not seen yet, and the newest time they have seen of it (an ISO-8601 UTC instant; null when never marked). */
@Serializable
data class ReadMarkDto(
    val unread: Int,
    val seenUpTo: String? = null,
)

/** A Santa's own marks for what their receiver keeps and sends. Only ever returned to that Santa. */
@Serializable
data class ReadMarksDto(
    val wishlist: ReadMarkDto,
    val answers: ReadMarkDto,
    val letters: ReadMarkDto,
)

/** The newest time the Santa has seen of one kind: an ISO-8601 UTC instant, taken from what the app showed. */
@Serializable
data class MarkReadRequest(
    val seenUpTo: String? = null,
)
