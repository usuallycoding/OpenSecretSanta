package com.usuallycoding.opensecretsanta.data.api

import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto

/** Why a call to the server did not give the answer the screen asked for. */
sealed interface ApiError {
    /** HTTP 401: wrong credentials on sign-in, or a token the server no longer accepts. */
    data object Unauthorized : ApiError

    /** HTTP 403 with `password_change_required`: the password is temporary and must be replaced first. */
    data object PasswordChangeRequired : ApiError

    /** HTTP 400 naming the request field that is wrong (or null when the error is about the request as a whole). */
    data class Validation(val field: String?, val message: String) : ApiError

    /** HTTP 429: sign-in is paused; [retryAfterSeconds] comes from `Retry-After` when the server sent one. */
    data class RateLimited(val retryAfterSeconds: Int?) : ApiError

    /** HTTP 409 `confirmation_required`: deleting the group would also delete the data in [impact], so the owner must confirm. */
    data class ConfirmationRequired(val impact: DeletionImpactDto) : ApiError

    /** Any other error the server explained with an error body (404, 409, ...). */
    data class Rejected(val status: Int, val code: String, val message: String) : ApiError

    /** No response at all: the server could not be reached or the request timed out. */
    data object Network : ApiError

    /** A response the app cannot make sense of. */
    data object Unexpected : ApiError
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}
