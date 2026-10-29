package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.admin.GroupMembershipRepository
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database

/**
 * The users an admin manages through groups: participants of the groups they own that have left
 * Draft, and only those who already have an account.
 */
class SqliteGroupMembershipRepository(private val database: Database) : GroupMembershipRepository {

    override fun membersOfGroupsOwnedBy(owner: Username): Set<Username> = database.connection().use { c ->
        c.prepareStatement(
            "SELECT DISTINCT u.username FROM group_participants p " +
                "JOIN groups g ON g.id = p.group_id " +
                "JOIN users u ON u.username = p.username " +
                "WHERE g.owner_username = ? AND g.state <> 'draft'",
        ).use { s ->
            s.setString(1, owner.value)
            s.executeQuery().use { rows ->
                generateSequence { if (rows.next()) Username.parse(rows.getString(1)) else null }.toSet()
            }
        }
    }

    override fun unassignedUsersCreatedBy(admin: Username): Set<Username> = database.connection().use { c ->
        c.prepareStatement(
            "SELECT u.username FROM users u WHERE u.created_by = ? AND u.is_admin = 0 " +
                "AND NOT EXISTS (SELECT 1 FROM group_participants p WHERE p.username = u.username)",
        ).use { s ->
            s.setString(1, admin.value)
            s.executeQuery().use { rows ->
                generateSequence { if (rows.next()) Username.parse(rows.getString(1)) else null }.toSet()
            }
        }
    }
}
