package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.admin.NewAccountPlanner
import com.usuallycoding.opensecretsanta.domain.admin.TemporaryCredentials
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository

private fun Group.requireCanMoveTo(target: GroupState) {
    if (!state.canMoveTo(target)) {
        throw DomainException.InvalidState("A group in ${state.storedName} cannot move to ${target.storedName}.")
    }
}

/** Draft to Pending: every participant without an account gets one, in the same step as the state change. */
class MoveToPending(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
    private val users: UserRepository,
    private val planner: NewAccountPlanner,
    private val viewer: GroupViewer,
) {
    operator fun invoke(actor: User, id: Long): GroupChange {
        val group = loadOwned(actor, id)
        group.requireCanMoveTo(GroupState.PENDING)

        val planned = planner.plan(group.participants, existing = users.existing(group.participants))
        val updated = groups.moveToPending(group.id, planned.map { it.newUser })
        return GroupChange(viewer(updated), planned.map { TemporaryCredentials(it.username, it.temporaryPassword) })
    }
}

/**
 * Pending to Active: needs [MIN_PARTICIPANTS] participants (with fewer, a participant could work out
 * who their Santa is) and draws the assignments in the same step. Incomplete profiles never stop it;
 * they are only flagged in the returned view.
 */
class ActivateGroup(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
    private val assignmentPlanner: AssignmentPlanner,
    private val viewer: GroupViewer,
) {
    operator fun invoke(actor: User, id: Long): GroupView {
        val group = loadOwned(actor, id)
        group.requireCanMoveTo(GroupState.ACTIVE)
        if (group.participants.size < MIN_PARTICIPANTS) {
            throw DomainException.ValidationFailed(
                "participants",
                "At least $MIN_PARTICIPANTS participants are needed to activate a group.",
            )
        }
        return viewer(groups.activate(group.id, assignmentPlanner.plan(group.participants)))
    }

    companion object {
        const val MIN_PARTICIPANTS = 4
    }
}

/** Active to Archived: participants and assignments stay as they are. */
class ArchiveGroup(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
    private val viewer: GroupViewer,
) {
    operator fun invoke(actor: User, id: Long): GroupView {
        val group = loadOwned(actor, id)
        group.requireCanMoveTo(GroupState.ARCHIVED)
        return viewer(groups.archive(group.id))
    }
}

/** The owner's list of every Santa and receiver pair; exists only once the group is Active. */
class ListAssignments(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
) {
    operator fun invoke(actor: User, id: Long): List<Assignment> {
        val group = loadOwned(actor, id)
        if (group.state != GroupState.ACTIVE && group.state != GroupState.ARCHIVED) {
            throw DomainException.InvalidState("Assignments exist only once a group is Active.")
        }
        return groups.assignments(group.id)
    }
}

/** What a participant may see: non-Draft groups they belong to, never anything about assignments. */
class ListMyGroups(private val groups: GroupRepository) {
    operator fun invoke(user: User): List<Group> = groups.findVisibleTo(user.username)
}

/** Loads a group for one of its participants; Drafts, strangers and missing groups all look the same. */
class ParticipantGroupLoader(private val groups: GroupRepository) {
    operator fun invoke(user: User, id: Long): Group {
        val group = groups.find(id)
        if (group == null || group.state == GroupState.DRAFT || user.username !in group.participants) {
            throw DomainException.NotFound("Group not found.")
        }
        return group
    }
}

class GetMyGroup(groups: GroupRepository) {
    private val load = ParticipantGroupLoader(groups)

    operator fun invoke(user: User, id: Long): Group = load(user, id)
}
