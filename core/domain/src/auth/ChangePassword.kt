package com.usuallycoding.opensecretsanta.domain.auth

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.PasswordPolicy
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

class PasswordChangeResult(val user: User, val token: String)

/**
 * Lets a user replace their password. The change discards any temporary password, clears the
 * must-change flag and bumps the token version, so every earlier token stops working; the
 * returned token keeps the caller logged in.
 */
class ChangePassword(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val tokens: TokenService,
) {
    operator fun invoke(username: Username, currentPassword: String, newPassword: String): PasswordChangeResult {
        val credentials = users.credentials(username) ?: throw DomainException.NotFound()
        if (!hasher.verify(currentPassword, credentials.passwordHash)) {
            throw DomainException.Forbidden("The current password is incorrect.")
        }
        PasswordPolicy.validateNew(newPassword, current = currentPassword)

        val updated = users.replaceCredentials(
            username = username,
            passwordHash = hasher.hash(newPassword),
            encryptedTempPassword = null,
            mustChangePassword = false,
        ) ?: throw DomainException.NotFound()
        return PasswordChangeResult(updated, tokens.issue(updated.username, updated.tokenVersion))
    }
}
