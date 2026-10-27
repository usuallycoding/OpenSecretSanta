package com.usuallycoding.opensecretsanta.domain.admin

import com.usuallycoding.opensecretsanta.domain.auth.PasswordHasher
import com.usuallycoding.opensecretsanta.domain.auth.SecretBox
import com.usuallycoding.opensecretsanta.domain.auth.TempPasswordGenerator
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username

/** An account about to be created, together with the clear temporary password to hand out. */
class PlannedAccount(val newUser: NewUser, val temporaryPassword: String) {
    val username: Username get() = newUser.username
}

/**
 * Works out which accounts are missing and prepares them (hashed and encrypted passwords) without
 * storing anything, so that the caller can store them together with other changes in one transaction.
 */
class NewAccountPlanner(
    private val hasher: PasswordHasher,
    private val secretBox: SecretBox,
    private val generator: TempPasswordGenerator,
) {
    fun plan(usernames: Collection<Username>, existing: Set<Username>): List<PlannedAccount> =
        usernames.distinct().filter { it !in existing }.map { username ->
            val temporaryPassword = generator.generate()
            PlannedAccount(
                newUser = NewUser(
                    username = username,
                    passwordHash = hasher.hash(temporaryPassword),
                    encryptedTempPassword = secretBox.encrypt(temporaryPassword),
                    mustChangePassword = true,
                    isAdmin = false,
                ),
                temporaryPassword = temporaryPassword,
            )
        }
}
