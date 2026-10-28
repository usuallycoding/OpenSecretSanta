package com.usuallycoding.opensecretsanta.domain.error

import com.usuallycoding.opensecretsanta.domain.group.DeletionImpact

/** Failures the domain reports to its callers; each carries the stable code the API exposes. */
sealed class DomainException(val code: String, message: String) : RuntimeException(message) {
    class InvalidCredentials : DomainException("invalid_credentials", "Invalid username or password.")
    class Unauthorized(message: String = "Authentication required.") : DomainException("unauthorized", message)
    class PasswordChangeRequired : DomainException("password_change_required", "The password must be changed before continuing.")
    class Forbidden(message: String = "You are not allowed to do that.") : DomainException("forbidden", message)
    class NotFound(message: String = "Not found.") : DomainException("not_found", message)
    class InvalidState(message: String) : DomainException("invalid_state", message)
    /** The group has data that deleting it would destroy, and the caller has not said that is fine. */
    class ConfirmationRequired(val impact: DeletionImpact) :
        DomainException("confirmation_required", "Deleting this group also deletes its draw, wishlist items, questions and answers. Confirm to go on.")
    class ValidationFailed(val field: String, message: String) : DomainException("validation_failed", message)
    class TooManyAttempts(val retryAfterSeconds: Long) :
        DomainException("too_many_attempts", "Too many attempts. Try again later.")
}
