package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    /** The request field a validation error is about, when there is one. */
    val field: String? = null,
    /** What deleting a group would also delete, with the `confirmation_required` code. */
    val impact: DeletionImpactDto? = null,
)

/** The data a group's deletion takes with it: the counts the owner is asked to confirm. */
@Serializable
data class DeletionImpactDto(
    val wishlistItems: Int,
    val questions: Int,
    val answers: Int,
    val draw: Boolean,
)
