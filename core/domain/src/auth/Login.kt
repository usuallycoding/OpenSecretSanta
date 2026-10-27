package com.usuallycoding.opensecretsanta.domain.auth

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

class LoginResult(val user: User, val token: String)

/**
 * Checks a username and password. After [MAX_FAILURES] consecutive failures the username is locked
 * for [LOCK_MILLIS]; unknown usernames are tracked and answered identically so that neither
 * the response nor its timing reveals whether an account exists.
 */
class Login(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val tokens: TokenService,
    private val attempts: LoginAttemptTracker,
    private val clock: Clock,
) {
    private val timingDummyHash by lazy { hasher.hash("timing-equalizer") }

    operator fun invoke(rawUsername: String, password: String): LoginResult {
        val username = Username.parseOrNull(rawUsername)
        if (username == null) {
            hasher.verify(password, timingDummyHash)
            throw DomainException.InvalidCredentials()
        }

        val now = clock.nowMillis()
        val previous = attempts.get(username)
        val lockedUntil = previous?.lockedUntilMillis
        if (lockedUntil != null && lockedUntil > now) {
            throw DomainException.TooManyAttempts(retryAfterSeconds = (lockedUntil - now + 999) / 1000)
        }

        val credentials = users.credentials(username)
        val matches = if (credentials != null) {
            hasher.verify(password, credentials.passwordHash)
        } else {
            hasher.verify(password, timingDummyHash)
            false
        }
        val user = if (matches) users.find(username) else null
        if (user == null) {
            recordFailure(username, previous, now)
            throw DomainException.InvalidCredentials()
        }

        attempts.clear(username)
        return LoginResult(user, tokens.issue(user.username, user.tokenVersion))
    }

    private fun recordFailure(username: Username, previous: LoginAttempts?, now: Long) {
        val lockExpired = previous?.lockedUntilMillis?.let { it <= now } ?: false
        val failures = (if (previous == null || lockExpired) 0 else previous.failures) + 1
        val lockedUntil = if (failures >= MAX_FAILURES) now + LOCK_MILLIS else null
        attempts.save(username, LoginAttempts(failures, lockedUntil))
    }

    companion object {
        const val MAX_FAILURES = 5
        const val LOCK_MILLIS = 15L * 60 * 1000
    }
}
