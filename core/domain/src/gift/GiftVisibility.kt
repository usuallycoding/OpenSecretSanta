package com.usuallycoding.opensecretsanta.domain.gift

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository

/**
 * A participant's own receiver, with the details a Santa needs to post a gift. Values are read at
 * the time of the request, so changes the receiver makes to their profile are seen straight away.
 */
class GetMyReceiver(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val groups: GroupRepository,
    private val users: UserRepository,
) {
    operator fun invoke(user: User, groupId: Long): ReceiverDetails {
        val group = loadParticipantGroup(user, groupId)
        if (group.state != GroupState.ACTIVE && group.state != GroupState.ARCHIVED) {
            throw DomainException.InvalidState("Receivers are known once the group is active.")
        }
        val receiver = groups.receiverOf(group.id, user.username) ?: throw DomainException.NotFound("Group not found.")
        val account = users.find(receiver) ?: error("Receiver $receiver of group ${group.id} has no account")
        return ReceiverDetails(
            username = account.username,
            fullName = account.profile.fullName,
            address = account.profile.address,
            phone = account.profile.phone,
            deliveryInfo = account.profile.deliveryInfo,
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
    private val users: UserRepository,
) {
    operator fun invoke(user: User, groupId: Long): SantaReveal {
        val group = loadParticipantGroup(user, groupId)
        if (group.state != GroupState.ARCHIVED) {
            throw DomainException.InvalidState("Your Santa is revealed once the group is archived.")
        }
        val santa = groups.santaOf(group.id, user.username) ?: throw DomainException.NotFound("Group not found.")
        val account = users.find(santa) ?: error("Santa $santa of group ${group.id} has no account")
        return SantaReveal(username = account.username, fullName = account.profile.fullName)
    }
}
