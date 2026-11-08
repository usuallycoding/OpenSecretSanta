package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.announcement.Announcement
import com.usuallycoding.opensecretsanta.domain.announcement.AnnouncementReadRepository
import com.usuallycoding.opensecretsanta.domain.announcement.AnnouncementRepository
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * In-memory announcements that apply the same rules as the real repository (Pending or Active only, numbering per group, cap), and the
 * marks of what each person has seen of them.
 */
class InMemoryAnnouncements(private val groups: InMemoryGroups) : AnnouncementRepository, AnnouncementReadRepository {
    private val rows = mutableMapOf<Long, MutableList<Announcement>>()
    private val marks = mutableMapOf<Pair<Long, Username>, Long>()

    override fun add(groupId: Long, title: String, body: String, atMillis: Long, max: Int): Announcement {
        val group = groups.find(groupId) ?: throw DomainException.NotFound("Group not found.")
        if (group.state != GroupState.PENDING && group.state != GroupState.ACTIVE) {
            throw DomainException.InvalidState("Announcements can only be sent to a pending or active group.")
        }
        val kept = rows.getOrPut(groupId) { mutableListOf() }
        if (kept.size >= max) throw DomainException.InvalidState("The announcement limit has been reached.")
        return Announcement(kept.size + 1, title, body, atMillis).also { kept += it }
    }

    override fun list(groupId: Long): List<Announcement> = rows[groupId].orEmpty().sortedBy { it.number }

    /** Puts [count] announcements in the group without going through the rules, as a group that has been announced to for a while. */
    fun seed(groupId: Long, count: Int) {
        val kept = rows.getOrPut(groupId) { mutableListOf() }
        repeat(count) { kept += Announcement(kept.size + 1, "Title ${kept.size + 1}", "Message", 1_000L + kept.size) }
    }

    override fun seenUpTo(groupId: Long, username: Username): Long? = marks[groupId to username]

    override fun markSeen(groupId: Long, username: Username, atMillis: Long): Long =
        maxOf(marks[groupId to username] ?: Long.MIN_VALUE, atMillis).also { marks[groupId to username] = it }

    override fun unreadCounts(username: Username, groupIds: Collection<Long>): Map<Long, Int> = groupIds.associateWith { id ->
        val seen = marks[id to username] ?: Long.MIN_VALUE
        rows[id].orEmpty().count { it.sentAtMillis > seen }
    }
}
