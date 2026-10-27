package com.usuallycoding.opensecretsanta.domain.gift

import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * A question as its Santa sees it. The receiver is implied (it is whoever the Santa gives to), so
 * no username is carried here.
 */
data class AskedQuestion(
    val number: Int,
    val text: String,
    val askedAtMillis: Long,
    val answer: String?,
    val answeredAtMillis: Long?,
    val answerUpdatedAtMillis: Long?,
)

/**
 * A question as its receiver sees it. This type deliberately has no field that could hold the
 * asker: the receiver must not learn who their Santa is before the group is archived.
 */
data class ReceivedQuestion(
    val number: Int,
    val text: String,
    val askedAtMillis: Long,
    val answer: String?,
    val answeredAtMillis: Long?,
    val answerUpdatedAtMillis: Long?,
)

/** What a Santa may know about the person they give a gift to. */
data class ReceiverDetails(
    val username: Username,
    val fullName: String?,
    val address: String?,
    val phone: String?,
    val deliveryInfo: String?,
)

/** What a receiver may learn about their Santa once the group is archived, and nothing more. */
data class SantaReveal(
    val username: Username,
    val fullName: String?,
)

/**
 * Storage for the questions between Santas and receivers. Writes name the state they expect (the
 * group must be Active) and apply in one transaction, throwing `InvalidState` when the group is not
 * Active or a limit is reached, and `NotFound` when the group, the assignment or the question does
 * not exist.
 */
interface QuestionRepository {
    /** Adds the next numbered question to [santa]'s thread; fails when [maxQuestions] already exist. */
    fun ask(groupId: Long, santa: Username, text: String, atMillis: Long, maxQuestions: Int): AskedQuestion

    /** Sets or replaces the answer to question [number] among those addressed to [receiver]. */
    fun answer(groupId: Long, receiver: Username, number: Int, text: String, atMillis: Long): ReceivedQuestion

    fun askedBy(groupId: Long, santa: Username): List<AskedQuestion>

    fun askedTo(groupId: Long, receiver: Username): List<ReceivedQuestion>
}
