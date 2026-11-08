package com.usuallycoding.opensecretsanta.domain.announcement

object AnnouncementLimits {
    const val MAX_PER_GROUP = 200
}

/** An announcement the owner sent to a group, as it is kept: numbered from 1 within the group. */
data class Announcement(
    val number: Int,
    val title: String,
    val body: String,
    val sentAtMillis: Long,
)

/**
 * Storage for announcements. An add applies in one transaction, throwing `InvalidState` when the group is not Pending or Active
 * or the limit is reached, and `NotFound` when the group does not exist.
 */
interface AnnouncementRepository {
    /** Adds the next numbered announcement to the group's; fails when [max] already exist. */
    fun add(groupId: Long, title: String, body: String, atMillis: Long, max: Int): Announcement

    /** The group's announcements, in the order sent. */
    fun list(groupId: Long): List<Announcement>
}
