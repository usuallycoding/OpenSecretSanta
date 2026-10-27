package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.HealthResponse
import com.usuallycoding.opensecretsanta.server.data.db.Database
import org.slf4j.LoggerFactory

class HealthService(private val database: Database) {
    private val log = LoggerFactory.getLogger(HealthService::class.java)

    fun check(): HealthResponse = try {
        HealthResponse(HealthResponse.STATUS_OK, database.schemaVersion())
    } catch (e: Exception) {
        log.error("Health check failed", e)
        HealthResponse(HealthResponse.STATUS_ERROR)
    }
}
