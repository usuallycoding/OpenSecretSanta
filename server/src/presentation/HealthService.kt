package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiVersion
import com.usuallycoding.opensecretsanta.contract.HealthResponse
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.download.ApkDownloads
import org.slf4j.LoggerFactory

class HealthService(private val database: Database, private val downloads: ApkDownloads) {
    private val log = LoggerFactory.getLogger(HealthService::class.java)

    fun check(): HealthResponse = try {
        HealthResponse(HealthResponse.STATUS_OK, database.schemaVersion(), ApiVersion.CURRENT, androidDownload())
    } catch (e: Exception) {
        log.error("Health check failed", e)
        HealthResponse(HealthResponse.STATUS_ERROR)
    }

    /** Whether an app can be downloaded; a folder that cannot be read only means no, and never makes the server unhealthy. */
    private fun androidDownload(): Boolean = runCatching { downloads.latest() != null }.getOrDefault(false)
}
