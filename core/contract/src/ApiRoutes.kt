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

    fun adminUser(username: String) = "$ADMIN_USERS/$username"
    fun adminUserResetPassword(username: String) = "$ADMIN_USERS/$username/reset-password"
    fun adminUserPromote(username: String) = "$ADMIN_USERS/$username/promote"
}
