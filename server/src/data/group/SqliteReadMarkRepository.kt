package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.gift.ReadKind
import com.usuallycoding.opensecretsanta.domain.gift.ReadMarkRepository
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database

/** A Santa's read marks, one row per group and kind. The row hangs off the Santa's assignment and goes with the group. */
class SqliteReadMarkRepository(private val database: Database) : ReadMarkRepository {

    override fun seenUpTo(groupId: Long, santa: Username): Map<ReadKind, Long> = database.connection().use { c ->
        c.prepareStatement("SELECT kind, seen_up_to FROM read_marks WHERE group_id = ? AND santa = ?").use { s ->
            s.setLong(1, groupId)
            s.setString(2, santa.value)
            s.executeQuery().use { rows ->
                buildMap { while (rows.next()) ReadKind.of(rows.getString(1))?.let { put(it, rows.getLong(2)) } }
            }
        }
    }

    override fun markSeen(groupId: Long, santa: Username, kind: ReadKind, atMillis: Long): Long = database.transaction { c ->
        c.prepareStatement(
            "INSERT INTO read_marks (group_id, santa, kind, seen_up_to) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (group_id, santa, kind) DO UPDATE SET seen_up_to = MAX(seen_up_to, excluded.seen_up_to)",
        ).use { s ->
            s.setLong(1, groupId)
            s.setString(2, santa.value)
            s.setString(3, kind.key)
            s.setLong(4, atMillis)
            s.executeUpdate()
        }
        c.prepareStatement("SELECT seen_up_to FROM read_marks WHERE group_id = ? AND santa = ? AND kind = ?").use { s ->
            s.setLong(1, groupId)
            s.setString(2, santa.value)
            s.setString(3, kind.key)
            s.executeQuery().use { it.next(); it.getLong(1) }
        }
    }
}
