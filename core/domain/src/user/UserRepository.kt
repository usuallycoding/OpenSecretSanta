package com.usuallycoding.opensecretsanta.domain.user

class NewUser(
    val username: Username,
    val passwordHash: String,
    val encryptedTempPassword: ByteArray?,
    val mustChangePassword: Boolean,
    val isAdmin: Boolean,
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

    /** Replaces the credentials and increments the user's token version; returns the updated user. */
    fun replaceCredentials(
        username: Username,
        passwordHash: String,
        encryptedTempPassword: ByteArray?,
        mustChangePassword: Boolean,
    ): User?

    fun markAdmin(username: Username): User?
}
