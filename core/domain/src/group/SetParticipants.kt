package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.admin.NewAccountPlanner
import com.usuallycoding.opensecretsanta.domain.admin.TemporaryCredentials
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository

/** The outcome of a step that may create accounts: the group, and the credentials of any new ones. */
class GroupChange(val view: GroupView, val createdAccounts: List<TemporaryCredentials>)

/**
 * Replaces a group's participant list. In Draft the list is just names; in Pending every name
 * without an account gets one straight away, while removed participants keep theirs.
 */
class SetParticipants(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
    private val users: UserRepository,
    private val planner: NewAccountPlanner,
    private val viewer: GroupViewer,
) {
    operator fun invoke(actor: User, id: Long, rawParticipants: List<String>): GroupChange {
        val group = loadOwned(actor, id)
        if (!group.state.allowsParticipantChanges) {
            throw DomainException.InvalidState("The participants of an Active or Archived group cannot change.")
        }
        val participants = parseParticipants(rawParticipants)

        val planned = if (group.state == GroupState.PENDING) {
            planner.plan(participants, existing = users.existing(participants))
        } else {
            emptyList()
        }
        val updated = groups.replaceParticipants(group.id, participants, planned.map { it.newUser })
        return GroupChange(viewer(updated), planned.map { TemporaryCredentials(it.username, it.temporaryPassword) })
    }
}
