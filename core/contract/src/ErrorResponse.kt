package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    /** The request field a validation error is about, when there is one. */
    val field: String? = null,
)
