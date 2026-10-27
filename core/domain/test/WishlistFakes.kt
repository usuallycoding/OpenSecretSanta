package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.domain.wishlist.SharedWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistEntry
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistRepository

/** In-memory wishlists that apply the same rules as the real repository. */
class InMemoryWishlists(private val groups: InMemoryGroups) : WishlistRepository {
    private data class Row(
        val id: Long,
        val groupId: Long,
        val owner: Username,
        var title: String,
        var note: String?,
        var url: String?,
        val createdAt: Long,
        var updatedAt: Long,
    )

    /** Rows in insertion order; list order is the order of this list for one (group, owner). */
    private val rows = mutableListOf<Row>()
    private var nextId = 1L

    private fun requireWritable(groupId: Long, owner: Username) {
        val group = groups.find(groupId) ?: throw DomainException.NotFound("Group not found.")
        if (group.state != GroupState.PENDING && group.state != GroupState.ACTIVE) {
            throw DomainException.InvalidState("Wishlists can be changed while the group is pending or active.")
        }
        if (owner !in group.participants) throw DomainException.NotFound("Group not found.")
    }

    private fun mine(groupId: Long, owner: Username) = rows.filter { it.groupId == groupId && it.owner == owner }

    private fun Row.toItem() = WishlistItem(id, title, note, url, createdAt, updatedAt)

    override fun list(groupId: Long, owner: Username) = mine(groupId, owner).map { it.toItem() }

    override fun add(groupId: Long, owner: Username, entry: WishlistEntry, atMillis: Long, maxItems: Int): WishlistItem {
        requireWritable(groupId, owner)
        if (mine(groupId, owner).size >= maxItems) throw DomainException.InvalidState("The wishlist is full.")
        val row = Row(nextId++, groupId, owner, entry.title, entry.note, entry.url, atMillis, atMillis)
        rows += row
        return row.toItem()
    }

    override fun update(groupId: Long, owner: Username, itemId: Long, entry: WishlistEntry, atMillis: Long): WishlistItem {
        requireWritable(groupId, owner)
        val row = mine(groupId, owner).firstOrNull { it.id == itemId } ?: throw DomainException.NotFound("Item not found.")
        row.title = entry.title
        row.note = entry.note
        row.url = entry.url
        row.updatedAt = atMillis
        return row.toItem()
    }

    override fun delete(groupId: Long, owner: Username, itemId: Long) {
        requireWritable(groupId, owner)
        val row = mine(groupId, owner).firstOrNull { it.id == itemId } ?: throw DomainException.NotFound("Item not found.")
        rows.remove(row)
    }

    override fun reorder(groupId: Long, owner: Username, orderedIds: List<Long>): List<WishlistItem> {
        requireWritable(groupId, owner)
        val current = mine(groupId, owner)
        if (orderedIds.size != current.size || orderedIds.toSet() != current.map { it.id }.toSet()) {
            throw DomainException.ValidationFailed("itemIds", "The ids must be exactly your current items, each once.")
        }
        val byId = current.associateBy { it.id }
        rows.removeAll(current.toSet())
        rows.addAll(orderedIds.map { byId.getValue(it) })
        return list(groupId, owner)
    }

    /** What a Santa sees of [owner]'s list: no ids. */
    fun shared(groupId: Long, owner: Username) =
        mine(groupId, owner).map { SharedWishlistItem(it.title, it.note, it.url, it.createdAt, it.updatedAt) }
}
