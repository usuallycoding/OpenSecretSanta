package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.header
import io.ktor.server.response.respond

fun DomainException.httpStatus(): HttpStatusCode = when (this) {
    is DomainException.InvalidCredentials, is DomainException.Unauthorized -> HttpStatusCode.Unauthorized
    is DomainException.PasswordChangeRequired, is DomainException.Forbidden -> HttpStatusCode.Forbidden
    is DomainException.NotFound -> HttpStatusCode.NotFound
    is DomainException.InvalidState, is DomainException.ConfirmationRequired -> HttpStatusCode.Conflict
    is DomainException.ValidationFailed -> HttpStatusCode.BadRequest
    is DomainException.TooManyAttempts -> HttpStatusCode.TooManyRequests
}

/** Maps every failure to the JSON [ErrorResponse] body; unexpected ones never leak details. */
fun StatusPagesConfig.installErrorHandling() {
    exception<DomainException> { call, cause ->
        if (cause is DomainException.TooManyAttempts) {
            call.response.header(HttpHeaders.RetryAfter, cause.retryAfterSeconds.toString())
        }
        val field = (cause as? DomainException.ValidationFailed)?.field
        val impact = (cause as? DomainException.ConfirmationRequired)?.impact?.let {
            DeletionImpactDto(it.wishlistItems, it.questions, it.answers, it.draw)
        }
        call.respond(cause.httpStatus(), ErrorResponse(cause.code, cause.message.orEmpty(), field, impact))
    }
    exception<BadRequestException> { call, _ ->
        call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_failed", "The request could not be understood."))
    }
    exception<Throwable> { call, cause ->
        call.application.log.error("Unhandled exception", cause)
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "An unexpected error occurred."))
    }
    status(HttpStatusCode.Unauthorized) { call, _ ->
        respondUnauthorized(call)
    }
    // Rejections by the rate limiter arrive without a body; Retry-After is already set by then.
    status(HttpStatusCode.TooManyRequests) { call, _ ->
        val error = DomainException.TooManyAttempts(retryAfterSeconds = 0)
        call.respond(HttpStatusCode.TooManyRequests, ErrorResponse(error.code, error.message.orEmpty()))
    }
}

private suspend fun respondUnauthorized(call: ApplicationCall) {
    val error = DomainException.Unauthorized()
    call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer")
    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error.code, error.message.orEmpty()))
}
