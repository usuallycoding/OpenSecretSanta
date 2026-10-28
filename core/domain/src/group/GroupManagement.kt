package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.admin.requireAdmin
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User

class CreateGroup(
    private val groups: GroupRepository,
    private val viewer: GroupViewer,
) {
    operator fun invoke(actor: User, details: GroupDetails, rawParticipants: List<String>): GroupView {
        requireAdmin(actor)
        return viewer(groups.create(actor.username, details, parseParticipants(rawParticipants)))
    }
}

class UpdateGroupDetails(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
    private val viewer: GroupViewer,
) {
    operator fun invoke(actor: User, id: Long, details: GroupDetails): GroupView {
        val group = loadOwned(actor, id)
        if (!group.state.allowsDetailChanges) {
            throw DomainException.InvalidState("An archived group can no longer be edited.")
        }
        return viewer(groups.updateDetails(group.id, details))
    }
}

/**
 * Deletes a group in any state. A group that has a draw, wishlist items, questions or answers is only deleted when the
 * caller has [confirmed] it, after being told (through [DomainException.ConfirmationRequired]) what would go.
 */
class DeleteGroup(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
) {
    operator fun invoke(actor: User, id: Long, confirmed: Boolean = false) {
        val group = loadOwned(actor, id)
        if (!confirmed) {
            val impact = groups.impact(group.id)
            if (!impact.isEmpty) throw DomainException.ConfirmationRequired(impact)
        }
        groups.delete(group.id)
    }
}

class ListOwnedGroups(
    private val groups: GroupRepository,
    private val viewer: GroupViewer,
) {
    operator fun invoke(actor: User): List<GroupView> {
        requireAdmin(actor)
        return groups.findOwnedBy(actor.username).map(viewer::invoke)
    }
}

class GetOwnedGroup(
    private val loadOwned: OwnedGroupLoader,
    private val viewer: GroupViewer,
) {
    operator fun invoke(actor: User, id: Long): GroupView = viewer(loadOwned(actor, id))
}
