package com.usuallycoding.opensecretsanta.domain.auth

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * The failed-attempt budget of a username: after [MAX_FAILURES] consecutive failures it is locked for [LOCK_MILLIS].
 * Signing in and handing over the administrator role spend the same budget, so there is one lock and no second way
 * to guess a password.
 */
class AttemptLimiter(
    private val attempts: LoginAttemptTracker,
    private val clock: Clock,
) {
    /** What was recorded before, for [recordFailure]; throws `TooManyAttempts` while the username is locked. */
    fun check(username: Username): LoginAttempts? {
        val now = clock.nowMillis()
        val previous = attempts.get(username)
        val lockedUntil = previous?.lockedUntilMillis
        if (lockedUntil != null && lockedUntil > now) {
            throw DomainException.TooManyAttempts(retryAfterSeconds = (lockedUntil - now + 999) / 1000)
        }
        return previous
    }

    fun recordFailure(username: Username, previous: LoginAttempts?) {
        val now = clock.nowMillis()
        val lockExpired = previous?.lockedUntilMillis?.let { it <= now } ?: false
        val failures = (if (previous == null || lockExpired) 0 else previous.failures) + 1
        val lockedUntil = if (failures >= MAX_FAILURES) now + LOCK_MILLIS else null
        attempts.save(username, LoginAttempts(failures, lockedUntil))
    }

    fun clear(username: Username) = attempts.clear(username)

    companion object {
        const val MAX_FAILURES = 5
        const val LOCK_MILLIS = 15L * 60 * 1000
    }
}
