package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.domain.auth.TokenService
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val ACCESS_AUTH = "access"

/** The caller, as loaded from the database for this very request. */
class UserPrincipal(val user: User)

fun ApplicationCall.currentUser(): User =
    principal<UserPrincipal>()?.user ?: throw DomainException.Unauthorized()

/** Runs blocking database work off the request threads. */
suspend fun <T> blocking(block: () -> T): T = withContext(Dispatchers.IO) { block() }

/**
 * Accepts a bearer token only if it is genuine and its token version still matches the stored user.
 * Admin status and the password-change requirement are read from the stored user, never the token.
 */
fun AuthenticationConfig.installAccessTokens(users: UserRepository, tokens: TokenService) {
    bearer(ACCESS_AUTH) {
        authenticate { credential ->
            blocking {
                val claims = tokens.verify(credential.token) ?: return@blocking null
                val user = users.find(claims.username) ?: return@blocking null
                if (user.tokenVersion == claims.tokenVersion) UserPrincipal(user) else null
            }
        }
    }
}

/** The only requests a caller with a temporary password may make: read their account and replace the password. */
private val OPEN_WHILE_PASSWORD_IS_TEMPORARY = setOf(
    HttpMethod.Get to ApiRoutes.ME,
    HttpMethod.Put to ApiRoutes.ME_PASSWORD,
)

/**
 * Install once on the authenticated routes. Everything is closed to a caller who still has a
 * temporary password except the allow-list above, so a new route cannot forget the rule.
 */
val PasswordChangeGate = createRouteScopedPlugin("PasswordChangeGate") {
    on(AuthenticationChecked) { call ->
        val mustChange = call.principal<UserPrincipal>()?.user?.mustChangePassword == true
        val request = call.request.httpMethod to call.request.path()
        if (mustChange && request !in OPEN_WHILE_PASSWORD_IS_TEMPORARY) {
            throw DomainException.PasswordChangeRequired()
        }
    }
}
