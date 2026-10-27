package com.usuallycoding.opensecretsanta.contract

object ApiRoutes {
    const val HEALTH = "/health"

    const val LOGIN = "/auth/login"

    const val ME = "/me"
    const val ME_PASSWORD = "/me/password"

    const val ADMIN_USERS = "/admin/users"
    const val ADMIN_USER = "/admin/users/{username}"
    const val ADMIN_USER_RESET_PASSWORD = "/admin/users/{username}/reset-password"
    const val ADMIN_USER_PROMOTE = "/admin/users/{username}/promote"

    const val ADMIN_GROUPS = "/admin/groups"
    const val ADMIN_GROUP = "/admin/groups/{id}"
    const val ADMIN_GROUP_PARTICIPANTS = "/admin/groups/{id}/participants"
    const val ADMIN_GROUP_PENDING = "/admin/groups/{id}/pending"
    const val ADMIN_GROUP_ACTIVATE = "/admin/groups/{id}/activate"
    const val ADMIN_GROUP_ARCHIVE = "/admin/groups/{id}/archive"
    const val ADMIN_GROUP_ASSIGNMENTS = "/admin/groups/{id}/assignments"

    const val GROUPS = "/groups"
    const val GROUP = "/groups/{id}"

    fun adminGroup(id: Long) = "$ADMIN_GROUPS/$id"
    fun adminGroupParticipants(id: Long) = "$ADMIN_GROUPS/$id/participants"
    fun adminGroupPending(id: Long) = "$ADMIN_GROUPS/$id/pending"
    fun adminGroupActivate(id: Long) = "$ADMIN_GROUPS/$id/activate"
    fun adminGroupArchive(id: Long) = "$ADMIN_GROUPS/$id/archive"
    fun adminGroupAssignments(id: Long) = "$ADMIN_GROUPS/$id/assignments"
    fun group(id: Long) = "$GROUPS/$id"

    fun adminUser(username: String) = "$ADMIN_USERS/$username"
    fun adminUserResetPassword(username: String) = "$ADMIN_USERS/$username/reset-password"
    fun adminUserPromote(username: String) = "$ADMIN_USERS/$username/promote"
}
