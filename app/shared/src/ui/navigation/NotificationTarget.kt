package com.usuallycoding.opensecretsanta.ui.navigation

import androidx.navigation.NavController
import com.usuallycoding.opensecretsanta.contract.NotificationData

/**
 * Where tapping a notification goes. A notification about a group names it, and the group's own page is always behind what it opens
 * so that Back goes up one level at a time; the daily digest is about no group and opens just the group list.
 */
object NotificationTarget {
    /**
     * The screens to open, in order, for a notification of [category] about group [groupId]; none, for the daily digest or anything
     * else with no group, which leaves the group list. A kind the app does not know opens the group. [target] says which of a kind's
     * notifications it is when the kind alone does not: the announcements category also holds being added to a group and the draw,
     * which open the group, so only an announcement the owner sent opens the announcements.
     */
    fun routesOf(category: String?, groupId: Long?, target: String? = null): List<Any> = buildList {
        if (category == NotificationData.CATEGORY_DAILY || groupId == null) return@buildList
        add(GroupRoute(groupId))
        // an announcement the owner sent is read on the Announcements page
        if (category == "announcements" && target == NotificationData.TARGET_MESSAGES) add(GroupPane.Announcements.route(groupId))
    }
}

/**
 * Opens a tapped notification's [routes] with the group list under them and nothing of the screens that were open before, as a
 * fresh start of the app would: a tap while the app is on another group leaves nothing of it behind.
 */
fun NavController.openNotification(routes: List<Any>) {
    navigate(GroupsRoute) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
    routes.forEach { navigate(it) }
}
