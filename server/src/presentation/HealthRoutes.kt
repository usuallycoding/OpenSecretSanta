package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.HealthResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes(health: HealthService) {
    get(ApiRoutes.HEALTH) {
        val result = health.check()
        val status = if (result.status == HealthResponse.STATUS_OK) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(status, result)
    }
}
