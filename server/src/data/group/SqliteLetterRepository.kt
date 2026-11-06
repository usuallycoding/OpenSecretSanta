package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.Letter
import com.usuallycoding.opensecretsanta.domain.gift.LetterRepository
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.Connection
import java.sql.ResultSet

/**
 * Like the questions: every write first runs `UPDATE groups SET state = state WHERE id = ? AND state = 'active'`, which checks
 * the group is Active and takes SQLite's write lock, so the numbering, the cap and the insert cannot interleave with another writer.
 */
class SqliteLetterRepository(private val database: Database) : LetterRepository {

    override fun send(groupId: Long, receiver: Username, title: String, text: String, atMillis: Long, maxLetters: Int): Letter =
        database.transaction { c ->
            requireActive(c, groupId)
            val hasAssignment = c.prepareStatement("SELECT 1 FROM assignments WHERE group_id = ? AND receiver = ?").use { s ->
                s.setLong(1, groupId)
                s.setString(2, receiver.value)
                s.executeQuery().use { it.next() }
            }
            if (!hasAssignment) throw DomainException.NotFound("Group not found.")

            val (highest, count) = c.prepareStatement("SELECT COALESCE(MAX(number), 0), COUNT(*) FROM letters WHERE group_id = ? AND receiver = ?").use { s ->
                s.setLong(1, groupId)
                s.setString(2, receiver.value)
                s.executeQuery().use { it.next(); it.getInt(1) to it.getInt(2) }
            }
            if (count >= maxLetters) throw DomainException.InvalidState("The letter limit has been reached.")

            val number = highest + 1
            c.prepareStatement("INSERT INTO letters (group_id, receiver, number, title, text, sent_at) VALUES (?, ?, ?, ?, ?, ?)").use { s ->
                s.setLong(1, groupId)
                s.setString(2, receiver.value)
                s.setInt(3, number)
                s.setString(4, title)
                s.setString(5, text)
                s.setLong(6, atMillis)
                s.executeUpdate()
            }
            Letter(number, title, text, atMillis)
        }

    override fun sentBy(groupId: Long, receiver: Username): List<Letter> = database.connection().use { c ->
        c.prepareStatement("SELECT $COLUMNS FROM letters WHERE group_id = ? AND receiver = ? ORDER BY number").use { s ->
            s.setLong(1, groupId)
            s.setString(2, receiver.value)
            s.executeQuery().use { rows -> generateSequence { if (rows.next()) toLetter(rows) else null }.toList() }
        }
    }

    override fun sentToSanta(groupId: Long, santa: Username): List<Letter> = database.connection().use { c ->
        c.prepareStatement(
            "SELECT l.number, l.title, l.text, l.sent_at FROM letters l " +
                "JOIN assignments a ON a.group_id = l.group_id AND a.receiver = l.receiver " +
                "WHERE l.group_id = ? AND a.santa = ? ORDER BY l.number",
        ).use { s ->
            s.setLong(1, groupId)
            s.setString(2, santa.value)
            s.executeQuery().use { rows -> generateSequence { if (rows.next()) toLetter(rows) else null }.toList() }
        }
    }

    private fun requireActive(c: Connection, groupId: Long) {
        val changed = c.createStatement().use { it.executeUpdate("UPDATE groups SET state = state WHERE id = $groupId AND state = 'active'") }
        if (changed == 0) {
            val exists = c.createStatement().use { s -> s.executeQuery("SELECT 1 FROM groups WHERE id = $groupId").use { it.next() } }
            throw if (exists) DomainException.InvalidState("Letters can only be sent while the group is active.") else DomainException.NotFound("Group not found.")
        }
    }

    private fun toLetter(rows: ResultSet) = Letter(
        number = rows.getInt("number"),
        title = rows.getString("title"),
        text = rows.getString("text"),
        sentAtMillis = rows.getLong("sent_at"),
    )

    private companion object {
        const val COLUMNS = "number, title, text, sent_at"
    }
}
