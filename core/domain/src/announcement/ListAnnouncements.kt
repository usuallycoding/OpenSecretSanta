package com.usuallycoding.opensecretsanta.domain.announcement

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User

/**
 * The announcements kept for a group, in the order they were sent, for a participant of a Pending, Active or Archived group, or
 * for its owner, who need not take part in it. They are the same for everyone, and nothing is recorded about a read.
 */
class ListAnnouncements(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val loadOwned: OwnedGroupLoader,
    private val announcements: AnnouncementRepository,
) {
    /** A Draft group, a stranger and a missing group all look the same to a participant: not found. */
    fun forParticipant(user: User, groupId: Long): List<Announcement> = announcements.list(loadParticipantGroup(user, groupId).id)

    /** Anyone but the owner gets "not found"; a Draft group has none yet. */
    fun forOwner(actor: User, groupId: Long): List<Announcement> {
        val group = loadOwned(actor, groupId)
        if (group.state == GroupState.DRAFT) throw DomainException.InvalidState("A draft group has no announcements.")
        return announcements.list(group.id)
    }
}
