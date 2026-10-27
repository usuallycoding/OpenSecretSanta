package com.usuallycoding.opensecretsanta.domain.error

/** Failures the domain reports to its callers; each carries the stable code the API exposes. */
sealed class DomainException(val code: String, message: String) : RuntimeException(message) {
    class InvalidCredentials : DomainException("invalid_credentials", "Invalid username or password.")
    class Unauthorized(message: String = "Authentication required.") : DomainException("unauthorized", message)
    class PasswordChangeRequired : DomainException("password_change_required", "The password must be changed before continuing.")
    class Forbidden(message: String = "You are not allowed to do that.") : DomainException("forbidden", message)
    class NotFound(message: String = "Not found.") : DomainException("not_found", message)
    class InvalidState(message: String) : DomainException("invalid_state", message)
    class ValidationFailed(val field: String, message: String) : DomainException("validation_failed", message)
    class TooManyAttempts(val retryAfterSeconds: Long) :
        DomainException("too_many_attempts", "Too many attempts. Try again later.")
}
