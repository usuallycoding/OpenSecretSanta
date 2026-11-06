package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.ui.Strings

/**
 * What the owner is told about one participant next to their username. A person with no account has only [noAccount]: there is
 * nothing else to say about an account that does not exist. In a Draft nobody has an account for the group yet and nobody can
 * fill delivery info in, so only a missing account is marked there.
 */
data class ParticipantMarks(
    val noAccount: Boolean = false,
    /** The account exists and its password is still the temporary one. */
    val temporaryPassword: Boolean = false,
    /** The account exists and the delivery info lacks a full name, an email, a phone or an address. */
    val incompleteInfo: Boolean = false,
) {
    val any: Boolean get() = noAccount || temporaryPassword || incompleteInfo
}

/** The marks the legend explains, in the order it lists them. */
enum class Mark { NoAccount, TemporaryPassword, IncompleteInfo }

internal fun AdminParticipant.marks(draft: Boolean) = ParticipantMarks(
    noAccount = !hasAccount,
    temporaryPassword = hasAccount && !draft && mustChangePassword,
    incompleteInfo = hasAccount && !draft && !profileComplete,
)

/** The marks of the two sides of a pair; both people of a drawn group have an account. */
internal fun AdminPair.santaMarks() = ParticipantMarks(temporaryPassword = santaTemporary, incompleteInfo = santaIncomplete)

internal fun AdminPair.receiverMarks() = ParticipantMarks(temporaryPassword = receiverTemporary, incompleteInfo = receiverIncomplete)

/** The marks present in a list, in the legend's order: only these are explained. */
internal fun legendMarks(marks: Collection<ParticipantMarks>): List<Mark> = buildList {
    if (marks.any { it.noAccount }) add(Mark.NoAccount)
    if (marks.any { it.temporaryPassword }) add(Mark.TemporaryPassword)
    if (marks.any { it.incompleteInfo }) add(Mark.IncompleteInfo)
}

/** How many participants are on a temporary password and how many have incomplete delivery info. */
data class MarkCounts(val temporaryPassword: Int, val incompleteInfo: Int)

internal fun markCounts(participants: List<AdminParticipant>, draft: Boolean): MarkCounts {
    val marks = participants.map { it.marks(draft) }
    return MarkCounts(marks.count { it.temporaryPassword }, marks.count { it.incompleteInfo })
}

/** "2 on a temporary password, 3 with delivery info incomplete": a count of 0 is left out, and with both 0 there is no line. */
internal fun markCountLine(counts: MarkCounts): String? =
    listOfNotNull(
        counts.temporaryPassword.takeIf { it > 0 }?.let(Strings::onTemporaryPassword),
        counts.incompleteInfo.takeIf { it > 0 }?.let(Strings::withIncompleteInfo),
    ).takeIf { it.isNotEmpty() }?.joinToString(", ")
