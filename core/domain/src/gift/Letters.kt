package com.usuallycoding.opensecretsanta.domain.gift

import com.usuallycoding.opensecretsanta.domain.user.Username

object LetterLimits {
    const val MAX_TITLE_LENGTH = 100
    const val MAX_TEXT_LENGTH = 2000
    const val MAX_LETTERS_PER_RECEIVER = 30
}

/**
 * A letter from a receiver to their Santa. The same shape serves both sides and has no field that names anyone, so reading
 * letters can never reveal who the Santa is.
 */
data class Letter(
    val number: Int,
    val title: String,
    val text: String,
    val sentAtMillis: Long,
)

/** One receiver's letters to their Santa as the group owner sees them: both people are named. */
data class LetterBox(
    val santa: Username,
    val receiver: Username,
    val letters: List<Letter>,
)

/**
 * Storage for letters. A send names the state it expects (the group must be Active) and applies in one transaction, throwing
 * `InvalidState` when the group is not Active or the limit is reached, and `NotFound` when the group or the sender's assignment
 * does not exist.
 */
interface LetterRepository {
    /** Adds the next numbered letter to [receiver]'s letters; fails when [maxLetters] already exist. */
    fun send(groupId: Long, receiver: Username, title: String, text: String, atMillis: Long, maxLetters: Int): Letter

    /** The letters [receiver] has sent their Santa, in the order sent. */
    fun sentBy(groupId: Long, receiver: Username): List<Letter>

    /** The letters [santa]'s receiver has sent them, in the order sent. */
    fun sentToSanta(groupId: Long, santa: Username): List<Letter>
}
