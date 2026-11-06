package com.usuallycoding.opensecretsanta.ui.letters

import com.usuallycoding.opensecretsanta.contract.AdminLetterBoxDto
import com.usuallycoding.opensecretsanta.contract.LetterDto
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.formatInstant

/** One letter as the screens show it: nothing in it can name anyone. [sent] is the time it was sent, ready to read. */
data class LetterItem(val number: Int, val title: String, val text: String, val sent: String)

internal fun LetterDto.toItem() = LetterItem(number, title, text, formatInstant(sentAt))

/** The server lists letters in the order they were sent; the screens show the newest first. */
internal fun List<LetterDto>.newestFirst(): List<LetterItem> = map { it.toItem() }.sortedByDescending { it.number }

/** The letters the person sent their Santa, newest first, and the group's phase, read together. */
data class SentLettersData(val letters: List<LetterItem>, val phase: GroupPhase)

/** The letters the person's receiver sent them, newest first, with the receiver's username once it is known. */
data class InboxData(val receiver: String?, val letters: List<LetterItem>)

/** One pair's letters as the group's owner reads them: the receiver who wrote them and the Santa who gets them. */
data class AdminLetterBox(val santa: String, val receiver: String, val letters: List<LetterItem>)

internal fun AdminLetterBoxDto.toBox() = AdminLetterBox(santa, receiver, letters.newestFirst())

/** The letters of the pair whose receiver is [receiver]: empty when the pair has none or is not there. */
internal fun List<AdminLetterBox>.lettersOf(receiver: String): List<LetterItem> = firstOrNull { it.receiver == receiver }?.letters.orEmpty()

/** The sheet for a new letter: the title and the description, each with its own problem. */
data class LetterEditor(
    val title: String = "",
    val text: String = "",
    val titleError: String? = null,
    val textError: String? = null,
    val general: String? = null,
    val saving: Boolean = false,
)

/** Something that happened while the person was busy, and that the page says once. */
enum class LetterNotice { Locked, LimitReached, Unreachable }

data class LetterUiState(val editor: LetterEditor? = null, val notice: LetterNotice? = null)
