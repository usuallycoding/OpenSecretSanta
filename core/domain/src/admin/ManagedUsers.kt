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
class ManagedUser(val user: User, val temporaryPassword: String?, val canDelete: Boolean = false)

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
        val deletable = access.deletableUsernames(actor.username)
        return users.findAll(access.managedUsernames(actor.username))
            .sortedBy { it.username.value }
            .map { viewOf(users, secretBox, it, it.username in deletable) }
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
        return viewOf(users, secretBox, user, access.canDelete(actor.username, target))
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

private fun viewOf(users: UserRepository, secretBox: SecretBox, user: User, canDelete: Boolean = false): ManagedUser {
    val temporary = if (user.mustChangePassword) {
        users.credentials(user.username)?.encryptedTempPassword?.let(secretBox::decrypt)
    } else {
        null
    }
    return ManagedUser(user, temporary, canDelete)
}

/**
 * Deletes a stray account: one the administrator created that is in no group. Anything else the administrator may see is
 * refused, and so is a user that joined a group between looking and deleting, which the repository checks again as it deletes.
 */
class DeleteUser(
    private val users: UserRepository,
    private val access: AccessPolicy,
) {
    operator fun invoke(actor: User, rawTarget: String) {
        requireAdmin(actor)
        val target = targetUsername(rawTarget)
        if (!access.canManage(actor.username, target)) throw DomainException.NotFound("User not found.")
        if (!access.canDelete(actor.username, target)) {
            throw DomainException.InvalidState("Only an account you created that is in no group can be deleted.")
        }
        if (!users.deleteUnassigned(target, actor.username)) {
            throw DomainException.InvalidState("Only an account you created that is in no group can be deleted.")
        }
    }
}
