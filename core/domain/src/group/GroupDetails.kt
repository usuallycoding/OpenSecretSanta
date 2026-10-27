package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** What the owner can edit about a group, apart from its participants. */
data class GroupDetails(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
) {
    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 2000
        const val MAX_LOCATION_LENGTH = 200

        /** Builds details from raw input: trims text, turns blanks into null and validates every field. */
        fun of(
            name: String?,
            description: String?,
            location: String?,
            date: String?,
            time: String?,
        ): GroupDetails {
            val cleanName = name?.trim().orEmpty()
            if (cleanName.isEmpty() || cleanName.length > MAX_NAME_LENGTH) {
                throw DomainException.ValidationFailed("name", "Name must be 1 to $MAX_NAME_LENGTH characters long.")
            }
            return GroupDetails(
                name = cleanName,
                description = optionalText("description", description, MAX_DESCRIPTION_LENGTH),
                location = optionalText("location", location, MAX_LOCATION_LENGTH),
                date = optional("date", date, "an ISO date such as 2026-12-24") { LocalDate.parse(it) },
                time = optional("time", time, "a time of day such as 18:30") { LocalTime.parse(it) },
            )
        }

        private fun optionalText(field: String, value: String?, maxLength: Int): String? {
            val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (trimmed.length > maxLength) {
                throw DomainException.ValidationFailed(field, "$field must be at most $maxLength characters long.")
            }
            return trimmed
        }

        private fun <T> optional(field: String, value: String?, expected: String, parse: (String) -> T): T? {
            val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return try {
                parse(trimmed)
            } catch (_: IllegalArgumentException) {
                throw DomainException.ValidationFailed(field, "$field must be $expected.")
            }
        }
    }
}
