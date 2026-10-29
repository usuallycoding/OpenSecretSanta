package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.DeletionImpact
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.user.insertUsers
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * Every state change names the state it expects in its `UPDATE ... WHERE state = ...`, so a stale
 * or repeated request changes nothing and is reported as `InvalidState`.
 */
class SqliteGroupRepository(private val database: Database) : GroupRepository {

    override fun create(owner: Username, details: GroupDetails, participants: List<Username>): Group =
        database.transaction { c ->
            val id = c.prepareStatement(
                "INSERT INTO groups (owner_username, name, description, location, event_date, event_time) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            ).use { s ->
                s.setString(1, owner.value)
                bindDetails(s, details, firstIndex = 2)
                s.executeUpdate()
                s.generatedKeys.use { keys -> keys.next(); keys.getLong(1) }
            }
            writeParticipants(c, id, participants)
            load(c, id)!!
        }

    override fun find(id: Long): Group? = database.connection().use { load(it, id) }

    /** Newest first: by the time of creation, and within one second by the later `id`, which is never reused. */
    override fun findOwnedBy(owner: Username): List<Group> = database.connection().use { c ->
        loadAll(c, c.prepareStatement("SELECT id FROM groups WHERE owner_username = ? ORDER BY created_at DESC, id DESC")) { it.setString(1, owner.value) }
    }

    /** Newest first, like [findOwnedBy]. */
    override fun findVisibleTo(username: Username): List<Group> = database.connection().use { c ->
        loadAll(
            c,
            c.prepareStatement(
                "SELECT g.id FROM groups g JOIN group_participants p ON p.group_id = g.id " +
                    "WHERE p.username = ? AND g.state <> 'draft' ORDER BY g.created_at DESC, g.id DESC",
            ),
        ) { it.setString(1, username.value) }
    }

    override fun updateDetails(id: Long, details: GroupDetails): Group = database.transaction { c ->
        val changed = c.prepareStatement(
            "UPDATE groups SET name = ?, description = ?, location = ?, event_date = ?, event_time = ? " +
                "WHERE id = ? AND state <> 'archived'",
        ).use { s ->
            bindDetails(s, details, firstIndex = 1)
            s.setLong(6, id)
            s.executeUpdate()
        }
        if (changed == 0) throw missingOrWrongState(c, id, "An archived group can no longer be edited.")
        load(c, id)!!
    }

    /** The participants, draw, wishlist items, questions and answers go with the group through their `ON DELETE CASCADE`. */
    override fun delete(id: Long) {
        database.transaction { c ->
            val changed = c.prepareStatement("DELETE FROM groups WHERE id = ?").use { s ->
                s.setLong(1, id)
                s.executeUpdate()
            }
            if (changed == 0) throw DomainException.NotFound("Group not found.")
        }
    }

    override fun impact(id: Long): DeletionImpact = database.connection().use { c ->
        fun count(sql: String): Int = c.prepareStatement(sql).use { s ->
            s.setLong(1, id)
            s.executeQuery().use { rows -> rows.next(); rows.getInt(1) }
        }
        val exists = count("SELECT COUNT(*) FROM groups WHERE id = ?") > 0
        if (!exists) throw DomainException.NotFound("Group not found.")
        DeletionImpact(
            wishlistItems = count("SELECT COUNT(*) FROM wishlist_items WHERE group_id = ?"),
            questions = count("SELECT COUNT(*) FROM questions WHERE group_id = ?"),
            answers = count("SELECT COUNT(*) FROM questions WHERE group_id = ? AND answer IS NOT NULL"),
            draw = count("SELECT COUNT(*) FROM assignments WHERE group_id = ?") > 0,
        )
    }

    override fun replaceParticipants(id: Long, participants: List<Username>, newUsers: List<NewUser>): Group =
        database.transaction { c ->
            changeState(c, id, setOf(GroupState.DRAFT, GroupState.PENDING), to = null)
            c.prepareStatement("DELETE FROM group_participants WHERE group_id = ?").use { s ->
                s.setLong(1, id)
                s.executeUpdate()
            }
            writeParticipants(c, id, participants)
            if (newUsers.isNotEmpty()) insertUsers(c, newUsers)
            load(c, id)!!
        }

    override fun moveToPending(id: Long, newUsers: List<NewUser>): Group = database.transaction { c ->
        changeState(c, id, setOf(GroupState.DRAFT), to = GroupState.PENDING)
        if (newUsers.isNotEmpty()) insertUsers(c, newUsers)
        load(c, id)!!
    }

    override fun activate(id: Long, assignments: List<Assignment>): Group = database.transaction { c ->
        changeState(c, id, setOf(GroupState.PENDING), to = GroupState.ACTIVE)

        val stored = participantsOf(c, id).toSet()
        if (assignments.map { it.santa }.toSet() != stored || assignments.map { it.receiver }.toSet() != stored) {
            throw DomainException.InvalidState("The participants changed while the assignments were being drawn. Try again.")
        }
        c.prepareStatement("INSERT INTO assignments (group_id, santa, receiver) VALUES (?, ?, ?)").use { s ->
            assignments.forEach { pair ->
                s.setLong(1, id)
                s.setString(2, pair.santa.value)
                s.setString(3, pair.receiver.value)
                s.executeUpdate()
            }
        }
        load(c, id)!!
    }

