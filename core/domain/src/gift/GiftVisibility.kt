package com.usuallycoding.opensecretsanta.domain.gift

import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfoRepository
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User

/**
 * A participant's own receiver, with the details a Santa needs to post a gift. Values are read at
 * the time of the request, so changes the receiver makes to their delivery info for the group are seen straight away.
 */
class GetMyReceiver(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val groups: GroupRepository,
    private val deliveryInfos: DeliveryInfoRepository,
) {
    operator fun invoke(user: User, groupId: Long): ReceiverDetails {
        val group = loadParticipantGroup(user, groupId)
        if (group.state != GroupState.ACTIVE && group.state != GroupState.ARCHIVED) {
            throw DomainException.InvalidState("Receivers are known once the group is active.")
        }
        val receiver = groups.receiverOf(group.id, user.username) ?: throw DomainException.NotFound("Group not found.")
        val info = deliveryInfos.find(group.id, receiver)
        return ReceiverDetails(
            username = receiver,
            fullName = info.fullName,
            address = info.address,
            phone = info.phone,
            deliveryInfo = info.deliveryInfo,
            email = info.email,
        )
    }
}

/**
 * Who gave the participant their gift: shown only once the group is Archived, and then only the
 * username and full name. Before that the refusal is a fixed message that names nobody.
 */
class GetMySanta(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val groups: GroupRepository,
    private val deliveryInfos: DeliveryInfoRepository,
) {
    operator fun invoke(user: User, groupId: Long): SantaReveal {
        val group = loadParticipantGroup(user, groupId)
        if (group.state != GroupState.ARCHIVED) {
            throw DomainException.InvalidState("Your Santa is revealed once the group is archived.")
        }
        val santa = groups.santaOf(group.id, user.username) ?: throw DomainException.NotFound("Group not found.")
        return SantaReveal(username = santa, fullName = deliveryInfos.find(group.id, santa).fullName)
    }
}
