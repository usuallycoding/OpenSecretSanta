package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionNumbersMigrationTest {
    /** A database as it was before V017, with alice asking two questions and bob none, and then migrated to the end. */
    private fun migrated(): Database {
        val database = Database(newTempDbPath().toString())
        val all = ClasspathMigrations.load()
        MigrationRunner(database, all.filter { it.version < 17 }).migrate()
        database.connection().use { c ->
            c.createStatement().use { s ->
                s.execute("INSERT INTO users (username, password_hash) VALUES ('boss', 'h')")
                s.execute("INSERT INTO groups (owner_username, name, state) VALUES ('boss', 'Party', 'active')")
                s.execute("INSERT INTO assignments (group_id, santa, receiver) VALUES (1, 'alice', 'bob'), (1, 'bob', 'alice')")
                s.execute("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES (1, 'alice', 1, 'One', 100), (1, 'alice', 2, 'Two', 200)")
            }
        }
        MigrationRunner(database, all).migrate()
        return database
    }

    private fun lastNumbers(database: Database): Map<String, Int> = database.connection().use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT santa, last_question_number FROM assignments").use { rows ->
                generateSequence { if (rows.next()) rows.getString(1) to rows.getInt(2) else null }.toMap()
            }
        }
    }

    @Test
    fun aThreadContinuesAfterItsHighestNumber() {
        assertEquals(2, lastNumbers(migrated())["alice"])
    }

    @Test
    fun aThreadWithoutQuestionsStartsAtZero() {
        assertEquals(0, lastNumbers(migrated())["bob"])
    }
}
