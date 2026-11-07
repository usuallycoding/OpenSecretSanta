package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.contract.DrawRuleDto
import com.usuallycoding.opensecretsanta.domain.group.RuleKind
import com.usuallycoding.opensecretsanta.ui.Strings

/** One draw rule as the owner reads it. */
data class DrawRuleItem(val id: Long, val giver: String, val receiver: String, val kind: RuleKind) {
    val sentence: String get() = Strings.ruleSentence(giver, receiver, kind == RuleKind.MUST)
}

internal fun DrawRuleDto.toItem(): DrawRuleItem? =
    RuleKind.fromStored(kind)?.let { DrawRuleItem(id, giver, receiver, it) }

/** Musts before nevers, each in order of giver and then receiver. */
fun List<DrawRuleItem>.inReadingOrder(): List<DrawRuleItem> =
    sortedWith(compareBy<DrawRuleItem> { it.kind != RuleKind.MUST }.thenBy { it.giver }.thenBy { it.receiver })

/** What the add form holds. A rule needs two different people. */
data class RuleForm(
    val giver: String? = null,
    val receiver: String? = null,
    val kind: RuleKind = RuleKind.MUST,
    val bothWays: Boolean = false,
) {
    val canAdd: Boolean get() = giver != null && receiver != null && giver != receiver
}

/** The rules page's content: the rules and what is needed to show and change them. */
data class DrawRulesData(
    val phase: AdminPhase,
    val participants: List<String>,
    val rules: List<DrawRuleItem>,
    val fewDraws: Boolean,
) {
    /** Rules change while the group is Pending and are only read afterwards. */
    val canEdit: Boolean get() = phase == AdminPhase.Pending
}

data class DrawRulesUi(
    val form: RuleForm = RuleForm(),
    val busy: Boolean = false,
    /** The server's explanation of a refused rule, or a failure to reach it; said until the owner changes the form. */
    val message: String? = null,
)
