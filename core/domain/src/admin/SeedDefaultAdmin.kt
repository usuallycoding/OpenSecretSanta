package com.usuallycoding.opensecretsanta.domain.admin

import com.usuallycoding.opensecretsanta.domain.auth.PasswordHasher
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.PasswordPolicy
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * Creates the first administrator when no user exists yet. The password is a one-time secret the
 * operator already knows, so no retrievable copy is kept; the admin must replace it at first login.
 */
class SeedDefaultAdmin(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
) {
    /** Returns the created admin, or null when users already exist and nothing was done. */
    operator fun invoke(rawUsername: String, password: String?): User? {
        if (users.count() > 0) return null

        val username = Username.parse(rawUsername, field = "ADMIN_USERNAME")
        if (password.isNullOrBlank()) {
            throw DomainException.ValidationFailed("ADMIN_PASSWORD", "a password is required when the database has no users.")
        }
        PasswordPolicy.validateNew(password, field = "ADMIN_PASSWORD")

        users.insertAll(
            listOf(
                NewUser(
                    username = username,
                    passwordHash = hasher.hash(password),
                    encryptedTempPassword = null,
                    mustChangePassword = true,
                    isAdmin = true,
                ),
            ),
        )
        return users.find(username)
    }
}
