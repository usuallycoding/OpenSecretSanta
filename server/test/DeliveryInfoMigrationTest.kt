package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeliveryInfoMigrationTest {
    /** A database as it was before V010, with alice's saved profile, bob who saved nothing, and two groups, then migrated. */
    private fun migrated(): Database {
        val database = Database(newTempDbPath().toString())
        val all = ClasspathMigrations.load()
        MigrationRunner(database, all.filter { it.version < 10 }).migrate()
        database.connection().use { c ->
            c.createStatement().use { s ->
                s.execute("INSERT INTO users (username, password_hash, full_name, email, phone, address, delivery_info) VALUES ('alice', 'h', 'Alice A', 'a@example.org', '555', '1 Main St', 'ring twice')")
                s.execute("INSERT INTO users (username, password_hash) VALUES ('bob', 'h')")
                s.execute("INSERT INTO users (username, password_hash, address) VALUES ('carol', 'h', '9 Side Rd')")
                s.execute("INSERT INTO users (username, password_hash) VALUES ('boss', 'h')")
                s.execute("INSERT INTO groups (owner_username, name, state) VALUES ('boss', 'One', 'active')")
                s.execute("INSERT INTO groups (owner_username, name, state) VALUES ('boss', 'Two', 'draft')")
                s.execute("INSERT INTO group_participants (group_id, username, position) VALUES (1, 'alice', 0), (1, 'bob', 1), (2, 'alice', 0), (2, 'newcomer', 1)")
            }
        }
        MigrationRunner(database, all).migrate()
        return database
    }

    private fun rows(database: Database): List<List<String?>> = database.connection().use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT group_id, username, full_name, email, phone, address, delivery_info FROM group_delivery_info ORDER BY group_id, username").use { r ->
                generateSequence { if (r.next()) (1..7).map { r.getString(it) } else null }.toList()
            }
        }
    }

    @Test
    fun aSavedProfileIsCopiedIntoEveryGroupThePersonIsIn() {
        val saved = rows(migrated())

        assertEquals(
            listOf(
                listOf("1", "alice", "Alice A", "a@example.org", "555", "1 Main St", "ring twice"),
                listOf("2", "alice", "Alice A", "a@example.org", "555", "1 Main St", "ring twice"),
            ),
            saved,
        )
    }

    @Test
    fun someoneWhoSavedNothingOrIsInNoGroupGetsNoRow() {
        val users = rows(migrated()).map { it[1] }

        assertFalse("bob" in users)
        assertFalse("carol" in users)
        assertFalse("newcomer" in users)
    }

    @Test
    fun theAccountsNoLongerHoldTheProfile() {
        val database = migrated()
        val columns = database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT name FROM pragma_table_info('users')").use { r -> generateSequence { if (r.next()) r.getString(1) else null }.toSet() }
            }
        }

        for (gone in listOf("full_name", "email", "phone", "address", "delivery_info")) assertFalse(gone in columns, gone)
        assertTrue("username" in columns && "password_hash" in columns)
    }

    @Test
    fun deletingAGroupTakesItsDeliveryInfoWithIt() {
        val database = migrated()
        database.connection().use { c ->
            c.createStatement().use { it.execute("PRAGMA foreign_keys = ON"); it.execute("DELETE FROM groups WHERE id = 1") }
        }

        assertEquals(listOf("2"), rows(database).map { it[0] })
    }
}
