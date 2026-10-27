package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val schemaVersion: Int? = null,
) {
    companion object {
        const val STATUS_OK = "ok"
        const val STATUS_ERROR = "error"
    }
}
