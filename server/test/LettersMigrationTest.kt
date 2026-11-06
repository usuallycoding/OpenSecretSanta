package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import kotlin.test.Test
import kotlin.test.assertEquals

class LettersMigrationTest {
    @Test
    fun peopleWhoAlreadySavedTheirSwitchesGetLettersOnAndKeepTheRest() {
        val database = Database(newTempDbPath().toString())
        val all = ClasspathMigrations.load()
        MigrationRunner(database, all.filter { it.version < 11 }).migrate()
        database.connection().use { c ->
            c.createStatement().use { s ->
                s.execute("INSERT INTO users (username, password_hash) VALUES ('alice', 'h')")
                s.execute("INSERT INTO notification_prefs (username, announcements, wishlist, questions, answers) VALUES ('alice', 1, 0, 1, 0)")
            }
        }

        MigrationRunner(database, all).migrate()

        val row = database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT announcements, wishlist, questions, answers, letters FROM notification_prefs WHERE username = 'alice'").use { r ->
                    r.next()
                    (1..5).map { r.getInt(it) }
                }
            }
        }
        assertEquals(listOf(1, 0, 1, 0, 1), row)
    }

    @Test
    fun theLettersTableHangsOffTheReceiversAssignment() {
        val database = Database(newTempDbPath().toString())
        MigrationRunner(database, ClasspathMigrations.load()).migrate()

        val columns = database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT name FROM pragma_table_info('letters') ORDER BY cid").use { r ->
                    generateSequence { if (r.next()) r.getString(1) else null }.toList()
                }
            }
        }

        assertEquals(listOf("group_id", "receiver", "number", "title", "text", "sent_at"), columns)
    }
}
