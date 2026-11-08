package com.usuallycoding.opensecretsanta.ui.navigation

import androidx.navigation.NavController

/**
 * Opens [to] for [groupId], from the group's page ([current] null) or from another pane, which it replaces: the back stack is then the
 * group list, the group's page and the one pane, and Back, like the close icon, always leaves the pane for the group's page. Opening the
 * pane that is open changes nothing.
 */
fun NavController.openPane(current: GroupPane?, groupId: Long, to: GroupPane) {
    if (current == to) return
    navigate(to.route(groupId)) {
        if (current != null) popUpTo(current.route(groupId)) { inclusive = true }
        launchSingleTop = true
    }
}

/** Closes the pane [current], which an address or a notification may have opened with no group page behind it, into the group's page. */
fun NavController.closePane(current: GroupPane, groupId: Long) {
    navigate(GroupRoute(groupId)) {
        popUpTo(current.route(groupId)) { inclusive = true }
        launchSingleTop = true
    }
}

/** Replaces the pane [current] by the pane [to] that stands in for it in the group's phase, or by the group's page when [to] is null. */
fun NavController.replacePane(current: GroupPane, groupId: Long, to: GroupPane?) {
    if (to == null) closePane(current, groupId) else openPane(current, groupId, to)
}
