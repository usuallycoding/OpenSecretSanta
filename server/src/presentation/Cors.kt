package com.usuallycoding.opensecretsanta.server.presentation

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.plugins.cors.CORSConfig

/**
 * Lets browser clients served from [origins] (`scheme://host[:port]`, already validated by the
 * configuration) call the API. Tokens travel in the `Authorization` header, so no cookies or
 * credentials are involved and nothing wider than the listed origins is ever allowed.
 */
fun CORSConfig.allowConfiguredOrigins(origins: List<String>) {
    for (origin in origins) {
        val scheme = origin.substringBefore("://")
        val host = origin.substringAfter("://")
        allowHost(host, schemes = listOf(scheme))
    }
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Options)
}
