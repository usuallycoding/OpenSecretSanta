package com.usuallycoding.opensecretsanta.domain.admin

import com.usuallycoding.opensecretsanta.domain.auth.LoginAttemptTracker
import com.usuallycoding.opensecretsanta.domain.auth.PasswordHasher
import com.usuallycoding.opensecretsanta.domain.auth.SecretBox
import com.usuallycoding.opensecretsanta.domain.auth.TempPasswordGenerator
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

/** A user as an admin sees them: the temporary password is present only while it is still in force. */
class ManagedUser(val user: User, val temporaryPassword: String?)

class TemporaryCredentials(val username: Username, val temporaryPassword: String)

internal fun requireAdmin(actor: User) {
    if (!actor.isAdmin) throw DomainException.Forbidden("Administrator rights are required.")
}

internal fun targetUsername(raw: String): Username =
    Username.parseOrNull(raw) ?: throw DomainException.NotFound("User not found.")

class ListManagedUsers(
    private val users: UserRepository,
    private val access: AccessPolicy,
    private val secretBox: SecretBox,
) {
    operator fun invoke(actor: User): List<ManagedUser> {
        requireAdmin(actor)
        return users.findAll(access.managedUsernames(actor.username))
            .sortedBy { it.username.value }
            .map { viewOf(users, secretBox, it) }
    }
}

class GetManagedUser(
    private val users: UserRepository,
    private val access: AccessPolicy,
    private val secretBox: SecretBox,
) {
    operator fun invoke(actor: User, rawTarget: String): ManagedUser {
        requireAdmin(actor)
        val target = targetUsername(rawTarget)
        if (!access.canManage(actor.username, target)) throw DomainException.NotFound("User not found.")
        val user = users.find(target) ?: throw DomainException.NotFound("User not found.")
        return viewOf(users, secretBox, user)
    }
}

class ResetPassword(
    private val users: UserRepository,
    private val access: AccessPolicy,
    private val hasher: PasswordHasher,
    private val secretBox: SecretBox,
    private val generator: TempPasswordGenerator,
    private val attempts: LoginAttemptTracker,
) {
    operator fun invoke(actor: User, rawTarget: String): TemporaryCredentials {
        requireAdmin(actor)
        val target = targetUsername(rawTarget)
        if (!access.canManage(actor.username, target)) throw DomainException.NotFound("User not found.")

        val temporaryPassword = generator.generate()
        users.replaceCredentials(
            username = target,
            passwordHash = hasher.hash(temporaryPassword),
            encryptedTempPassword = secretBox.encrypt(temporaryPassword),
            mustChangePassword = true,
        ) ?: throw DomainException.NotFound("User not found.")
        attempts.clear(target)
        return TemporaryCredentials(target, temporaryPassword)
    }
}

private fun viewOf(users: UserRepository, secretBox: SecretBox, user: User): ManagedUser {
    val temporary = if (user.mustChangePassword) {
        users.credentials(user.username)?.encryptedTempPassword?.let(secretBox::decrypt)
    } else {
        null
    }
    return ManagedUser(user, temporary)
}
