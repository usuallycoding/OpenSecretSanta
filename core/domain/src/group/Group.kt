package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * A Secret Santa group. [participants] are usernames in the owner's order; while the group is
 * Draft they need not have accounts.
 */
data class Group(
    val id: Long,
    val owner: Username,
    val details: GroupDetails,
    val state: GroupState,
    val participants: List<Username>,
)

/** [santa] gives a gift to [receiver]. */
data class Assignment(val santa: Username, val receiver: Username)
