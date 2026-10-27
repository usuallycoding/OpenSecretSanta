package com.usuallycoding.opensecretsanta.domain.admin

import com.usuallycoding.opensecretsanta.domain.auth.PasswordHasher
import com.usuallycoding.opensecretsanta.domain.auth.SecretBox
import com.usuallycoding.opensecretsanta.domain.auth.TempPasswordGenerator
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * Creates an account, with a generated temporary password, for each username that does not exist yet.
 * Existing users are left alone, and a single invalid entry means nothing is created.
 */
class CreateMissingUsers(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val secretBox: SecretBox,
    private val generator: TempPasswordGenerator,
) {
    operator fun invoke(rawUsernames: List<String>): List<TemporaryCredentials> {
        val requested = rawUsernames.map { Username.parse(it, field = "usernames") }.distinct()
        val existing = users.existing(requested)

        val created = requested.filter { it !in existing }.map { it to generator.generate() }
        users.insertAll(
            created.map { (username, temporaryPassword) ->
                NewUser(
                    username = username,
                    passwordHash = hasher.hash(temporaryPassword),
                    encryptedTempPassword = secretBox.encrypt(temporaryPassword),
                    mustChangePassword = true,
                    isAdmin = false,
                )
            },
        )
        return created.map { (username, temporaryPassword) -> TemporaryCredentials(username, temporaryPassword) }
    }
}
