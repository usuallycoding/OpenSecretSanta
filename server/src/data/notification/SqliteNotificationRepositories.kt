package com.usuallycoding.opensecretsanta.server.data.notification

import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.notification.DigestMarkRepository
import com.usuallycoding.opensecretsanta.domain.notification.DeviceRepository
import com.usuallycoding.opensecretsanta.domain.notification.NotificationSettings
import com.usuallycoding.opensecretsanta.domain.notification.NotificationSettingsRepository
import com.usuallycoding.opensecretsanta.domain.notification.RegisteredDevice
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.ResultSet

class SqliteDeviceRepository(private val database: Database) : DeviceRepository {

    override fun register(username: Username, token: String, platform: DevicePlatform, atMillis: Long) {
        database.connection().use { c ->
            // the token is the key: registering it again, or for someone else, replaces the row
            c.prepareStatement(
                "INSERT INTO push_devices (token, username, platform, created_at) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (token) DO UPDATE SET username = excluded.username, platform = excluded.platform, created_at = excluded.created_at",
            ).use { s ->
                s.setString(1, token)
                s.setString(2, username.value)
                s.setString(3, platform.name.lowercase())
                s.setLong(4, atMillis)
                s.executeUpdate()
            }
        }
    }

    override fun remove(username: Username, token: String) {
        database.connection().use { c ->
            c.prepareStatement("DELETE FROM push_devices WHERE token = ? AND username = ?").use { s ->
                s.setString(1, token)
                s.setString(2, username.value)
                s.executeUpdate()
            }
        }
    }

    override fun removeInvalid(tokens: Collection<String>) {
        if (tokens.isEmpty()) return
        database.transaction { c ->
            c.prepareStatement("DELETE FROM push_devices WHERE token = ?").use { s ->
                tokens.forEach { token ->
                    s.setString(1, token)
                    s.executeUpdate()
                }
            }
        }
    }

    override fun devicesOf(usernames: Collection<Username>): List<RegisteredDevice> {
        if (usernames.isEmpty()) return emptyList()
        return database.connection().use { c ->
            usernames.chunked(CHUNK).flatMap { names ->
                c.prepareStatement(
                    "SELECT token, username, platform FROM push_devices WHERE username IN (${names.joinToString { "?" }}) ORDER BY created_at, token",
                ).use { s ->
                    names.forEachIndexed { i, name -> s.setString(i + 1, name.value) }
                    s.executeQuery().use { rows -> generateSequence { if (rows.next()) toDevice(rows) else null }.toList() }
                }
            }
        }
    }

    private fun toDevice(row: ResultSet) = RegisteredDevice(
        token = row.getString(1),
        username = Username.parse(row.getString(2)),
        platform = DevicePlatform.valueOf(row.getString(3).uppercase()),
    )

    private companion object {
        /** Stays well under SQLite's limit on the number of parameters in one statement. */
        const val CHUNK = 500
    }
}

class SqliteNotificationSettingsRepository(private val database: Database) : NotificationSettingsRepository {

    override fun find(username: Username): NotificationSettings = findAll(listOf(username))[username] ?: NotificationSettings()

    override fun findAll(usernames: Collection<Username>): Map<Username, NotificationSettings> {
        if (usernames.isEmpty()) return emptyMap()
        val saved = database.connection().use { c ->
            usernames.distinct().chunked(CHUNK).flatMap { names ->
                c.prepareStatement(
                    "SELECT username, announcements, daily FROM notification_prefs " +
                        "WHERE username IN (${names.joinToString { "?" }})",
                ).use { s ->
                    names.forEachIndexed { i, name -> s.setString(i + 1, name.value) }
                    s.executeQuery().use { rows ->
                        generateSequence {
                            if (rows.next()) {
                                Username.parse(rows.getString(1)) to NotificationSettings(
                                    announcements = rows.getInt(2) == 1,
                                    daily = rows.getInt(3) == 1,
                                )
                            } else {
                                null
                            }
                        }.toList()
                    }
                }
            }
        }.toMap()
        return usernames.associateWith { saved[it] ?: NotificationSettings() }
    }

    override fun save(username: Username, settings: NotificationSettings) {
        database.connection().use { c ->
            c.prepareStatement(
                // the four older columns follow the digest switch, so that a server from before it reads the same choice
                "INSERT INTO notification_prefs (username, announcements, wishlist, questions, answers, letters, daily) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (username) DO UPDATE SET announcements = excluded.announcements, wishlist = excluded.wishlist, " +
                    "questions = excluded.questions, answers = excluded.answers, letters = excluded.letters, daily = excluded.daily",
            ).use { s ->
                s.setString(1, username.value)
                s.setInt(2, settings.announcements.toInt())
                val daily = settings.daily.toInt()
                s.setInt(3, daily)
                s.setInt(4, daily)
                s.setInt(5, daily)
                s.setInt(6, daily)
                s.setInt(7, daily)
                s.executeUpdate()
            }
        }
    }

    private fun Boolean.toInt() = if (this) 1 else 0

    private companion object {
        const val CHUNK = 500
    }
}

class SqliteDigestMarkRepository(private val database: Database) : DigestMarkRepository {

    override fun mark(username: Username, atMillis: Long) {
        database.connection().use { c ->
            // a person already marked keeps the earlier time
            c.prepareStatement("INSERT INTO digest_marks (username, marked_at) VALUES (?, ?) ON CONFLICT (username) DO NOTHING").use { s ->
                s.setString(1, username.value)
                s.setLong(2, atMillis)
                s.executeUpdate()
            }
        }
    }

    override fun takeAll(): List<Username> =
        database.transaction { c ->
            val marked = c.prepareStatement("SELECT username FROM digest_marks ORDER BY marked_at, username").use { s ->
                s.executeQuery().use { rows -> generateSequence { if (rows.next()) Username.parse(rows.getString(1)) else null }.toList() }
            }
            c.prepareStatement("DELETE FROM digest_marks").use { it.executeUpdate() }
            marked
        }
}
