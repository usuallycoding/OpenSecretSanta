package com.usuallycoding.opensecretsanta.domain.wishlist

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * The group owner reads one participant's wishlist for the group. Read-only, without item ids, and
 * it records nothing, so the participant cannot tell it happened.
 */
class GetParticipantWishlist(
    private val loadOwned: OwnedGroupLoader,
    private val wishlists: WishlistRepository,
) {
    operator fun invoke(actor: User, groupId: Long, rawUsername: String): List<SharedWishlistItem> {
        val group = loadOwned(actor, groupId)
        if (group.state == GroupState.DRAFT) {
            throw DomainException.InvalidState("Wishlists exist once the group has left Draft.")
        }
        val participant = Username.parseOrNull(rawUsername)?.takeIf { it in group.participants }
            ?: throw DomainException.NotFound("Participant not found.")
        return wishlists.list(group.id, participant).map {
            SharedWishlistItem(it.title, it.note, it.url, it.createdAtMillis, it.updatedAtMillis)
        }
    }
}
