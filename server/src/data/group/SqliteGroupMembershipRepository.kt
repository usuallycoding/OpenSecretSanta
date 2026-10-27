package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.admin.GroupMembershipRepository
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database

class SqliteGroupMembershipRepository(private val database: Database) : GroupMembershipRepository {

    override fun membersOfGroupsOwnedBy(owner: Username): Set<Username> = database.connection().use { c ->
        c.prepareStatement(
            "SELECT DISTINCT m.username FROM group_members m " +
                "JOIN groups g ON g.id = m.group_id WHERE g.owner_username = ?",
        ).use { s ->
            s.setString(1, owner.value)
            s.executeQuery().use { rows ->
                generateSequence { if (rows.next()) Username.parse(rows.getString(1)) else null }.toSet()
            }
        }
    }
}
