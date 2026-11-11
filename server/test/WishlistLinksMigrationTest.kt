package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import kotlin.test.Test
import kotlin.test.assertEquals

class WishlistLinksMigrationTest {
    private data class Item(val id: Long, val title: String, val note: String?, val position: Int, val created: Long, val updated: Long)

    /** A database as it was before V016, with three items (one with a link, one with a blank one, one with none), and then migrated to the end. */
    private fun migrated(): Database {
        val database = Database(newTempDbPath().toString())
        val all = ClasspathMigrations.load()
        MigrationRunner(database, all.filter { it.version < 16 }).migrate()
        database.connection().use { c ->
            c.createStatement().use { s ->
                s.execute("INSERT INTO users (username, password_hash) VALUES ('boss', 'h'), ('alice', 'h')")
                s.execute("INSERT INTO groups (owner_username, name, state) VALUES ('boss', 'Party', 'active')")
                s.execute("INSERT INTO wishlist_items (group_id, username, position, title, note, url, created_at, updated_at) VALUES (1, 'alice', 0, 'Socks', 'Grey', 'https://shop.example/socks', 100, 200)")
                s.execute("INSERT INTO wishlist_items (group_id, username, position, title, note, url, created_at, updated_at) VALUES (1, 'alice', 1, 'Tea', NULL, '  ', 300, 300)")
                s.execute("INSERT INTO wishlist_items (group_id, username, position, title, note, url, created_at, updated_at) VALUES (1, 'alice', 2, 'Mug', 'Blue', NULL, 400, 500)")
            }
        }
        MigrationRunner(database, all).migrate()
        return database
    }

    private fun links(database: Database): List<Triple<Long, Int, String>> = database.connection().use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT item_id, position, url FROM wishlist_item_links ORDER BY item_id, position").use { rows ->
                generateSequence { if (rows.next()) Triple(rows.getLong(1), rows.getInt(2), rows.getString(3)) else null }.toList()
            }
        }
    }

    private fun items(database: Database): List<Item> = database.connection().use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT id, title, note, position, created_at, updated_at FROM wishlist_items ORDER BY id").use { rows ->
                generateSequence {
                    if (rows.next()) Item(rows.getLong(1), rows.getString(2), rows.getString(3), rows.getInt(4), rows.getLong(5), rows.getLong(6)) else null
                }.toList()
            }
        }
    }

    @Test
    fun anExistingLinkBecomesTheFirstAndOnlyLinkOfItsItem() {
        assertEquals(listOf(Triple(1L, 0, "https://shop.example/socks")), links(migrated()))
    }

    @Test
    fun itemsWithoutALinkOrWithABlankOneStayWithoutOne() {
        assertEquals(setOf(1L), links(migrated()).map { it.first }.toSet())
    }

    @Test
    fun everyItemKeepsItsTitleNotePlaceAndTimes() {
        assertEquals(
            listOf(
                Item(1, "Socks", "Grey", 0, 100, 200),
                Item(2, "Tea", null, 1, 300, 300),
                Item(3, "Mug", "Blue", 2, 400, 500),
            ),
            items(migrated()),
        )
    }
}
