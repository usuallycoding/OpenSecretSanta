package com.usuallycoding.opensecretsanta.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.auth.TokenClaims
import com.usuallycoding.opensecretsanta.domain.auth.TokenService
import com.usuallycoding.opensecretsanta.domain.user.Username
import java.util.Date

/**
 * HS256 access tokens carrying the username (`sub`) and the user's token version (`tv`).
 * Expiry is stamped from [clock] at issue time and enforced by the JWT library on verification.
 */
class JwtTokenService(
    secret: String,
    private val clock: Clock,
    private val lifetimeMillis: Long = DEFAULT_LIFETIME_MILLIS,
) : TokenService {
    private val algorithm = Algorithm.HMAC256(secret)
    private val verifier = JWT.require(algorithm).withClaimPresence("sub").withClaimPresence(VERSION_CLAIM).build()

    override fun issue(username: Username, tokenVersion: Int): String {
        val now = clock.nowMillis()
        return JWT.create()
            .withSubject(username.value)
            .withClaim(VERSION_CLAIM, tokenVersion)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + lifetimeMillis))
            .sign(algorithm)
    }

    override fun verify(token: String): TokenClaims? = try {
        val decoded = verifier.verify(token)
        val username = Username.parseOrNull(decoded.subject)
        val version = decoded.getClaim(VERSION_CLAIM).asInt()
        if (username == null || version == null) null else TokenClaims(username, version)
    } catch (_: JWTVerificationException) {
        null
    }

    companion object {
        const val VERSION_CLAIM = "tv"
        const val DEFAULT_LIFETIME_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
