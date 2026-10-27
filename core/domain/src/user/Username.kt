package com.usuallycoding.opensecretsanta.domain.user

import com.usuallycoding.opensecretsanta.domain.error.DomainException

/** A normalized (lowercase) username; two usernames that differ only in case are equal. */
class Username private constructor(val value: String) {
    override fun equals(other: Any?): Boolean = other is Username && other.value == value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 32
        private val ALLOWED = Regex("[a-z0-9._-]+")

        fun parse(raw: String, field: String = "username"): Username {
            val normalized = raw.lowercase()
            if (normalized.length !in MIN_LENGTH..MAX_LENGTH) {
                throw DomainException.ValidationFailed(field, "Username must be $MIN_LENGTH to $MAX_LENGTH characters long.")
            }
            if (!ALLOWED.matches(normalized)) {
                throw DomainException.ValidationFailed(field, "Username may only contain letters, digits, '.', '_' and '-'.")
            }
            return Username(normalized)
        }

        fun parseOrNull(raw: String): Username? =
            try {
                parse(raw)
            } catch (_: DomainException.ValidationFailed) {
                null
            }
    }
}
