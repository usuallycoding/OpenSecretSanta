package com.usuallycoding.opensecretsanta.domain.user

class NewUser(
    val username: Username,
    val passwordHash: String,
    val encryptedTempPassword: ByteArray?,
    val mustChangePassword: Boolean,
    val isAdmin: Boolean,
    /** The administrator whose action created the account; none for the first administrator. */
    val createdBy: Username? = null,
)

class UserCredentials(
    val passwordHash: String,
    val encryptedTempPassword: ByteArray?,
)

interface UserRepository {
    fun count(): Int

    fun find(username: Username): User?

    fun findAll(usernames: Collection<Username>): List<User>

    fun credentials(username: Username): UserCredentials?

    /** The subset of [usernames] that already have an account. */
    fun existing(usernames: Collection<Username>): Set<Username>

    /** Inserts all users or none. */
    fun insertAll(users: List<NewUser>)

    fun updateProfile(username: Username, profile: Profile): User?

    /**
     * Deletes the account of [username], with its devices, settings, login record and leftover wishlist items, only if
     * [createdBy] created it, it is not an administrator and no group names it, all decided in the same transaction.
     * Returns whether it was deleted.
     */
    fun deleteUnassigned(username: Username, createdBy: Username): Boolean

    /** Replaces the credentials and increments the user's token version; returns the updated user. */
    fun replaceCredentials(
        username: Username,
        passwordHash: String,
        encryptedTempPassword: ByteArray?,
        mustChangePassword: Boolean,
    ): User?
}
