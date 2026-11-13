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
                s.executeQuery("SELECT announcements, wishlist, questions, answers, daily FROM notification_prefs").use {
                    it.next()
                    assertEquals(listOf(1, 1, 1, 1, 1), (1..5).map(it::getInt))
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

    @Test
    fun theDigestSwitchIsOnForAnyoneWhoHadAnyOfTheFourOnAndOffOnlyIfAllWereOff() {
        val old = temporaryDatabase()
        val before = ClasspathMigrations.load().filter { it.version < 18 }
        MigrationRunner(old, before).migrate()
        fun run(statement: String) = old.connection().use { c -> c.createStatement().use { it.execute(statement) } }
        listOf("some", "allon", "alloff").forEach { run("INSERT INTO users (username, password_hash) VALUES ('$it', 'h')") }
        run("INSERT INTO notification_prefs (username, wishlist, questions, answers, letters) VALUES ('some', 0, 0, 1, 0)")
        run("INSERT INTO notification_prefs (username) VALUES ('allon')")
        run("INSERT INTO notification_prefs (username, wishlist, questions, answers, letters) VALUES ('alloff', 0, 0, 0, 0)")

        MigrationRunner(old, ClasspathMigrations.load()).migrate()

        val daily = old.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT username, daily FROM notification_prefs ORDER BY username").use { rows ->
                    generateSequence { if (rows.next()) rows.getString(1) to rows.getInt(2) else null }.toList()
                }
            }
        }
        assertEquals(listOf("alloff" to 0, "allon" to 1, "some" to 1), daily)
    }

    @Test
    fun aMarkIsOnePerPersonAndGoesWhenThePersonDoes() {
        user("alice")
        user("bob")
        sql("INSERT INTO digest_marks (username, marked_at) VALUES ('alice', 1)")
        sql("INSERT INTO digest_marks (username, marked_at) VALUES ('bob', 2)")

        assertFailsWith<SQLException> { sql("INSERT INTO digest_marks (username, marked_at) VALUES ('alice', 3)") }
        sql("DELETE FROM users WHERE username = 'alice'")

        assertEquals(1, count("digest_marks"))
    }

    private fun temporaryDatabase() = Database(newTempDbPath().toString())
}
