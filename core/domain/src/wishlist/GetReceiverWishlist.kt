package com.usuallycoding.opensecretsanta.domain.wishlist

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User

/**
 * A Santa reads the wishlist their receiver keeps for this group. It answers for the caller's own
 * receiver only, returns items without ids, and records nothing about the read.
 */
class GetReceiverWishlist(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val groups: GroupRepository,
    private val wishlists: WishlistRepository,
) {
    operator fun invoke(user: User, groupId: Long): List<SharedWishlistItem> {
        val group = loadParticipantGroup(user, groupId)
        if (group.state != GroupState.ACTIVE && group.state != GroupState.ARCHIVED) {
            throw DomainException.InvalidState("Receivers are known once the group is active.")
        }
        val receiver = groups.receiverOf(group.id, user.username) ?: throw DomainException.NotFound("Group not found.")
        return wishlists.list(group.id, receiver).map {
            SharedWishlistItem(it.title, it.note, it.url, it.createdAtMillis, it.updatedAtMillis)
        }
    }
}
