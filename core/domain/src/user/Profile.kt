package com.usuallycoding.opensecretsanta.domain.user

import com.usuallycoding.opensecretsanta.domain.error.DomainException

/** The user-editable part of an account; every field is optional. */
data class Profile(
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val deliveryInfo: String? = null,
) {
    /** Enough for a Santa to post a gift: a name and an address are both present. */
    val isComplete: Boolean get() = fullName != null && address != null

    companion object {
        const val MAX_LENGTH = 500
        private val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

        val EMPTY = Profile()

        /** Builds a profile from raw input: trims text, turns blanks into null and validates. */
        fun of(
            fullName: String?,
            email: String?,
            phone: String?,
            address: String?,
            deliveryInfo: String?,
        ): Profile {
            val cleanEmail = clean("email", email)
            if (cleanEmail != null && !EMAIL.matches(cleanEmail)) {
                throw DomainException.ValidationFailed("email", "Email is not a valid address.")
            }
            return Profile(
                fullName = clean("fullName", fullName),
                email = cleanEmail,
                phone = clean("phone", phone),
                address = clean("address", address),
                deliveryInfo = clean("deliveryInfo", deliveryInfo),
            )
        }

        private fun clean(field: String, value: String?): String? {
            val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (trimmed.length > MAX_LENGTH) {
                throw DomainException.ValidationFailed(field, "$field must be at most $MAX_LENGTH characters long.")
            }
            return trimmed
        }
    }
}
