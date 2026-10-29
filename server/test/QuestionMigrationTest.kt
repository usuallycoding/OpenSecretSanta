package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuestionMigrationTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }

    private fun sql(statement: String) = database.connection().use { c -> c.createStatement().use { it.execute(statement) } }

    private fun count(table: String, where: String = "1=1"): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM $table WHERE $where").use { it.next(); it.getInt(1) } }
    }

    /** A group with the assignment alice -> bob; returns its id. */
    private fun groupWithAssignment(): Int {
        sql("INSERT OR IGNORE INTO users (username, password_hash) VALUES ('boss', 'h')")
        sql("INSERT INTO groups (owner_username, name, state) VALUES ('boss', 'Party', 'active')")
        val id = database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT MAX(id) FROM groups").use { it.next(); it.getInt(1) } }
        }
        sql("INSERT INTO assignments (group_id, santa, receiver) VALUES ($id, 'alice', 'bob'), ($id, 'bob', 'alice')")
        return id
    }

    @Test
    fun schemaVersionIsLatest() {
        assertEquals(7, database.schemaVersion())
    }

    @Test
    fun aQuestionBelongsToASantasAssignment() {
        val id = groupWithAssignment()

        sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'alice', 1, 'What size?', 1000)")

        assertEquals(1, count("questions"))
        assertFailsWith<SQLException> { sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'carol', 1, 'No assignment', 1000)") }
        assertFailsWith<SQLException> { sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES (9999, 'alice', 1, 'No group', 1000)") }
    }

    @Test
    fun numbersAreUniquePerSantaAndStartAtOne() {
        val id = groupWithAssignment()
        sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'alice', 1, 'One', 1000)")

        assertFailsWith<SQLException> { sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'alice', 1, 'Duplicate', 1001)") }
        assertFailsWith<SQLException> { sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'alice', 0, 'Zero', 1001)") }
        sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'bob', 1, 'Another Santa may use 1 too', 1001)")
        assertEquals(2, count("questions"))
    }

    @Test
    fun anAnswerAlwaysComesWithItsTimes() {
        val id = groupWithAssignment()
        sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'alice', 1, 'One', 1000)")

        assertFailsWith<SQLException> { sql("UPDATE questions SET answer = 'Medium' WHERE group_id = $id") }
        sql("UPDATE questions SET answer = 'Medium', answered_at = 2000, answer_updated_at = 2000 WHERE group_id = $id")
        assertFailsWith<SQLException> { sql("UPDATE questions SET answered_at = NULL WHERE group_id = $id") }
    }

    @Test
    fun deletingTheGroupRemovesItsQuestions() {
        val id = groupWithAssignment()
        sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'alice', 1, 'One', 1000), ($id, 'bob', 1, 'Two', 1000)")

        sql("DELETE FROM groups WHERE id = $id")

        assertEquals(0, count("questions"))
        assertEquals(0, count("assignments"))
    }
}