    override fun archive(id: Long): Group = database.transaction { c ->
        changeState(c, id, setOf(GroupState.ACTIVE), to = GroupState.ARCHIVED)
        load(c, id)!!
    }

    override fun assignments(id: Long): List<Assignment> = database.connection().use { c ->
        c.prepareStatement("SELECT santa, receiver FROM assignments WHERE group_id = ? ORDER BY rowid").use { s ->
            s.setLong(1, id)
            s.executeQuery().use { rows ->
                generateSequence { if (rows.next()) Assignment(Username.parse(rows.getString(1)), Username.parse(rows.getString(2))) else null }.toList()
            }
        }
    }

    override fun receiverOf(groupId: Long, santa: Username): Username? = database.connection().use { c ->
        c.prepareStatement("SELECT receiver FROM assignments WHERE group_id = ? AND santa = ?").use { s ->
            s.setLong(1, groupId)
            s.setString(2, santa.value)
            s.executeQuery().use { rows -> if (rows.next()) Username.parse(rows.getString(1)) else null }
        }
    }

    override fun santaOf(groupId: Long, receiver: Username): Username? = database.connection().use { c ->
        c.prepareStatement("SELECT santa FROM assignments WHERE group_id = ? AND receiver = ?").use { s ->
            s.setLong(1, groupId)
            s.setString(2, receiver.value)
            s.executeQuery().use { rows -> if (rows.next()) Username.parse(rows.getString(1)) else null }
        }
    }

    // --- helpers ---

    /** Moves the group to [to] (or only checks the state when null) if it is currently in one of [from]. */
    private fun changeState(c: Connection, id: Long, from: Set<GroupState>, to: GroupState?) {
        val allowed = from.joinToString(", ") { "'${it.storedName}'" }
        val target = to?.let { "'${it.storedName}'" } ?: "state"
        val changed = c.createStatement().use { it.executeUpdate("UPDATE groups SET state = $target WHERE id = $id AND state IN ($allowed)") }
        if (changed == 0) {
            throw missingOrWrongState(c, id, "The group is not in a state that allows this (expected ${from.joinToString(" or ") { it.storedName }}).")
        }
    }

    private fun missingOrWrongState(c: Connection, id: Long, wrongStateMessage: String): DomainException {
        val exists = c.prepareStatement("SELECT 1 FROM groups WHERE id = ?").use { s ->
            s.setLong(1, id)
            s.executeQuery().use { it.next() }
        }
        return if (exists) DomainException.InvalidState(wrongStateMessage) else DomainException.NotFound("Group not found.")
    }

    private fun bindDetails(s: java.sql.PreparedStatement, details: GroupDetails, firstIndex: Int) {
        s.setString(firstIndex, details.name)
        s.setString(firstIndex + 1, details.description)
        s.setString(firstIndex + 2, details.location)
        s.setString(firstIndex + 3, details.date?.toString())
        s.setString(firstIndex + 4, details.time?.toString())
    }

    private fun writeParticipants(c: Connection, id: Long, participants: List<Username>) {
        c.prepareStatement("INSERT INTO group_participants (group_id, username, position) VALUES (?, ?, ?)").use { s ->
            participants.forEachIndexed { index, username ->
                s.setLong(1, id)
                s.setString(2, username.value)
                s.setInt(3, index)
                s.executeUpdate()
            }
        }
    }

    private fun participantsOf(c: Connection, id: Long): List<Username> =
        c.prepareStatement("SELECT username FROM group_participants WHERE group_id = ? ORDER BY position").use { s ->
            s.setLong(1, id)
            s.executeQuery().use { rows -> generateSequence { if (rows.next()) Username.parse(rows.getString(1)) else null }.toList() }
        }

    private fun load(c: Connection, id: Long): Group? =
        c.prepareStatement("SELECT id, owner_username, name, description, location, event_date, event_time, state FROM groups WHERE id = ?").use { s ->
            s.setLong(1, id)
            s.executeQuery().use { rows -> if (rows.next()) toGroup(rows, participantsOf(c, id)) else null }
        }

    private fun loadAll(c: Connection, query: java.sql.PreparedStatement, bind: (java.sql.PreparedStatement) -> Unit): List<Group> {
        val ids = query.use { s ->
            bind(s)
            s.executeQuery().use { rows -> generateSequence { if (rows.next()) rows.getLong(1) else null }.toList() }
        }
        return ids.mapNotNull { load(c, it) }
    }

    private fun toGroup(rows: ResultSet, participants: List<Username>) = Group(
        id = rows.getLong("id"),
        owner = Username.parse(rows.getString("owner_username")),
        details = GroupDetails(
            name = rows.getString("name"),
            description = rows.getString("description"),
            location = rows.getString("location"),
            date = rows.getString("event_date")?.let(LocalDate::parse),
            time = rows.getString("event_time")?.let(LocalTime::parse),
        ),
        state = GroupState.fromStored(rows.getString("state")),
        participants = participants,
    )
}
