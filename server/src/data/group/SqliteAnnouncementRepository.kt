package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.announcement.Announcement
import com.usuallycoding.opensecretsanta.domain.announcement.AnnouncementRepository
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.Connection

/**
 * Like the letters: every write first runs `UPDATE groups SET state = state WHERE id = ? AND state IN ('pending', 'active')`, which
 * checks the group is Pending or Active and takes SQLite's write lock, so the numbering, the cap and the insert cannot interleave
 * with another writer.
 */
class SqliteAnnouncementRepository(private val database: Database) : AnnouncementRepository {

    override fun add(groupId: Long, title: String, body: String, atMillis: Long, max: Int): Announcement =
        database.transaction { c ->
            requireOpen(c, groupId)
            val (highest, count) = c.prepareStatement("SELECT COALESCE(MAX(number), 0), COUNT(*) FROM announcements WHERE group_id = ?").use { s ->
                s.setLong(1, groupId)
                s.executeQuery().use { it.next(); it.getInt(1) to it.getInt(2) }
            }
            if (count >= max) throw DomainException.InvalidState("The announcement limit has been reached.")

            val number = highest + 1
            c.prepareStatement("INSERT INTO announcements (group_id, number, title, body, sent_at) VALUES (?, ?, ?, ?, ?)").use { s ->
                s.setLong(1, groupId)
                s.setInt(2, number)
                s.setString(3, title)
                s.setString(4, body)
                s.setLong(5, atMillis)
                s.executeUpdate()
            }
            Announcement(number, title, body, atMillis)
        }

    override fun list(groupId: Long): List<Announcement> = database.connection().use { c ->
        c.prepareStatement("SELECT number, title, body, sent_at FROM announcements WHERE group_id = ? ORDER BY number").use { s ->
            s.setLong(1, groupId)
            s.executeQuery().use { rows ->
                generateSequence {
                    if (rows.next()) Announcement(rows.getInt("number"), rows.getString("title"), rows.getString("body"), rows.getLong("sent_at")) else null
                }.toList()
            }
        }
    }

    private fun requireOpen(c: Connection, groupId: Long) {
        val changed = c.createStatement().use { it.executeUpdate("UPDATE groups SET state = state WHERE id = $groupId AND state IN ('pending', 'active')") }
        if (changed == 0) {
            val exists = c.createStatement().use { s -> s.executeQuery("SELECT 1 FROM groups WHERE id = $groupId").use { it.next() } }
            throw if (exists) {
                DomainException.InvalidState("Announcements can only be sent to a pending or active group.")
            } else {
                DomainException.NotFound("Group not found.")
            }
        }
    }
}
