package com.usuallycoding.opensecretsanta.ui.groups

import com.usuallycoding.opensecretsanta.ui.Strings

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val DATE = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
private val TIME = Regex("(\\d{1,2}):(\\d{2})(?::\\d{2}(?:\\.\\d+)?)?")

/**
 * A group's date and time as the organiser entered them, for reading: `24 Dec 2026, 18:30`. They are
 * local values, so nothing is converted to another timezone. Either part may be missing, and a part
 * that cannot be read is shown as it came, never an error.
 */
fun formatWhen(date: String?, time: String?): String? {
    val readableDate = date?.takeIf { it.isNotBlank() }?.let { raw ->
        DATE.matchEntire(raw.trim())?.let { match ->
            val (year, month, day) = match.destructured
            val monthName = MONTHS.getOrNull(month.toInt() - 1) ?: return@let null
            "${day.toInt()} $monthName $year"
        } ?: raw
    }
    val readableTime = time?.takeIf { it.isNotBlank() }?.let { raw ->
        TIME.matchEntire(raw.trim())?.let { match ->
            val (hour, minute) = match.destructured
            "${hour.padStart(2, '0')}:$minute"
        } ?: raw
    }
    return listOfNotNull(readableDate, readableTime).takeIf { it.isNotEmpty() }?.joinToString(", ")
}

/** How a group's state is named, and what it means for the person; [note] says nothing about who gives to whom. */
data class GroupStateInfo(val label: String, val note: String?)

fun groupStateInfo(state: String): GroupStateInfo = when (state.lowercase()) {
    "pending" -> GroupStateInfo(Strings.STATE_PENDING, Strings.STATE_PENDING_NOTE)
    "active" -> GroupStateInfo(Strings.STATE_ACTIVE, Strings.STATE_ACTIVE_NOTE)
    "archived" -> GroupStateInfo(Strings.STATE_ARCHIVED, Strings.STATE_ARCHIVED_NOTE)
    else -> GroupStateInfo(state.replaceFirstChar { it.uppercase() }, note = null)
}

/** Usernames are case-insensitive, so "Alice" and "alice" are the same person. */
fun isMe(username: String, me: String): Boolean = username.equals(me, ignoreCase = true)
