package com.usuallycoding.opensecretsanta.domain.wishlist

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * Storage for the wishlists, one per participant per group. Writes apply in one transaction and
 * state what they expect: the group must be Pending or Active (otherwise `InvalidState`), the owner
 * must still participate (otherwise `NotFound`), and an item id must belong to that owner's list in
 * that group (otherwise `NotFound`).
 */
interface WishlistRepository {
    fun list(groupId: Long, owner: Username): List<WishlistItem>

    /** Appends to the end of the list; `InvalidState` when [maxItems] already exist. */
    fun add(groupId: Long, owner: Username, entry: WishlistEntry, atMillis: Long, maxItems: Int): WishlistItem

    fun update(groupId: Long, owner: Username, itemId: Long, entry: WishlistEntry, atMillis: Long): WishlistItem

    fun delete(groupId: Long, owner: Username, itemId: Long)

    /**
     * Applies [orderedIds] as the new order in one step, but only if they are exactly the owner's
     * current items, each once; otherwise `ValidationFailed` naming `itemIds` and nothing changes.
     */
    fun reorder(groupId: Long, owner: Username, orderedIds: List<Long>): List<WishlistItem>
}

private fun Group.requireEditableWishlist() {
    if (state != GroupState.PENDING && state != GroupState.ACTIVE) {
        throw DomainException.InvalidState("Wishlists can be changed while the group is pending or active.")
    }
}

/** A participant's own list for one group; readable from the moment the group leaves Draft, even once Archived. */
class GetMyWishlist(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val wishlists: WishlistRepository,
) {
    operator fun invoke(user: User, groupId: Long): List<WishlistItem> {
        val group = loadParticipantGroup(user, groupId)
        return wishlists.list(group.id, user.username)
    }
}

class AddWishlistItem(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val wishlists: WishlistRepository,
    private val clock: Clock,
) {
    operator fun invoke(user: User, groupId: Long, title: String?, note: String?, url: String?): WishlistItem {
        val group = loadParticipantGroup(user, groupId)
        group.requireEditableWishlist()
        val entry = WishlistEntry.of(title, note, url)
        return wishlists.add(group.id, user.username, entry, clock.nowMillis(), WishlistLimits.MAX_ITEMS)
    }
}

class UpdateWishlistItem(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val wishlists: WishlistRepository,
    private val clock: Clock,
) {
    operator fun invoke(user: User, groupId: Long, itemId: Long, title: String?, note: String?, url: String?): WishlistItem {
        val group = loadParticipantGroup(user, groupId)
        group.requireEditableWishlist()
        val entry = WishlistEntry.of(title, note, url)
        return wishlists.update(group.id, user.username, itemId, entry, clock.nowMillis())
    }
}

class DeleteWishlistItem(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val wishlists: WishlistRepository,
) {
    operator fun invoke(user: User, groupId: Long, itemId: Long) {
        val group = loadParticipantGroup(user, groupId)
        group.requireEditableWishlist()
        wishlists.delete(group.id, user.username, itemId)
    }
}

class ReorderWishlist(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val wishlists: WishlistRepository,
) {
    operator fun invoke(user: User, groupId: Long, itemIds: List<Long>): List<WishlistItem> {
        val group = loadParticipantGroup(user, groupId)
        group.requireEditableWishlist()
        return wishlists.reorder(group.id, user.username, itemIds)
    }
}
