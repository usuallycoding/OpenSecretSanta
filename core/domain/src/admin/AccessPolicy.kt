package com.usuallycoding.opensecretsanta.domain.admin

import com.usuallycoding.opensecretsanta.domain.user.Username

interface GroupMembershipRepository {
    /** The members of every group [owner] owns, without [owner] themself unless they are a member. */
    fun membersOfGroupsOwnedBy(owner: Username): Set<Username>

    /** The non-administrator accounts [admin] created that no group, of any owner or state, names. */
    fun unassignedUsersCreatedBy(admin: Username): Set<Username>
}

/**
 * Decides which users an admin may manage: themself, the members of groups they own, and the accounts they created
 * that are in no group (a stray account, such as one made by a typo, would otherwise be out of everyone's reach).
 */
class AccessPolicy(private val memberships: GroupMembershipRepository) {

    fun managedUsernames(admin: Username): Set<Username> =
        memberships.membersOfGroupsOwnedBy(admin) + memberships.unassignedUsersCreatedBy(admin) + admin

    fun canManage(admin: Username, target: Username): Boolean =
        target == admin ||
            target in memberships.membersOfGroupsOwnedBy(admin) ||
            target in memberships.unassignedUsersCreatedBy(admin)

    /** The managed users [admin] may delete: the ones they created that are in no group. */
    fun deletableUsernames(admin: Username): Set<Username> = memberships.unassignedUsersCreatedBy(admin)

    fun canDelete(admin: Username, target: Username): Boolean = target in deletableUsernames(admin)
}
