package com.usuallycoding.opensecretsanta.domain.delivery

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * Storage for delivery info, one per participant per group. A person who has saved nothing for a group has no row, which
 * reads as empty details; the rows go with the group.
 */
interface DeliveryInfoRepository {
    /** What [username] saved for the group, or empty details when nothing was saved. */
    fun find(groupId: Long, username: Username): DeliveryInfo

    /** The saved details of [usernames] in the group; a person who saved nothing is not in the map. */
    fun findAll(groupId: Long, usernames: Collection<Username>): Map<Username, DeliveryInfo>

    /** Replaces everything [username] saved for the group. */
    fun save(groupId: Long, username: Username, info: DeliveryInfo)
}

/** A participant's own delivery info for one group; readable from the moment the group leaves Draft, even once Archived. */
class GetMyDeliveryInfo(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val deliveryInfos: DeliveryInfoRepository,
) {
    operator fun invoke(user: User, groupId: Long): DeliveryInfo {
        val group = loadParticipantGroup(user, groupId)
        return deliveryInfos.find(group.id, user.username)
    }
}

/** Replaces a participant's own delivery info for a group that is Pending or Active. */
class UpdateMyDeliveryInfo(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val deliveryInfos: DeliveryInfoRepository,
) {
    operator fun invoke(user: User, groupId: Long, info: DeliveryInfo): DeliveryInfo {
        val group = loadParticipantGroup(user, groupId)
        if (group.state != GroupState.PENDING && group.state != GroupState.ACTIVE) {
            throw DomainException.InvalidState("Delivery info can be changed while the group is pending or active.")
        }
        deliveryInfos.save(group.id, user.username, info)
        return info
    }
}

/**
 * The group owner reads one participant's delivery info for the group: whatever the person saved, or empty details. Read-only,
 * and it records nothing, so the participant cannot tell it happened.
 */
class GetParticipantDeliveryInfo(
    private val loadOwned: OwnedGroupLoader,
    private val deliveryInfos: DeliveryInfoRepository,
) {
    operator fun invoke(actor: User, groupId: Long, rawUsername: String): DeliveryInfo {
        val group = loadOwned(actor, groupId)
        if (group.state == GroupState.DRAFT) {
            throw DomainException.InvalidState("Delivery info exists once the group has left Draft.")
        }
        val participant = Username.parseOrNull(rawUsername)?.takeIf { it in group.participants }
            ?: throw DomainException.NotFound("Participant not found.")
        return deliveryInfos.find(group.id, participant)
    }
}
