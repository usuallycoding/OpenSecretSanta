package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.AskedQuestion
import com.usuallycoding.opensecretsanta.domain.gift.QuestionRepository
import com.usuallycoding.opensecretsanta.domain.gift.ReceivedQuestion
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.Connection
import java.sql.ResultSet

/**
 * Every write first runs `UPDATE groups SET state = state WHERE id = ? AND state = 'active'`. That
 * checks the group is Active and takes SQLite's write lock, so the numbering, the cap and the insert
 * that follow cannot interleave with another writer.
 */
class SqliteQuestionRepository(private val database: Database) : QuestionRepository {

    override fun ask(groupId: Long, santa: Username, text: String, atMillis: Long, maxQuestions: Int): AskedQuestion =
        database.transaction { c ->
            requireActive(c, groupId)
            val hasAssignment = c.prepareStatement("SELECT 1 FROM assignments WHERE group_id = ? AND santa = ?").use { s ->
                s.setLong(1, groupId)
                s.setString(2, santa.value)
                s.executeQuery().use { it.next() }
            }
            if (!hasAssignment) throw DomainException.NotFound("Group not found.")

            val (highest, count) = c.prepareStatement("SELECT COALESCE(MAX(number), 0), COUNT(*) FROM questions WHERE group_id = ? AND santa = ?").use { s ->
                s.setLong(1, groupId)
                s.setString(2, santa.value)
                s.executeQuery().use { it.next(); it.getInt(1) to it.getInt(2) }
            }
            if (count >= maxQuestions) throw DomainException.InvalidState("The question limit has been reached.")

            val number = highest + 1
            c.prepareStatement("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES (?, ?, ?, ?, ?)").use { s ->
                s.setLong(1, groupId)
                s.setString(2, santa.value)
                s.setInt(3, number)
                s.setString(4, text)
                s.setLong(5, atMillis)
                s.executeUpdate()
            }
            AskedQuestion(number, text, atMillis, answer = null, answeredAtMillis = null, answerUpdatedAtMillis = null)
        }

    override fun answer(groupId: Long, receiver: Username, number: Int, text: String, atMillis: Long): ReceivedQuestion =
        database.transaction { c ->
            requireActive(c, groupId)
            val santa = c.prepareStatement("SELECT santa FROM assignments WHERE group_id = ? AND receiver = ?").use { s ->
                s.setLong(1, groupId)
                s.setString(2, receiver.value)
                s.executeQuery().use { if (it.next()) it.getString(1) else null }
            } ?: throw DomainException.NotFound("Question not found.")

            val changed = c.prepareStatement(
                "UPDATE questions SET answer = ?, answered_at = COALESCE(answered_at, ?), answer_updated_at = ? " +
                    "WHERE group_id = ? AND santa = ? AND number = ?",
            ).use { s ->
                s.setString(1, text)
                s.setLong(2, atMillis)
                s.setLong(3, atMillis)
                s.setLong(4, groupId)
                s.setString(5, santa)
                s.setInt(6, number)
                s.executeUpdate()
            }
            if (changed == 0) throw DomainException.NotFound("Question not found.")

            c.prepareStatement("SELECT $COLUMNS FROM questions WHERE group_id = ? AND santa = ? AND number = ?").use { s ->
                s.setLong(1, groupId)
                s.setString(2, santa)
                s.setInt(3, number)
                s.executeQuery().use { it.next(); toReceived(it) }
            }
        }

    override fun askedBy(groupId: Long, santa: Username): List<AskedQuestion> = database.connection().use { c ->
        c.prepareStatement("SELECT $COLUMNS FROM questions WHERE group_id = ? AND santa = ? ORDER BY number").use { s ->
            s.setLong(1, groupId)
            s.setString(2, santa.value)
            s.executeQuery().use { rows -> generateSequence { if (rows.next()) toAsked(rows) else null }.toList() }
        }
    }

    override fun askedTo(groupId: Long, receiver: Username): List<ReceivedQuestion> = database.connection().use { c ->
        c.prepareStatement(
            "SELECT q.number, q.text, q.asked_at, q.answer, q.answered_at, q.answer_updated_at FROM questions q " +
                "JOIN assignments a ON a.group_id = q.group_id AND a.santa = q.santa " +
                "WHERE q.group_id = ? AND a.receiver = ? ORDER BY q.number",
        ).use { s ->
            s.setLong(1, groupId)
            s.setString(2, receiver.value)
            s.executeQuery().use { rows -> generateSequence { if (rows.next()) toReceived(rows) else null }.toList() }
        }
    }

    private fun requireActive(c: Connection, groupId: Long) {
        val changed = c.createStatement().use { it.executeUpdate("UPDATE groups SET state = state WHERE id = $groupId AND state = 'active'") }
        if (changed == 0) {
            val exists = c.createStatement().use { s -> s.executeQuery("SELECT 1 FROM groups WHERE id = $groupId").use { it.next() } }
            throw if (exists) {
                DomainException.InvalidState("Questions and answers can only be written while the group is active.")
            } else {
                DomainException.NotFound("Group not found.")
            }
        }
    }

    private fun toAsked(rows: ResultSet) = AskedQuestion(
        number = rows.getInt("number"),
        text = rows.getString("text"),
        askedAtMillis = rows.getLong("asked_at"),
        answer = rows.getString("answer"),
        answeredAtMillis = rows.getLong("answered_at").takeUnless { rows.wasNull() },
        answerUpdatedAtMillis = rows.getLong("answer_updated_at").takeUnless { rows.wasNull() },
    )

    private fun toReceived(rows: ResultSet) = ReceivedQuestion(
        number = rows.getInt("number"),
        text = rows.getString("text"),
        askedAtMillis = rows.getLong("asked_at"),
        answer = rows.getString("answer"),
        answeredAtMillis = rows.getLong("answered_at").takeUnless { rows.wasNull() },
        answerUpdatedAtMillis = rows.getLong("answer_updated_at").takeUnless { rows.wasNull() },
    )

    private companion object {
        const val COLUMNS = "number, text, asked_at, answer, answered_at, answer_updated_at"
    }
}
