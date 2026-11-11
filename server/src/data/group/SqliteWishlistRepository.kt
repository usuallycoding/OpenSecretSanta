package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistEntry
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistRepository
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * A list is the set of rows sharing `(group_id, username)`, ordered by `position`. Every write first
 * runs `UPDATE groups SET state = state WHERE id = ? AND state IN ('pending', 'active')`, which
 * checks the group is editable and takes SQLite's write lock, and then confirms the owner still
 * participates, so the count, the cap and the position arithmetic cannot interleave with another
 * writer or with a participant being removed. An item's links are the rows of `wishlist_item_links` sharing its `item_id`,
 * ordered by `position`; they are only ever replaced as a whole, in the transaction that changes the item.
 */
class SqliteWishlistRepository(private val database: Database) : WishlistRepository {

    override fun list(groupId: Long, owner: Username): List<WishlistItem> =
        database.connection().use { listOn(it, groupId, owner) }

    override fun add(groupId: Long, owner: Username, entry: WishlistEntry, atMillis: Long, maxItems: Int): WishlistItem =
        database.transaction { c ->
            lockForWrite(c, groupId, owner)
            val (highest, count) = c.prepareStatement(
                "SELECT COALESCE(MAX(position), -1), COUNT(*) FROM wishlist_items WHERE group_id = ? AND username = ?",
            ).use { s ->
                s.setLong(1, groupId)
                s.setString(2, owner.value)
                s.executeQuery().use { it.next(); it.getInt(1) to it.getInt(2) }
            }
            if (count >= maxItems) throw DomainException.InvalidState("The wishlist is full.")

            val id = c.prepareStatement(
                "INSERT INTO wishlist_items (group_id, username, position, title, note, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            ).use { s ->
                s.setLong(1, groupId)
                s.setString(2, owner.value)
                s.setInt(3, highest + 1)
                s.setString(4, entry.title)
                s.setString(5, entry.note)
                s.setLong(6, atMillis)
                s.setLong(7, atMillis)
                s.executeUpdate()
                s.generatedKeys.use { keys -> keys.next(); keys.getLong(1) }
            }
            writeLinks(c, id, entry.urls)
            WishlistItem(id, entry.title, entry.note, entry.urls, atMillis, atMillis)
        }

    override fun update(groupId: Long, owner: Username, itemId: Long, entry: WishlistEntry, atMillis: Long): WishlistItem =
        database.transaction { c ->
            lockForWrite(c, groupId, owner)
            val current = listOn(c, groupId, owner).firstOrNull { it.id == itemId } ?: throw DomainException.NotFound("Item not found.")
            c.prepareStatement(
                "UPDATE wishlist_items SET title = ?, note = ?, updated_at = ? WHERE id = ? AND group_id = ? AND username = ?",
            ).use { s ->
                s.setString(1, entry.title)
                s.setString(2, entry.note)
                s.setLong(3, atMillis)
                s.setLong(4, itemId)
                s.setLong(5, groupId)
                s.setString(6, owner.value)
                s.executeUpdate()
            }
            writeLinks(c, itemId, entry.linksOver(current.urls))
            listOn(c, groupId, owner).first { it.id == itemId }
        }

    override fun delete(groupId: Long, owner: Username, itemId: Long) {
        database.transaction { c ->
            lockForWrite(c, groupId, owner)
            val changed = c.prepareStatement("DELETE FROM wishlist_items WHERE id = ? AND group_id = ? AND username = ?").use { s ->
                s.setLong(1, itemId)
                s.setLong(2, groupId)
                s.setString(3, owner.value)
                s.executeUpdate()
            }
            if (changed == 0) throw DomainException.NotFound("Item not found.")
        }
    }

