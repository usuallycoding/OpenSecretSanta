package com.usuallycoding.opensecretsanta.ui.admin

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Where an empty date picker starts: 25 December of this year, or of the next one once that day has passed. */
internal fun defaultGroupDate(today: LocalDate): LocalDate {
    val christmas = LocalDate(today.year, 12, 25)
    return if (today <= christmas) christmas else LocalDate(today.year + 1, 12, 25)
}

/** Where an empty time picker starts. */
internal val DEFAULT_GROUP_TIME = LocalTime(0, 0)

/**
 * The date picker counts in UTC milliseconds at midnight. The date a group has is a local value with no timezone, so it is
 * converted with UTC both ways: the date the person picks is the date that is saved, wherever they are.
 */
internal fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun dateFromPickerMillis(millis: Long): LocalDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

/** The date a form field holds, or null when it is empty or not a date. */
internal fun parseGroupDate(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim()) }.getOrNull()

/** The time a form field holds, or null when it is empty or not a time. */
internal fun parseGroupTime(text: String): LocalTime? = runCatching { LocalTime.parse(text.trim()) }.getOrNull()
