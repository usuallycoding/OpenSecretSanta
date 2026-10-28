package com.usuallycoding.opensecretsanta.ui.groups

import com.usuallycoding.opensecretsanta.ui.Strings
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

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

/** Where a group is in its life, for deciding what the person may do with it; [Other] is a state this app does not know. */
enum class GroupPhase { Pending, Active, Archived, Other }

fun groupPhase(state: String): GroupPhase = when (state.lowercase()) {
    "pending" -> GroupPhase.Pending
    "active" -> GroupPhase.Active
    "archived" -> GroupPhase.Archived
    else -> GroupPhase.Other
}

/** Usernames are case-insensitive, so "Alice" and "alice" are the same person. */
fun isMe(username: String, me: String): Boolean = username.equals(me, ignoreCase = true)

/**
 * An instant from the server (ISO-8601, UTC) for reading in Bucharest time, summer time included, with no zone suffix:
 * `1 Dec 2026, 12:00`. One that cannot be read is shown as it came.
 *
 * Bucharest is UTC+2, and UTC+3 from the last Sunday of March at 01:00 UTC until the last Sunday of October at
 * 01:00 UTC (the EU rule). It is applied here instead of asking the platform for the `Europe/Bucharest` zone, because the
 * web cannot always provide the time-zone database, and every platform must show the same time.
 */
fun formatInstant(raw: String): String {
    val instant = try {
        Instant.parse(raw.trim())
    } catch (_: Exception) {
        return raw
    }
    val offset = if (inEuSummerTime(instant)) 3.hours else 2.hours
    val local = (instant + offset).toLocalDateTime(TimeZone.UTC)
    val monthName = MONTHS[local.month.ordinal]
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.day} $monthName ${local.year}, $hour:$minute"
}

/** When something was changed, for reading, or null when it never was: the change is the same instant as the creation. */
fun changedTime(created: String?, updated: String): String? = updated.takeIf { it != created }?.let(::formatInstant)

/** Whether the EU's summer time is in force at [instant]. */
private fun inEuSummerTime(instant: Instant): Boolean {
    val year = instant.toLocalDateTime(TimeZone.UTC).year
    return instant >= lastSundayAtOneUtc(year, 3) && instant < lastSundayAtOneUtc(year, 10)
}

/** 01:00 UTC on the last Sunday of [month], a month of 31 days (March and October are). */
private fun lastSundayAtOneUtc(year: Int, month: Int): Instant {
    val lastDay = LocalDate(year, month, 31)
    val sunday = lastDay.minus(DatePeriod(days = lastDay.dayOfWeek.isoDayNumber % 7))
    return LocalDateTime(sunday, LocalTime(1, 0)).toInstant(TimeZone.UTC)
}