    override fun reorder(groupId: Long, owner: Username, orderedIds: List<Long>): List<WishlistItem> =
        database.transaction { c ->
            lockForWrite(c, groupId, owner)
            val current = listOn(c, groupId, owner).map { it.id }
            if (orderedIds.size != current.size || orderedIds.toSet() != current.toSet()) {
                throw DomainException.ValidationFailed("itemIds", "The ids must be exactly your current items, each once.")
            }

            // Uniqueness is checked per statement, so first park the whole list on negative positions
            // (which no real position uses), then write the final places.
            c.prepareStatement("UPDATE wishlist_items SET position = -1 - position WHERE group_id = ? AND username = ?").use { s ->
                s.setLong(1, groupId)
                s.setString(2, owner.value)
                s.executeUpdate()
            }
            c.prepareStatement("UPDATE wishlist_items SET position = ? WHERE id = ?").use { s ->
                orderedIds.forEachIndexed { index, id ->
                    s.setInt(1, index)
                    s.setLong(2, id)
                    s.executeUpdate()
                }
            }
            listOn(c, groupId, owner)
        }

    private fun lockForWrite(c: Connection, groupId: Long, owner: Username) {
        val editable = c.createStatement().use {
            it.executeUpdate("UPDATE groups SET state = state WHERE id = $groupId AND state IN ('pending', 'active')")
        }
        if (editable == 0) {
            val exists = c.createStatement().use { s -> s.executeQuery("SELECT 1 FROM groups WHERE id = $groupId").use { it.next() } }
            throw if (exists) {
                DomainException.InvalidState("Wishlists can be changed while the group is pending or active.")
            } else {
                DomainException.NotFound("Group not found.")
            }
        }
        val participates = c.prepareStatement("SELECT 1 FROM group_participants WHERE group_id = ? AND username = ?").use { s ->
            s.setLong(1, groupId)
            s.setString(2, owner.value)
            s.executeQuery().use { it.next() }
        }
        if (!participates) throw DomainException.NotFound("Group not found.")
    }

    /** Replaces all the links of an item; deleting first keeps `UNIQUE (item_id, position)` from clashing. */
    private fun writeLinks(c: Connection, itemId: Long, urls: List<String>) {
        c.prepareStatement("DELETE FROM wishlist_item_links WHERE item_id = ?").use { s ->
            s.setLong(1, itemId)
            s.executeUpdate()
        }
        c.prepareStatement("INSERT INTO wishlist_item_links (item_id, position, url) VALUES (?, ?, ?)").use { s ->
            urls.forEachIndexed { index, url ->
                s.setLong(1, itemId)
                s.setInt(2, index)
                s.setString(3, url)
                s.executeUpdate()
            }
        }
    }

    private fun listOn(c: Connection, groupId: Long, owner: Username): List<WishlistItem> {
        val links = c.prepareStatement(
            "SELECT l.item_id, l.url FROM wishlist_item_links l JOIN wishlist_items i ON i.id = l.item_id " +
                "WHERE i.group_id = ? AND i.username = ? ORDER BY l.item_id, l.position",
        ).use { s ->
            s.setLong(1, groupId)
            s.setString(2, owner.value)
            s.executeQuery().use { rows ->
                generateSequence { if (rows.next()) rows.getLong(1) to rows.getString(2) else null }
                    .groupBy({ it.first }, { it.second })
            }
        }
        return c.prepareStatement(
            "SELECT id, title, note, created_at, updated_at FROM wishlist_items " +
                "WHERE group_id = ? AND username = ? ORDER BY position",
        ).use { s ->
            s.setLong(1, groupId)
            s.setString(2, owner.value)
            s.executeQuery().use { rows -> generateSequence { if (rows.next()) toItem(rows, links) else null }.toList() }
        }
    }

    private fun toItem(rows: ResultSet, links: Map<Long, List<String>>): WishlistItem {
        val id = rows.getLong("id")
        return WishlistItem(
            id = id,
            title = rows.getString("title"),
            note = rows.getString("note"),
            urls = links[id].orEmpty(),
            createdAtMillis = rows.getLong("created_at"),
            updatedAtMillis = rows.getLong("updated_at"),
        )
    }
}
