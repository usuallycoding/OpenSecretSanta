package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.LoginResponse
import com.usuallycoding.opensecretsanta.domain.auth.Login
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimitConfig
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlin.time.Duration.Companion.minutes

val LOGIN_RATE_LIMIT = RateLimitName("login")

const val LOGIN_REQUESTS_PER_MINUTE = 20

/**
 * At most [LOGIN_REQUESTS_PER_MINUTE] login requests per minute per client address. Behind a
 * reverse proxy the address must be forwarded (X-Forwarded-For), otherwise all clients share one.
 */
fun RateLimitConfig.registerLoginLimit() {
    register(LOGIN_RATE_LIMIT) {
        rateLimiter(limit = LOGIN_REQUESTS_PER_MINUTE, refillPeriod = 1.minutes)
        requestKey { call -> call.request.origin.remoteHost }
    }
}

fun Route.authRoutes(login: Login) {
    rateLimit(LOGIN_RATE_LIMIT) {
        post(ApiRoutes.LOGIN) {
            val request = call.receive<LoginRequest>()
            val result = blocking { login(request.username, request.password) }
            call.respond(LoginResponse(result.token, result.user.mustChangePassword))
        }
    }
}
