package com.usuallycoding.opensecretsanta.server.data.user

import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserCredentials
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.Connection
import java.sql.ResultSet

class SqliteUserRepository(private val database: Database) : UserRepository {

    override fun count(): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM users").use { it.next(); it.getInt(1) } }
    }

    override fun find(username: Username): User? = database.connection().use { find(it, username) }

    override fun findAll(usernames: Collection<Username>): List<User> {
        if (usernames.isEmpty()) return emptyList()
        return database.connection().use { c ->
            c.prepareStatement("SELECT $USER_COLUMNS FROM users WHERE username IN (${placeholders(usernames.size)})").use { s ->
                usernames.forEachIndexed { index, username -> s.setString(index + 1, username.value) }
                s.executeQuery().use { rows -> generateSequence { if (rows.next()) toUser(rows) else null }.toList() }
            }
        }
    }

    override fun credentials(username: Username): UserCredentials? = database.connection().use { c ->
        c.prepareStatement("SELECT password_hash, temp_password_enc FROM users WHERE username = ?").use { s ->
            s.setString(1, username.value)
            s.executeQuery().use { rows ->
                if (rows.next()) UserCredentials(rows.getString(1), rows.getBytes(2)) else null
            }
        }
    }

    override fun existing(usernames: Collection<Username>): Set<Username> {
        if (usernames.isEmpty()) return emptySet()
        return database.connection().use { c ->
            c.prepareStatement("SELECT username FROM users WHERE username IN (${placeholders(usernames.size)})").use { s ->
                usernames.forEachIndexed { index, username -> s.setString(index + 1, username.value) }
                s.executeQuery().use { rows ->
                    generateSequence { if (rows.next()) Username.parse(rows.getString(1)) else null }.toSet()
                }
            }
        }
    }

    override fun insertAll(users: List<NewUser>) {
        if (users.isEmpty()) return
        database.transaction { insertUsers(it, users) }
    }

    override fun updateProfile(username: Username, profile: Profile): User? = database.connection().use { c ->
        val changed = c.prepareStatement(
            "UPDATE users SET full_name = ?, email = ?, phone = ?, address = ?, delivery_info = ? WHERE username = ?",
        ).use { s ->
            s.setString(1, profile.fullName)
            s.setString(2, profile.email)
            s.setString(3, profile.phone)
            s.setString(4, profile.address)
            s.setString(5, profile.deliveryInfo)
            s.setString(6, username.value)
            s.executeUpdate()
        }
        if (changed == 0) null else find(c, username)
    }

    override fun deleteUnassigned(username: Username, createdBy: Username): Boolean = database.transaction { c ->
        // Every statement repeats the eligibility, and the first one already holds the write lock, so a group that names the
        // person meanwhile keeps the account whole. The person is in no group, so what is left of their wishlist is stale.
        val eligible = "EXISTS (SELECT 1 FROM users u WHERE u.username = ? AND u.created_by = ? AND u.is_admin = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM group_participants p WHERE p.username = u.username))"
        for (table in listOf("wishlist_items", "login_attempts")) {
            c.prepareStatement("DELETE FROM $table WHERE username = ? AND $eligible").use { s ->
                s.setString(1, username.value)
                s.setString(2, username.value)
                s.setString(3, createdBy.value)
                s.executeUpdate()
            }
        }
        c.prepareStatement("DELETE FROM users WHERE username = ? AND created_by = ? AND is_admin = 0 AND NOT EXISTS (SELECT 1 FROM group_participants p WHERE p.username = users.username)").use { s ->
            s.setString(1, username.value)
            s.setString(2, createdBy.value)
            s.executeUpdate()
        } == 1
    }

    override fun replaceCredentials(
        username: Username,
        passwordHash: String,
        encryptedTempPassword: ByteArray?,
        mustChangePassword: Boolean,
    ): User? = database.connection().use { c ->
        val changed = c.prepareStatement(
            "UPDATE users SET password_hash = ?, temp_password_enc = ?, must_change_password = ?, " +
                "token_version = token_version + 1 WHERE username = ?",
        ).use { s ->
            s.setString(1, passwordHash)
            s.setBytes(2, encryptedTempPassword)
            s.setInt(3, if (mustChangePassword) 1 else 0)
            s.setString(4, username.value)
            s.executeUpdate()
        }
        if (changed == 0) null else find(c, username)
    }

    private fun find(connection: Connection, username: Username): User? =
        connection.prepareStatement("SELECT $USER_COLUMNS FROM users WHERE username = ?").use { s ->
            s.setString(1, username.value)
            s.executeQuery().use { rows -> if (rows.next()) toUser(rows) else null }
        }

    private fun toUser(rows: ResultSet) = User(
        username = Username.parse(rows.getString("username")),
        profile = Profile(
            fullName = rows.getString("full_name"),
            email = rows.getString("email"),
            phone = rows.getString("phone"),
            address = rows.getString("address"),
            deliveryInfo = rows.getString("delivery_info"),
        ),
        isAdmin = rows.getInt("is_admin") == 1,
        mustChangePassword = rows.getInt("must_change_password") == 1,
        tokenVersion = rows.getInt("token_version"),
    )

    private fun placeholders(count: Int) = List(count) { "?" }.joinToString(", ")

    private companion object {
        const val USER_COLUMNS =
            "username, full_name, email, phone, address, delivery_info, must_change_password, is_admin, token_version"
    }
}

/** Inserts users on an existing connection so that callers can do it inside their own transaction. */
internal fun insertUsers(connection: Connection, users: List<NewUser>) {
    connection.prepareStatement(
        "INSERT INTO users (username, password_hash, temp_password_enc, must_change_password, is_admin, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
    ).use { s ->
        users.forEach { user ->
            s.setString(1, user.username.value)
            s.setString(2, user.passwordHash)
            s.setBytes(3, user.encryptedTempPassword)
            s.setInt(4, if (user.mustChangePassword) 1 else 0)
            s.setInt(5, if (user.isAdmin) 1 else 0)
            s.setString(6, user.createdBy?.value)
            s.executeUpdate()
        }
    }
}
