package com.usuallycoding.opensecretsanta.ui.navigation

import androidx.navigation.NavController

/**
 * Where tapping a notification goes. The push message names the group and the kind of notification; what the kind is about
 * decides the page and tab, and the group's own page is always behind it so that Back goes up one level at a time.
 */
object NotificationTarget {
    /** The screens to open, in order, for a notification of [category] about group [groupId]. A kind the app does not know opens the group. */
    fun routesOf(category: String?, groupId: Long): List<Any> = buildList {
        add(GroupRoute(groupId))
        when (category) {
            // the Santa's question is answered by the receiver, on the page for what they receive
            "questions" -> add(ReceiverRoute(groupId, questions = true))
            // the receiver's answer and wishlist are read by the Santa, on the page for what they give
            "answers" -> add(GifterRoute(groupId, questions = true))
            "wishlist" -> add(GifterRoute(groupId))
        }
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
