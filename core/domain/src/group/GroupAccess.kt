package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.admin.requireAdmin
import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfoRepository
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

/** One participant as the owner sees them. */
class ParticipantStatus(
    val username: Username,
    val hasAccount: Boolean,
    val profileComplete: Boolean,
    val fullName: String? = null,
    /** True when the account exists and still has a temporary password; false with no account. Only a flag: the password is not here. */
    val mustChangePassword: Boolean = false,
)

/** A group together with what the owner needs to know about each participant. */
class GroupView(val group: Group, val participants: List<ParticipantStatus>)

/** Loads a group for its owner; anyone else, and missing groups, get the same "not found". */
class OwnedGroupLoader(private val groups: GroupRepository) {
    operator fun invoke(actor: User, id: Long): Group {
        requireAdmin(actor)
        val group = groups.find(id)
        if (group == null || group.owner != actor.username) throw DomainException.NotFound("Group not found.")
        return group
    }
}

class GroupViewer(private val users: UserRepository, private val deliveryInfos: DeliveryInfoRepository) {
    operator fun invoke(group: Group): GroupView {
        val accounts = users.findAll(group.participants).associateBy { it.username }
        val infos = deliveryInfos.findAll(group.id, group.participants)
        return GroupView(
            group = group,
            participants = group.participants.map { username ->
                val account = accounts[username]
                val info = infos[username]
                ParticipantStatus(
                    username,
                    hasAccount = account != null,
                    profileComplete = info?.isComplete == true,
                    fullName = info?.fullName,
                    mustChangePassword = account?.mustChangePassword == true,
                )
            },
        )
    }
}

/** Validates a raw participant list: valid usernames, duplicates (ignoring case) kept once, order preserved. */
fun parseParticipants(raw: List<String>): List<Username> =
    raw.map { Username.parse(it, field = "participants") }.distinct()
