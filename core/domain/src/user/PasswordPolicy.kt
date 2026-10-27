package com.usuallycoding.opensecretsanta.domain.user

import com.usuallycoding.opensecretsanta.domain.error.DomainException

object PasswordPolicy {
    const val MIN_LENGTH = 8

    /** Checks a password a user wants to set; [current] is the password being replaced, if any. */
    fun validateNew(newPassword: String, current: String? = null, field: String = "newPassword") {
        if (newPassword.length < MIN_LENGTH) {
            throw DomainException.ValidationFailed(field, "Password must be at least $MIN_LENGTH characters long.")
        }
        if (current != null && newPassword == current) {
            throw DomainException.ValidationFailed(field, "The new password must differ from the current one.")
        }
    }
}
