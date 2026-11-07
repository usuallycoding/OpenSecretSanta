package com.usuallycoding.opensecretsanta.domain.auth

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

class LoginResult(val user: User, val token: String)

/**
 * Checks a username and password. After [AttemptLimiter.MAX_FAILURES] consecutive failures the username is locked
 * for [AttemptLimiter.LOCK_MILLIS]; unknown usernames are tracked and answered identically so that neither
 * the response nor its timing reveals whether an account exists.
 */
class Login(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val tokens: TokenService,
    attempts: LoginAttemptTracker,
    clock: Clock,
) {
    private val limiter = AttemptLimiter(attempts, clock)
    private val timingDummyHash by lazy { hasher.hash("timing-equalizer") }

    operator fun invoke(rawUsername: String, password: String): LoginResult {
        val username = Username.parseOrNull(rawUsername)
        if (username == null) {
            hasher.verify(password, timingDummyHash)
            throw DomainException.InvalidCredentials()
        }

        val previous = limiter.check(username)

        val credentials = users.credentials(username)
        val matches = if (credentials != null) {
            hasher.verify(password, credentials.passwordHash)
        } else {
            hasher.verify(password, timingDummyHash)
            false
        }
        val user = if (matches) users.find(username) else null
        if (user == null) {
            limiter.recordFailure(username, previous)
            throw DomainException.InvalidCredentials()
        }

        limiter.clear(username)
        return LoginResult(user, tokens.issue(user.username, user.tokenVersion))
    }
}
