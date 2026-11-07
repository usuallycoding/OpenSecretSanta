package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.DrawRule
import com.usuallycoding.opensecretsanta.domain.group.DrawRuleRepository
import com.usuallycoding.opensecretsanta.domain.group.NewDrawRule
import com.usuallycoding.opensecretsanta.domain.group.RuleKind
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.Statement

class SqliteDrawRuleRepository(private val database: Database) : DrawRuleRepository {

    override fun list(groupId: Long): List<DrawRule> = database.connection().use { c ->
        c.prepareStatement("SELECT id, giver, receiver, kind FROM draw_rules WHERE group_id = ? ORDER BY id").use { s ->
            s.setLong(1, groupId)
            s.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) {
                        add(
                            DrawRule(
                                rows.getLong("id"),
                                Username.parse(rows.getString("giver")),
                                Username.parse(rows.getString("receiver")),
                                RuleKind.fromStored(rows.getString("kind"))!!,
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun add(groupId: Long, rules: List<NewDrawRule>): List<DrawRule> = database.transaction { c ->
        rules.map { rule ->
            val id = c.prepareStatement(
                "INSERT INTO draw_rules (group_id, giver, receiver, kind) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            ).use { s ->
                s.setLong(1, groupId)
                s.setString(2, rule.giver.value)
                s.setString(3, rule.receiver.value)
                s.setString(4, rule.kind.storedName)
                s.executeUpdate()
                s.generatedKeys.use { keys -> keys.next(); keys.getLong(1) }
            }
            DrawRule(id, rule.giver, rule.receiver, rule.kind)
        }
    }

    override fun delete(groupId: Long, ruleId: Long) {
        database.transaction { c ->
            val changed = c.prepareStatement("DELETE FROM draw_rules WHERE group_id = ? AND id = ?").use { s ->
                s.setLong(1, groupId)
                s.setLong(2, ruleId)
                s.executeUpdate()
            }
            if (changed == 0) throw DomainException.NotFound("Rule not found.")
        }
    }
}
