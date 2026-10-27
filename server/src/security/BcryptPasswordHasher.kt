package com.usuallycoding.opensecretsanta.server.security

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import com.usuallycoding.opensecretsanta.domain.auth.PasswordHasher

/**
 * bcrypt with a SHA-512 pre-hash for passwords longer than bcrypt's 72-byte limit, so that long
 * passwords are neither rejected nor silently truncated.
 */
class BcryptPasswordHasher(private val cost: Int = DEFAULT_COST) : PasswordHasher {
    private val version = BCrypt.Version.VERSION_2A
    private val hasher = BCrypt.with(version, LongPasswordStrategies.hashSha512(version))
    private val verifier = BCrypt.verifyer(version, LongPasswordStrategies.hashSha512(version))

    override fun hash(password: String): String = hasher.hashToString(cost, password.toCharArray())

    override fun verify(password: String, hash: String): Boolean =
        verifier.verify(password.toCharArray(), hash).verified

    companion object {
        const val DEFAULT_COST = 12
    }
}
