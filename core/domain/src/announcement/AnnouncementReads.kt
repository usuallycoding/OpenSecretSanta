package com.usuallycoding.opensecretsanta.domain.announcement

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.Username

/** How many of a group's announcements a participant has not seen, and the newest time they marked (null when they never did). */
data class AnnouncementReadMark(val unread: Int, val seenUpToMillis: Long?)

/** Storage for the newest announcement time each participant has seen, per group; it goes with the group. */
interface AnnouncementReadRepository {
    fun seenUpTo(groupId: Long, username: Username): Long?

    /** Keeps the later of the stored time and [atMillis], and returns the time that is stored afterwards. */
    fun markSeen(groupId: Long, username: Username, atMillis: Long): Long

    /**
     * For each of [groupIds], the number of announcements sent after the time [username] has seen, or all of them when the person
     * never marked any. A group with none is in the map with 0.
     */
    fun unreadCounts(username: Username, groupIds: Collection<Long>): Map<Long, Int>
}

private fun AnnouncementReadRepository.markOf(groupId: Long, username: Username) =
    AnnouncementReadMark(unreadCounts(username, listOf(groupId))[groupId] ?: 0, seenUpTo(groupId, username))

/** A participant reads what they have seen of a group's announcements. Only their own mark comes back. */
class GetMyAnnouncementReads(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val reads: AnnouncementReadRepository,
) {
    operator fun invoke(user: User, groupId: Long): AnnouncementReadMark {
        val group = loadParticipantGroup(user, groupId)
        return reads.markOf(group.id, user.username)
    }
}

/**
 * The participant says they have seen the group's announcements up to a time. The later of that and what is stored is kept, and never
 * a time later than now, so a mark can neither go back nor hide what has not been sent yet. Works in an Archived group too: it is the
 * person's own state, not the group's.
 */
class MarkAnnouncementsRead(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val reads: AnnouncementReadRepository,
    private val clock: Clock,
) {
    operator fun invoke(user: User, groupId: Long, seenUpToMillis: Long): AnnouncementReadMark {
        val group = loadParticipantGroup(user, groupId)
        reads.markSeen(group.id, user.username, minOf(seenUpToMillis, clock.nowMillis()))
        return reads.markOf(group.id, user.username)
    }
}
