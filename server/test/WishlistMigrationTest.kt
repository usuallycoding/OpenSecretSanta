package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WishlistMigrationTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }

    private fun sql(statement: String) = database.connection().use { c -> c.createStatement().use { it.execute(statement) } }

    private fun count(where: String = "1=1"): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM wishlist_items WHERE $where").use { it.next(); it.getInt(1) } }
    }

    private fun user(name: String) = sql("INSERT OR IGNORE INTO users (username, password_hash) VALUES ('$name', 'h')")

    private fun group(): Int {
        user("boss")
        sql("INSERT INTO groups (owner_username, name, state) VALUES ('boss', 'Party', 'active')")
        return database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT MAX(id) FROM groups").use { it.next(); it.getInt(1) } }
        }
    }

    private fun item(group: Int, owner: String, position: Int, title: String = "Socks") =
        sql("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES ($group, '$owner', $position, '$title', 1, 1)")

    @Test
    fun schemaVersionIsLatest() {
        assertEquals(7, database.schemaVersion())
    }

    @Test
    fun optionalFieldsMayBeEmpty() {
        val g = group()
        user("alice")

        item(g, "alice", 0)

        database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT note, url FROM wishlist_items").use {
                    it.next()
                    assertEquals(null, it.getString(1))
                    assertEquals(null, it.getString(2))
                }
            }
        }
    }

    @Test
    fun aTitleAndTheTimesAreRequired() {
        val g = group()
        user("alice")

        assertFailsWith<SQLException> { sql("INSERT INTO wishlist_items (group_id, username, position, created_at, updated_at) VALUES ($g, 'alice', 0, 1, 1)") }
        assertFailsWith<SQLException> { sql("INSERT INTO wishlist_items (group_id, username, position, title, updated_at) VALUES ($g, 'alice', 0, 't', 1)") }
    }

    @Test
    fun twoItemsOfOneListCannotShareAPlace() {
        val g = group()
        user("alice")
        item(g, "alice", 0)

        assertFailsWith<SQLException> { item(g, "alice", 0, "Another") }
        item(g, "alice", 1, "Another")
        assertEquals(2, count())
    }

    @Test
    fun theSamePlaceIsFineInAnotherGroupOrForAnotherUser() {
        val first = group()
        val second = group()
        user("alice")
        user("bob")

        item(first, "alice", 0)
        item(second, "alice", 0) // same user, other group
        item(first, "bob", 0) // other user, same group

        assertEquals(3, count())
    }

    @Test
    fun anUnknownGroupOrOwnerIsRefused() {
        val g = group()
        user("alice")

        assertFailsWith<SQLException> { item(9999, "alice", 0) }
        assertFailsWith<SQLException> { item(g, "ghost", 0) }
        assertEquals(0, count())
    }

    @Test
    fun deletingAGroupRemovesItsListsButNotOtherGroups() {
        val first = group()
        val second = group()
        user("alice")
        item(first, "alice", 0)
        item(first, "alice", 1)
        item(second, "alice", 0)

        sql("DELETE FROM groups WHERE id = $first")

        assertEquals(1, count())
        assertEquals(1, count("group_id = $second"))
    }

    @Test
    fun negativePositionsAreAllowedSoAReorderCanUseThemTemporarily() {
        val g = group()
        user("alice")

        item(g, "alice", -1)
        item(g, "alice", -2)

        assertEquals(2, count("position < 0"))
    }
}
