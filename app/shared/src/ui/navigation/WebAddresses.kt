package com.usuallycoding.opensecretsanta.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute

/**
 * The readable addresses of the web app, the part after the `#`: `#/groups/2`, `#/profile`,
 * `#/admin/users/alice`. Pure, so that the mapping can be tested without a browser.
 */
object WebAddresses {
    private val USERNAME = Regex("[a-z0-9._-]+")

    /** The address of a screen, or null for something that is not one of the app's routes. */
    fun addressOf(route: Any): String? = when (route) {
        GroupsRoute -> "#/groups"
        is GroupRoute -> "#/groups/${route.id}"
        is GifterRoute -> "#/groups/${route.groupId}/gifter"
        is ReceiverRoute -> "#/groups/${route.groupId}/receiver"
        ProfileRoute -> "#/profile"
        SettingsRoute -> "#/settings"
        AdminGroupsRoute -> "#/admin/groups"
        AdminGroupNewRoute -> "#/admin/groups/new"
        is AdminGroupRoute -> "#/admin/groups/${route.id}"
        is AdminGroupEditRoute -> "#/admin/groups/${route.id}/edit"
        AdminUsersRoute -> "#/admin/users"
        is AdminUserRoute -> "#/admin/users/${route.username}"
        else -> null
    }

    /**
     * The screen an address names, or null when it names none, or names an admin screen to someone
     * who is not an administrator. A trailing slash is tolerated.
     */
    fun routeOf(fragment: String, isAdmin: Boolean): Any? {
        val parts = fragment.removePrefix("#").split('/').filter { it.isNotEmpty() }
        return when (parts.firstOrNull()) {
            "groups" -> groupRoute(parts.drop(1))
            "profile" -> if (parts.size == 1) ProfileRoute else null
            "settings" -> if (parts.size == 1) SettingsRoute else null
            "admin" -> if (isAdmin) adminRoute(parts.drop(1)) else null
            else -> null
        }
    }

    private fun groupRoute(parts: List<String>): Any? {
        if (parts.isEmpty()) return GroupsRoute
        val id = parts[0].toLongOrNull() ?: return null
        return when (parts.drop(1)) {
            emptyList<String>() -> GroupRoute(id)
            listOf("gifter") -> GifterRoute(id)
            listOf("receiver") -> ReceiverRoute(id)
            else -> null
        }
    }

    private fun adminRoute(parts: List<String>): Any? = when (parts.firstOrNull()) {
        "groups" -> when {
            parts.size == 1 -> AdminGroupsRoute
            parts[1] == "new" -> if (parts.size == 2) AdminGroupNewRoute else null
            else -> {
                val id = parts[1].toLongOrNull()
                when {
                    id == null -> null
                    parts.size == 2 -> AdminGroupRoute(id)
                    parts.size == 3 && parts[2] == "edit" -> AdminGroupEditRoute(id)
                    else -> null
                }
            }
        }
        "users" -> when {
            parts.size == 1 -> AdminUsersRoute
            parts.size == 2 && USERNAME.matches(parts[1]) -> AdminUserRoute(parts[1])
            else -> null
        }
        else -> null
    }

    /** The address of a back stack entry, or null for an entry that is not one of the app's screens. */
    fun addressOf(entry: NavBackStackEntry): String? {
        val d = entry.destination
        val route: Any = when {
            d.hasRoute<GroupsRoute>() -> GroupsRoute
            d.hasRoute<GroupRoute>() -> entry.toRoute<GroupRoute>()
            d.hasRoute<GifterRoute>() -> entry.toRoute<GifterRoute>()
            d.hasRoute<ReceiverRoute>() -> entry.toRoute<ReceiverRoute>()
            d.hasRoute<ProfileRoute>() -> ProfileRoute
            d.hasRoute<SettingsRoute>() -> SettingsRoute
            d.hasRoute<AdminGroupsRoute>() -> AdminGroupsRoute
            d.hasRoute<AdminGroupNewRoute>() -> AdminGroupNewRoute
            d.hasRoute<AdminGroupRoute>() -> entry.toRoute<AdminGroupRoute>()
            d.hasRoute<AdminGroupEditRoute>() -> entry.toRoute<AdminGroupEditRoute>()
            d.hasRoute<AdminUsersRoute>() -> AdminUsersRoute
            d.hasRoute<AdminUserRoute>() -> entry.toRoute<AdminUserRoute>()
            else -> return null
        }
        return addressOf(route)
    }
}

/** Whether a screen belongs to the admin view. */
fun NavDestination.isAdminScreen(): Boolean =
    hasRoute<AdminGroupsRoute>() || hasRoute<AdminGroupNewRoute>() || hasRoute<AdminGroupRoute>() ||
        hasRoute<AdminGroupEditRoute>() || hasRoute<AdminUsersRoute>() || hasRoute<AdminUserRoute>()
