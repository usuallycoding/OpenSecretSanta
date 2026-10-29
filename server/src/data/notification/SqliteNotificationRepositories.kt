package com.usuallycoding.opensecretsanta.server.data.notification

import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
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
                    "SELECT username, announcements, wishlist, questions, answers FROM notification_prefs " +
                        "WHERE username IN (${names.joinToString { "?" }})",
                ).use { s ->
                    names.forEachIndexed { i, name -> s.setString(i + 1, name.value) }
                    s.executeQuery().use { rows ->
                        generateSequence {
                            if (rows.next()) {
                                Username.parse(rows.getString(1)) to NotificationSettings(
                                    announcements = rows.getInt(2) == 1,
                                    wishlist = rows.getInt(3) == 1,
                                    questions = rows.getInt(4) == 1,
                                    answers = rows.getInt(5) == 1,
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
                "INSERT INTO notification_prefs (username, announcements, wishlist, questions, answers) VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (username) DO UPDATE SET announcements = excluded.announcements, wishlist = excluded.wishlist, " +
                    "questions = excluded.questions, answers = excluded.answers",
            ).use { s ->
                s.setString(1, username.value)
                s.setInt(2, settings.announcements.toInt())
                s.setInt(3, settings.wishlist.toInt())
                s.setInt(4, settings.questions.toInt())
                s.setInt(5, settings.answers.toInt())
                s.executeUpdate()
            }
        }
    }

    private fun Boolean.toInt() = if (this) 1 else 0

    private companion object {
        const val CHUNK = 500
    }
}
