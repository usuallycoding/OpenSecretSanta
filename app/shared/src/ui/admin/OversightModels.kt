package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.DeliveryInfoDto
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.changedTime
import com.usuallycoding.opensecretsanta.ui.groups.formatInstant
import com.usuallycoding.opensecretsanta.ui.wishlist.linksOf

/** Who gives to whom, with which side is the administrator themselves. */
data class AdminPair(
    val santa: String,
    val receiver: String,
    val santaIsMe: Boolean,
    val receiverIsMe: Boolean,
    /** The person's delivery info for the group lacks a full name, an email, a phone or an address, so a gift cannot yet reach them. */
    val santaIncomplete: Boolean = false,
    val receiverIncomplete: Boolean = false,
    /** What the owner can search each side by besides the username. */
    val santaFullName: String? = null,
    val receiverFullName: String? = null,
    /** The person's account still has a temporary password. */
    val santaTemporary: Boolean = false,
    val receiverTemporary: Boolean = false,
)

/** The draw on a group's detail: absent while the group has no draw, otherwise the pairs or the reason they are missing. */
sealed interface DrawState {
    data object None : DrawState
    data class Pairs(val pairs: List<AdminPair>) : DrawState
    data object Failed : DrawState
}

internal fun AssignmentDto.toPair(me: String, participants: List<AdminParticipant>): AdminPair {
    val incomplete = participants.filter { it.hasAccount && !it.profileComplete }.map { it.username }.toSet()
    val temporary = participants.filter { it.hasAccount && it.mustChangePassword }.map { it.username }.toSet()
    val names = participants.associate { it.username to it.fullName }
    return AdminPair(
        santa = santa,
        receiver = receiver,
        santaIsMe = santa == me,
        receiverIsMe = receiver == me,
        santaIncomplete = santa in incomplete,
        receiverIncomplete = receiver in incomplete,
        santaFullName = names[santa],
        receiverFullName = names[receiver],
        santaTemporary = santa in temporary,
        receiverTemporary = receiver in temporary,
    )
}

/** One item of a participant's list as the owner reads it; [changed] is set only when it was edited after it was added. */
data class OversightItem(val title: String, val note: String?, val urls: List<String>, val added: String, val changed: String?)

internal fun SharedWishlistItemDto.toOversightItem() = OversightItem(
    title = title,
    note = note?.takeIf { it.isNotBlank() },
    urls = linksOf(urls, url),
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

/** What a participant told the group about where a gift can reach them, as the owner reads it: a field that is not set is null. */
data class OversightDeliveryInfo(
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val deliveryInfo: String?,
) {
    val isEmpty: Boolean get() = listOf(fullName, email, phone, address, deliveryInfo).all { it == null }

    /** The required fields that are not set, as words, in the order they are asked; the delivery information is optional. */
    val missing: List<String>
        get() = buildList {
            if (fullName == null) add("full name")
            if (email == null) add("email")
            if (phone == null) add("phone")
            if (address == null) add("address")
        }

    /** "Missing: email, phone" once something is set but not all four; nothing set is said by the sheet's own sentence instead. */
    val missingLine: String? get() = missing.takeIf { it.isNotEmpty() && !isEmpty }?.let(Strings::missingFields)
}

internal fun DeliveryInfoDto.toOversight() = OversightDeliveryInfo(
    fullName = fullName?.trim()?.ifEmpty { null },
    email = email?.trim()?.ifEmpty { null },
    phone = phone?.trim()?.ifEmpty { null },
    address = address?.trim()?.ifEmpty { null },
    deliveryInfo = deliveryInfo?.trim()?.ifEmpty { null },
)
