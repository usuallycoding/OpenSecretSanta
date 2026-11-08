package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.announcement.AnnouncementReadRepository
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database

/**
 * The newest announcement time each participant has seen, one row per group and person. The row hangs off the group and goes
 * with it; it has no key to the participants, so a person removed from a Pending group keeps their mark.
 */
class SqliteAnnouncementReadRepository(private val database: Database) : AnnouncementReadRepository {

    override fun seenUpTo(groupId: Long, username: Username): Long? = database.connection().use { c ->
        c.prepareStatement("SELECT seen_up_to FROM announcement_reads WHERE group_id = ? AND username = ?").use { s ->
            s.setLong(1, groupId)
            s.setString(2, username.value)
            s.executeQuery().use { if (it.next()) it.getLong(1) else null }
        }
    }

    override fun markSeen(groupId: Long, username: Username, atMillis: Long): Long = database.transaction { c ->
        c.prepareStatement(
            "INSERT INTO announcement_reads (group_id, username, seen_up_to) VALUES (?, ?, ?) " +
                "ON CONFLICT (group_id, username) DO UPDATE SET seen_up_to = MAX(seen_up_to, excluded.seen_up_to)",
        ).use { s ->
            s.setLong(1, groupId)
            s.setString(2, username.value)
            s.setLong(3, atMillis)
            s.executeUpdate()
        }
        c.prepareStatement("SELECT seen_up_to FROM announcement_reads WHERE group_id = ? AND username = ?").use { s ->
            s.setLong(1, groupId)
            s.setString(2, username.value)
            s.executeQuery().use { it.next(); it.getLong(1) }
        }
    }

    override fun unreadCounts(username: Username, groupIds: Collection<Long>): Map<Long, Int> {
        if (groupIds.isEmpty()) return emptyMap()
        val counts = groupIds.associateWithTo(HashMap<Long, Int>()) { 0 }
        database.connection().use { c ->
            val marks = groupIds.joinToString(",") { "?" }
            c.prepareStatement(
                "SELECT a.group_id, COUNT(*) FROM announcements a WHERE a.group_id IN ($marks) AND a.sent_at > " +
                    "COALESCE((SELECT r.seen_up_to FROM announcement_reads r WHERE r.group_id = a.group_id AND r.username = ?), ?) " +
                    "GROUP BY a.group_id",
            ).use { s ->
                var index = 1
                for (id in groupIds) s.setLong(index++, id)
                s.setString(index++, username.value)
                s.setLong(index, Long.MIN_VALUE)
                s.executeQuery().use { rows -> while (rows.next()) counts[rows.getLong(1)] = rows.getInt(2) }
            }
        }
        return counts
    }
}
