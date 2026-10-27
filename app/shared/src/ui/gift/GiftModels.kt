package com.usuallycoding.opensecretsanta.ui.gift

import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.ReceiverDto
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase

/** One question of a thread with the answer the receiver gave, if any. Nothing in it can name anyone. */
data class Question(val number: Int, val text: String, val answer: String?)

/** Where the person posts their gift: whatever the receiver has set, blank values left out. */
data class ReceiverInfo(
    val username: String,
    val fullName: String?,
    val address: String?,
    val phone: String?,
    val deliveryInfo: String?,
) {
    val hasPostalDetails: Boolean get() = listOf(fullName, address, phone, deliveryInfo).any { it != null }
}

/** The receiver, the questions the person asked and the group's phase, read together. */
data class ReceiverData(val receiver: ReceiverInfo, val questions: List<Question>, val phase: GroupPhase)

/** The questions the person's Santa asked and the group's phase, read together. */
data class SantaQuestionsData(val questions: List<Question>, val phase: GroupPhase) {
    val canAnswer: Boolean get() = phase == GroupPhase.Active
}

/** The bottom sheet: writing a question, or the answer to question [number]. */
data class TextEditor(
    val number: Int? = null,
    val text: String = "",
    val error: String? = null,
    val general: String? = null,
    val saving: Boolean = false,
)

/** Something that happened while the person was busy, and that the screen says once. */
enum class GiftNotice { Locked, LimitReached, QuestionGone, Unreachable }

data class ReceiverUiState(val editor: TextEditor? = null, val notice: GiftNotice? = null)

internal fun QuestionDto.toQuestion() = Question(number, text, answer)

internal fun ReceiverDto.toInfo() = ReceiverInfo(
    username,
    fullName.clean(), address.clean(), phone.clean(), deliveryInfo.clean(),
)

private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }

/** The text to send, or null when it is blank or longer than [max] once trimmed: what the server refuses. */
internal fun checkedText(text: String, max: Int): String? = text.trim().takeIf { it.isNotEmpty() && it.length <= max }
