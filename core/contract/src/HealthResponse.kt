package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val schemaVersion: Int? = null,
    /** The version of the API, [ApiVersion.CURRENT] of the server's build; absent when the server is not healthy, and from older servers. */
    val apiVersion: Int? = null,
    /** Whether an Android app can be downloaded at [ApiRoutes.DOWNLOAD_LATEST]; absent when the server is not healthy. */
    val androidDownload: Boolean? = null,
) {
    companion object {
        const val STATUS_OK = "ok"
        const val STATUS_ERROR = "error"
    }
}
