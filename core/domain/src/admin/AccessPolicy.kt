package com.usuallycoding.opensecretsanta.domain.admin

import com.usuallycoding.opensecretsanta.domain.user.Username

interface GroupMembershipRepository {
    /** The members of every group [owner] owns, without [owner] themself unless they are a member. */
    fun membersOfGroupsOwnedBy(owner: Username): Set<Username>
}

/** Decides which users an admin may manage: themself plus members of groups they own. */
class AccessPolicy(private val memberships: GroupMembershipRepository) {

    fun managedUsernames(admin: Username): Set<Username> =
        memberships.membersOfGroupsOwnedBy(admin) + admin

    fun canManage(admin: Username, target: Username): Boolean =
        target == admin || target in memberships.membersOfGroupsOwnedBy(admin)
}
