package com.usuallycoding.opensecretsanta.domain.auth

import com.usuallycoding.opensecretsanta.domain.user.Username

fun interface Clock {
    fun nowMillis(): Long
}

interface PasswordHasher {
    fun hash(password: String): String

    fun verify(password: String, hash: String): Boolean
}

class TokenClaims(val username: Username, val tokenVersion: Int)

interface TokenService {
    fun issue(username: Username, tokenVersion: Int): String

    /** The claims of a genuine, unexpired token, or null. */
    fun verify(token: String): TokenClaims?
}

/** Reversible encryption for the few secrets that must be shown again, such as temporary passwords. */
interface SecretBox {
    fun encrypt(plain: String): ByteArray

    fun decrypt(encrypted: ByteArray): String
}

fun interface TempPasswordGenerator {
    fun generate(): String
}

class LoginAttempts(val failures: Int, val lockedUntilMillis: Long?)

interface LoginAttemptTracker {
    fun get(username: Username): LoginAttempts?

    fun save(username: Username, attempts: LoginAttempts)

    fun clear(username: Username)
}
