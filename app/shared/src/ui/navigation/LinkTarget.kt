package com.usuallycoding.opensecretsanta.ui.navigation

/**
 * Where a web address opened in the app goes: only the group screens that an email links to. The address is the part after
 * the `#`, read with the mapping the web app uses; the group's own page is always behind the Gifter and Receiver pages, as
 * it is for a notification, so that Back goes up one level at a time.
 */
object LinkTarget {
    /** The screens to open, in order, for the [fragment] of a link; null for an address that is not a group screen (admin, profile, settings, none, unknown). */
    fun routesOf(fragment: String?): List<Any>? {
        if (fragment.isNullOrBlank()) return null
        return when (val route = WebAddresses.routeOf(fragment, isAdmin = false)) {
            is GroupRoute -> listOf(route)
            is GifterRoute -> listOf(GroupRoute(route.groupId), route)
            is ReceiverRoute -> listOf(GroupRoute(route.groupId), route)
            else -> null
        }
    }
}
