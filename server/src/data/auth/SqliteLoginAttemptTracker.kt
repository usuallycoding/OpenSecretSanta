package com.usuallycoding.opensecretsanta.server.data.auth

import com.usuallycoding.opensecretsanta.domain.auth.LoginAttemptTracker
import com.usuallycoding.opensecretsanta.domain.auth.LoginAttempts
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database

/** Failed-login counters kept in the database so that a restart does not lift a lockout. */
class SqliteLoginAttemptTracker(private val database: Database) : LoginAttemptTracker {

    override fun get(username: Username): LoginAttempts? = database.connection().use { c ->
        c.prepareStatement("SELECT failures, locked_until FROM login_attempts WHERE username = ?").use { s ->
            s.setString(1, username.value)
            s.executeQuery().use { rows ->
                if (rows.next()) {
                    LoginAttempts(rows.getInt(1), rows.getLong(2).takeUnless { rows.wasNull() })
                } else {
                    null
                }
            }
        }
    }

    override fun save(username: Username, attempts: LoginAttempts) {
        database.connection().use { c ->
            c.prepareStatement(
                "INSERT INTO login_attempts (username, failures, locked_until) VALUES (?, ?, ?) " +
                    "ON CONFLICT (username) DO UPDATE SET failures = excluded.failures, locked_until = excluded.locked_until",
            ).use { s ->
                s.setString(1, username.value)
                s.setInt(2, attempts.failures)
                if (attempts.lockedUntilMillis != null) s.setLong(3, attempts.lockedUntilMillis!!) else s.setNull(3, java.sql.Types.INTEGER)
                s.executeUpdate()
            }
        }
    }

    override fun clear(username: Username) {
        database.connection().use { c ->
            c.prepareStatement("DELETE FROM login_attempts WHERE username = ?").use { s ->
                s.setString(1, username.value)
                s.executeUpdate()
            }
        }
    }
}
