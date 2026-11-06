package com.usuallycoding.opensecretsanta.domain.delivery

import com.usuallycoding.opensecretsanta.domain.error.DomainException

/** What a participant tells one group about where and how a gift can reach them; every field is optional. */
data class DeliveryInfo(
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val deliveryInfo: String? = null,
) {
    /** The required fields that are not set, in the order they are asked: the delivery information is optional. */
    val missingFields: List<String> get() = buildList {
        if (fullName == null) add("fullName")
        if (email == null) add("email")
        if (phone == null) add("phone")
        if (address == null) add("address")
    }

    /** Enough for a gift to reach the person: the full name, email, phone and address are all present. */
    val isComplete: Boolean get() = missingFields.isEmpty()

    /** Refuses details that lack a required field, naming the first one missing. */
    fun requireComplete() {
        val field = missingFields.firstOrNull() ?: return
        throw DomainException.ValidationFailed(field, "${REQUIRED_LABELS.getValue(field)} is required.")
    }

    companion object {
        const val MAX_LENGTH = 500
        private val REQUIRED_LABELS = mapOf("fullName" to "Full name", "email" to "Email", "phone" to "Phone", "address" to "Address")
        private val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

        val EMPTY = DeliveryInfo()

        /** Builds delivery info from raw input: trims text, turns blanks into null and validates. */
        fun of(
            fullName: String?,
            email: String?,
            phone: String?,
            address: String?,
            deliveryInfo: String?,
        ): DeliveryInfo {
            val cleanEmail = clean("email", email)
            if (cleanEmail != null && !EMAIL.matches(cleanEmail)) {
                throw DomainException.ValidationFailed("email", "Email is not a valid address.")
            }
            return DeliveryInfo(
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
