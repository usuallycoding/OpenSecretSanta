package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupMigrationTest {
    private val database = Database(newTempDbPath().toString()).also {
        MigrationRunner(it, ClasspathMigrations.load()).migrate()
    }

    private fun sql(statement: String) = database.connection().use { c -> c.createStatement().use { it.execute(statement) } }

    private fun count(table: String, where: String = "1=1"): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM $table WHERE $where").use { it.next(); it.getInt(1) } }
    }

    private fun hasTable(name: String): Boolean = count("sqlite_master", "type = 'table' AND name = '$name'") == 1

    private fun newGroup(owner: String = "boss", name: String = "Party"): Int {
        sql("INSERT OR IGNORE INTO users (username, password_hash) VALUES ('$owner', 'h')")
        sql("INSERT INTO groups (owner_username, name) VALUES ('$owner', '$name')")
        return database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT MAX(id) FROM groups").use { it.next(); it.getInt(1) } }
        }
    }

    @Test
    fun schemaVersionIsFour() {
        assertEquals(4, database.schemaVersion())
    }

    @Test
    fun theOldMembershipTableIsGone() {
        assertFalse(hasTable("group_members"))
        assertTrue(hasTable("group_participants"))
        assertTrue(hasTable("assignments"))
    }

    @Test
    fun newGroupsStartInDraftWithOptionalFieldsEmpty() {
        val id = newGroup()

        database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT state, description, location, event_date, event_time FROM groups WHERE id = $id").use {
                    it.next()
                    assertEquals("draft", it.getString(1))
                    assertEquals(null, it.getString(2))
                    assertEquals(null, it.getString(3))
                    assertEquals(null, it.getString(4))
                    assertEquals(null, it.getString(5))
                }
            }
        }
    }

    @Test
    fun unknownStatesAreRejected() {
        val id = newGroup()
        assertFailsWith<SQLException> { sql("UPDATE groups SET state = 'finished' WHERE id = $id") }
        for (state in listOf("draft", "pending", "active", "archived")) sql("UPDATE groups SET state = '$state' WHERE id = $id")
    }

    @Test
    fun groupNameAndOwnerAreRequired() {
        assertFailsWith<SQLException> { sql("INSERT INTO groups (owner_username) VALUES ('boss')") }
        newGroup()
        assertFailsWith<SQLException> { sql("INSERT INTO groups (owner_username, name) VALUES ('nobody', 'x')") }
    }

    @Test
    fun participantsMayBeNamesWithoutAnAccount() {
        val id = newGroup()

        sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, 'not-registered-yet', 0)")

        assertEquals(1, count("group_participants", "group_id = $id"))
        assertEquals(0, count("users", "username = 'not-registered-yet'"))
    }

    @Test
    fun participantsAreUniquePerGroupAndStoredLowercase() {
        val id = newGroup()
        sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, 'alice', 0)")

        assertFailsWith<SQLException> { sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, 'alice', 1)") }
        assertFailsWith<SQLException> { sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, 'Bob', 1)") }
        sql("INSERT INTO group_participants (group_id, username, position) VALUES (${newGroup(name = "Other")}, 'alice', 0)")
    }

    @Test
    fun aSelfAssignmentIsRejected() {
        val id = newGroup()
        assertFailsWith<SQLException> { sql("INSERT INTO assignments (group_id, santa, receiver) VALUES ($id, 'alice', 'alice')") }
    }

    @Test
    fun aReceiverCannotBeDrawnTwiceAndASantaCannotGiveTwice() {
        val id = newGroup()
        sql("INSERT INTO assignments (group_id, santa, receiver) VALUES ($id, 'alice', 'bob')")

        assertFailsWith<SQLException> { sql("INSERT INTO assignments (group_id, santa, receiver) VALUES ($id, 'carol', 'bob')") }
        assertFailsWith<SQLException> { sql("INSERT INTO assignments (group_id, santa, receiver) VALUES ($id, 'alice', 'carol')") }
        sql("INSERT INTO assignments (group_id, santa, receiver) VALUES (${newGroup(name = "Other")}, 'carol', 'bob')")
    }

    @Test
    fun deletingAGroupRemovesItsParticipantsAndAssignments() {
        val id = newGroup()
        sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, 'alice', 0), ($id, 'bob', 1)")
        sql("INSERT INTO assignments (group_id, santa, receiver) VALUES ($id, 'alice', 'bob'), ($id, 'bob', 'alice')")

        sql("DELETE FROM groups WHERE id = $id")

        assertEquals(0, count("group_participants"))
        assertEquals(0, count("assignments"))
    }

    @Test
    fun rerunningTheMigrationsChangesNothing() {
        val id = newGroup()
        sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, 'alice', 0)")

        MigrationRunner(database, ClasspathMigrations.load()).migrate()

        assertEquals(1, count("groups"))
        assertEquals(1, count("group_participants"))
    }
}
