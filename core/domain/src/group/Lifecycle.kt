package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.announcement.AnnouncementReadRepository
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

        val planned = planner.plan(group.participants, existing = users.existing(group.participants), createdBy = actor.username)
        val updated = groups.moveToPending(group.id, planned.map { it.newUser })
        return GroupChange(viewer(updated), planned.map { TemporaryCredentials(it.username, it.temporaryPassword) })
    }
}

/**
 * Pending to Active: needs [MIN_PARTICIPANTS] participants (with fewer, a participant could work out
 * who their Santa is) and draws the assignments, honouring the group's draw rules, in the same step. Incomplete profiles never stop it;
 * they are only flagged in the returned view.
 */
class ActivateGroup(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
    private val assignmentPlanner: AssignmentPlanner,
    private val rules: DrawRuleRepository,
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
        // planned before anything is stored: rules no draw can satisfy leave the group Pending
        return viewer(groups.activate(group.id, assignmentPlanner.plan(group.participants, rules.list(group.id))))
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

/** A group as one of its participants sees it, with how many of its announcements that person has not seen. */
class MemberGroup(val group: Group, val unreadAnnouncements: Int)

/** What a participant may see: non-Draft groups they belong to, never anything about assignments, and what is new to them in each. */
class ListMyGroups(private val groups: GroupRepository, private val reads: AnnouncementReadRepository) {
    operator fun invoke(user: User): List<MemberGroup> {
        val visible = groups.findVisibleTo(user.username)
        val unread = reads.unreadCounts(user.username, visible.map { it.id })
        return visible.map { MemberGroup(it, unread[it.id] ?: 0) }
    }
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

class GetMyGroup(groups: GroupRepository, private val reads: AnnouncementReadRepository) {
    private val load = ParticipantGroupLoader(groups)

    operator fun invoke(user: User, id: Long): MemberGroup {
        val group = load(user, id)
        return MemberGroup(group, reads.unreadCounts(user.username, listOf(group.id))[group.id] ?: 0)
    }
}
