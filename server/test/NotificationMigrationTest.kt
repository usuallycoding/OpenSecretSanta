package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationMigrationTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }

    private fun sql(statement: String) = database.connection().use { c -> c.createStatement().use { it.execute(statement) } }

    private fun count(table: String): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM $table").use { it.next(); it.getInt(1) } }
    }

    private fun user(name: String) = sql("INSERT INTO users (username, password_hash) VALUES ('$name', 'h')")

    private fun device(token: String, owner: String, platform: String = "android") =
        sql("INSERT INTO push_devices (token, username, platform, created_at) VALUES ('$token', '$owner', '$platform', 1)")

    @Test
    fun aTokenBelongsToOnePersonAtATime() {
        user("alice")
        device("t1", "alice")

        assertFailsWith<SQLException> { device("t1", "alice") }
        assertEquals(1, count("push_devices"))
    }

    @Test
    fun onlyAndroidAndWebAreKnownPlatforms() {
        user("alice")

        assertFailsWith<SQLException> { device("t1", "alice", "ios") }
        device("t1", "alice", "android")
        device("t2", "alice", "web")
        assertEquals(2, count("push_devices"))
    }

    @Test
    fun aDeviceNeedsAnExistingPerson() {
        assertFailsWith<SQLException> { device("t1", "ghost") }
    }

    @Test
    fun theSwitchesDefaultToOnAndHoldOnlyZeroOrOne() {
        user("alice")

        sql("INSERT INTO notification_prefs (username) VALUES ('alice')")

        database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT announcements, wishlist, questions, answers FROM notification_prefs").use {
                    it.next()
                    assertEquals(listOf(1, 1, 1, 1), (1..4).map(it::getInt))
                }
            }
        }
        assertFailsWith<SQLException> { sql("UPDATE notification_prefs SET questions = 2") }
    }

    @Test
    fun deletingAPersonRemovesTheirDevicesAndChoices() {
        user("alice")
        user("bob")
        device("t1", "alice")
        device("t2", "bob")
        sql("INSERT INTO notification_prefs (username) VALUES ('alice')")
        sql("INSERT INTO notification_prefs (username) VALUES ('bob')")

        sql("DELETE FROM users WHERE username = 'alice'")

        assertEquals(1, count("push_devices"))
        assertEquals(1, count("notification_prefs"))
    }
}
