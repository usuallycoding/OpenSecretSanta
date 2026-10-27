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

class DeleteDraftGroup(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
) {
    operator fun invoke(actor: User, id: Long) {
        val group = loadOwned(actor, id)
        if (group.state != GroupState.DRAFT) {
            throw DomainException.InvalidState("Only a group in Draft can be deleted.")
        }
        groups.deleteDraft(group.id)
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
