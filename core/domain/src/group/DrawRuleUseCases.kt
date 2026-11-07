package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.Username

/** A group's rules and whether they leave few possible draws. */
class DrawRuleList(val rules: List<DrawRule>, val fewDraws: Boolean)

private fun Group.requireRulesVisible() {
    if (state == GroupState.DRAFT) throw DomainException.InvalidState("A Draft group has no draw rules yet.")
}

private fun Group.requireRulesEditable() {
    if (state != GroupState.PENDING) throw DomainException.InvalidState("Draw rules can only be changed while the group is Pending.")
}

/** The owner's rules for a Pending, Active or Archived group. */
class ListDrawRules(
    private val loadOwned: OwnedGroupLoader,
    private val rules: DrawRuleRepository,
) {
    operator fun invoke(actor: User, id: Long): DrawRuleList {
        val group = loadOwned(actor, id)
        group.requireRulesVisible()
        val stored = rules.list(group.id)
        return DrawRuleList(stored, fewDraws = group.state == GroupState.PENDING && DrawRules.fewDraws(group.participants, stored))
    }
}

/** Adds one rule, or with [bothWays] the rule and its reverse, refusing any set no draw could satisfy. */
class AddDrawRules(
    private val loadOwned: OwnedGroupLoader,
    private val rules: DrawRuleRepository,
) {
    operator fun invoke(actor: User, id: Long, giver: String, receiver: String, kind: RuleKind, bothWays: Boolean): List<DrawRule> {
        val group = loadOwned(actor, id)
        group.requireRulesEditable()
        val from = Username.parse(giver, field = "giver")
        val to = Username.parse(receiver, field = "receiver")
        val added = buildList {
            add(NewDrawRule(from, to, kind))
            if (bothWays) add(NewDrawRule(to, from, kind))
        }
        DrawRules.checkAddition(group.participants, rules.list(group.id), added)
        return rules.add(group.id, added)
    }
}

class DeleteDrawRule(
    private val loadOwned: OwnedGroupLoader,
    private val rules: DrawRuleRepository,
) {
    operator fun invoke(actor: User, id: Long, ruleId: Long) {
        val group = loadOwned(actor, id)
        group.requireRulesEditable()
        rules.delete(group.id, ruleId)
    }
}
