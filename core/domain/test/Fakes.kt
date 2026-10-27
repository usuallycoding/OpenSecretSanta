package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.GroupMembershipRepository
import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.auth.LoginAttemptTracker
import com.usuallycoding.opensecretsanta.domain.auth.LoginAttempts
import com.usuallycoding.opensecretsanta.domain.auth.PasswordHasher
import com.usuallycoding.opensecretsanta.domain.auth.SecretBox
import com.usuallycoding.opensecretsanta.domain.auth.TempPasswordGenerator
import com.usuallycoding.opensecretsanta.domain.auth.TokenClaims
import com.usuallycoding.opensecretsanta.domain.auth.TokenService
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserCredentials
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

fun u(name: String) = Username.parse(name)

class FakeClock(var now: Long = 1_000_000L) : Clock {
    override fun nowMillis() = now
    fun advanceMinutes(minutes: Long) {
        now += minutes * 60_000
    }
}

class FakeHasher : PasswordHasher {
    var verifyCalls = 0
    override fun hash(password: String) = "hash:$password"
    override fun verify(password: String, hash: String): Boolean {
        verifyCalls++
        return hash == "hash:$password"
    }
}

class FakeTokens : TokenService {
    override fun issue(username: Username, tokenVersion: Int) = "token:${username.value}:$tokenVersion"
    override fun verify(token: String): TokenClaims? {
        val parts = token.split(":")
        if (parts.size != 3 || parts[0] != "token") return null
        return TokenClaims(Username.parse(parts[1]), parts[2].toInt())
    }
}

class FakeSecretBox : SecretBox {
    override fun encrypt(plain: String) = "enc:$plain".encodeToByteArray()
    override fun decrypt(encrypted: ByteArray) = encrypted.decodeToString().removePrefix("enc:")
}

class SequenceTempPasswords : TempPasswordGenerator {
    private var counter = 0
    override fun generate() = "TempPass" + (++counter).toString().padStart(4, '0')
}

class InMemoryAttempts : LoginAttemptTracker {
    val store = mutableMapOf<Username, LoginAttempts>()
    override fun get(username: Username) = store[username]
    override fun save(username: Username, attempts: LoginAttempts) {
        store[username] = attempts
    }
    override fun clear(username: Username) {
        store.remove(username)
    }
}

class FakeMemberships(var owned: Map<Username, Set<Username>> = emptyMap()) : GroupMembershipRepository {
    override fun membersOfGroupsOwnedBy(owner: Username): Set<Username> = owned[owner].orEmpty()
}

class InMemoryUsers : UserRepository {
    private class Row(
        var user: User,
        var passwordHash: String,
        var encryptedTemp: ByteArray?,
    )

    private val rows = linkedMapOf<Username, Row>()

    override fun count() = rows.size
    override fun find(username: Username) = rows[username]?.user
    override fun findAll(usernames: Collection<Username>) = usernames.mapNotNull { rows[it]?.user }
    override fun credentials(username: Username) =
        rows[username]?.let { UserCredentials(it.passwordHash, it.encryptedTemp) }

    override fun existing(usernames: Collection<Username>) = usernames.filter { it in rows }.toSet()

    override fun insertAll(users: List<NewUser>) {
        require(users.none { it.username in rows }) { "duplicate username" }
        users.forEach {
            rows[it.username] = Row(
                User(it.username, Profile.EMPTY, it.isAdmin, it.mustChangePassword, tokenVersion = 1),
                it.passwordHash,
                it.encryptedTempPassword,
            )
        }
    }

    override fun updateProfile(username: Username, profile: Profile): User? =
        rows[username]?.also { it.user = it.user.copy(profile = profile) }?.user

    override fun replaceCredentials(
        username: Username,
        passwordHash: String,
        encryptedTempPassword: ByteArray?,
        mustChangePassword: Boolean,
    ): User? = rows[username]?.also {
        it.passwordHash = passwordHash
        it.encryptedTemp = encryptedTempPassword
        it.user = it.user.copy(mustChangePassword = mustChangePassword, tokenVersion = it.user.tokenVersion + 1)
    }?.user

    override fun markAdmin(username: Username): User? =
        rows[username]?.also { it.user = it.user.copy(isAdmin = true) }?.user

    fun add(
        name: String,
        password: String = "password1",
        admin: Boolean = false,
        mustChange: Boolean = false,
        tempPlain: String? = null,
    ): Username {
        val username = u(name)
        insertAll(
            listOf(
                NewUser(
                    username,
                    "hash:$password",
                    tempPlain?.let { FakeSecretBox().encrypt(it) },
                    mustChange,
                    admin,
                ),
            ),
        )
        return username
    }
}
