package com.usuallycoding.opensecretsanta.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.usuallycoding.opensecretsanta.ui.admin.AdminPhase
import com.usuallycoding.opensecretsanta.ui.admin.DrawState

/**
 * What the owner's group screen opens beside it on a wide window, and as a page of its own on a narrow one: the draw rules, one pair of
 * the draw and the announcements. Each has a route, and so a web address, of its own; the window's width only chooses how it is shown.
 */
sealed interface AdminPane {
    data object Rules : AdminPane

    data class Pair(val santa: String, val receiver: String) : AdminPane

    data object Announcements : AdminPane

    /** The route of this pane for [groupId]. */
    fun route(groupId: Long): Any = when (this) {
        Rules -> AdminGroupRulesRoute(groupId)
        is Pair -> AdminGroupPairRoute(groupId, santa, receiver)
        Announcements -> AdminAnnouncementsRoute(groupId)
    }

    /**
     * Whether this pane has anything to show in a group in [phase] with [draw]: true, false, or null while that is not known yet. The rules
     * and the announcements are there from Pending on, a pair once the names are drawn and only when it is one of the draw's pairs.
     */
    fun within(phase: AdminPhase?, draw: DrawState): Boolean? {
        if (phase == null) return null
        return when (this) {
            Rules, Announcements -> phase == AdminPhase.Pending || phase == AdminPhase.Active || phase == AdminPhase.Archived
            is Pair -> when {
                phase != AdminPhase.Active && phase != AdminPhase.Archived -> false
                draw is DrawState.Pairs -> draw.pairs.any { it.santa == santa && it.receiver == receiver }
                draw == DrawState.None -> null
                // the draw could not be read: the pair's own content loads and fails by itself
                else -> true
            }
        }
    }
}

/** The pane an owner's route opens, or null for a route that is not one of the owner's panes. */
fun adminPaneOf(route: Any): AdminPane? = when (route) {
    is AdminGroupRulesRoute -> AdminPane.Rules
    is AdminGroupPairRoute -> AdminPane.Pair(route.santa, route.receiver)
    is AdminAnnouncementsRoute -> AdminPane.Announcements
    else -> null
}

/** The group an owner's pane route is about, or null for a route that is not one. */
fun adminGroupIdOfPane(route: Any): Long? = when (route) {
    is AdminGroupRulesRoute -> route.id
    is AdminGroupPairRoute -> route.id
    is AdminAnnouncementsRoute -> route.id
    else -> null
}

/*
 * The two functions below are the only places that list the routes of the owner's panes for a destination or a back stack entry: the back
 * arrow, the sidebar, the open group and the address bar ask them, so that a route cannot be missed by one list and not by another.
 */

/** Whether a screen is the route of one of the owner's panes. */
fun NavDestination.isAdminPane(): Boolean =
    hasRoute<AdminGroupRulesRoute>() || hasRoute<AdminGroupPairRoute>() || hasRoute<AdminAnnouncementsRoute>()

/** The route, with its arguments, of a back stack entry that is one of the owner's panes, or null for any other entry. */
fun NavBackStackEntry.adminPaneRoute(): Any? = when {
    destination.hasRoute<AdminGroupRulesRoute>() -> toRoute<AdminGroupRulesRoute>()
    destination.hasRoute<AdminGroupPairRoute>() -> toRoute<AdminGroupPairRoute>()
    destination.hasRoute<AdminAnnouncementsRoute>() -> toRoute<AdminAnnouncementsRoute>()
    else -> null
}

/**
 * Opens [to] for [groupId] from the group's screen ([current] null) or from another pane, which it replaces, so that the back stack is the
 * group list, the group and the one pane. Opening the pane that is open changes nothing.
 */
fun NavController.openAdminPane(current: AdminPane?, groupId: Long, to: AdminPane) {
    if (current == to) return
    navigate(to.route(groupId)) {
        if (current != null) popUpTo(current.route(groupId)) { inclusive = true }
        launchSingleTop = true
    }
}

/** Closes the pane [current], which an address may have opened with no group screen behind it, into the group's screen. */
fun NavController.closeAdminPane(current: AdminPane, groupId: Long) {
    navigate(AdminGroupRoute(groupId)) {
        popUpTo(current.route(groupId)) { inclusive = true }
        launchSingleTop = true
    }
}
