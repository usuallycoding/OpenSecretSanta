package com.usuallycoding.opensecretsanta.ui.navigation

import androidx.navigation.NavController
import com.usuallycoding.opensecretsanta.contract.NotificationData

/**
 * Where tapping a notification goes. The push message names the group and the kind of notification; what the kind is about
 * decides the page and tab, and the group's own page is always behind it so that Back goes up one level at a time.
 */
object NotificationTarget {
    /**
     * The screens to open, in order, for a notification of [category] about group [groupId]. A kind the app does not know opens the group.
     * [target] says which of a kind's notifications it is when the kind alone does not: the announcements category also holds being added
     * to a group and the draw, which open the group, so only an announcement the owner sent opens the announcements.
     */
    fun routesOf(category: String?, groupId: Long, target: String? = null): List<Any> = buildList {
        add(GroupRoute(groupId))
        when (category) {
            // the Santa's question is answered by the receiver, on the page of the questions from their Santa
            "questions" -> add(GroupPane.ReceiverQuestions.route(groupId))
            // the receiver's answer and wishlist are read by the Santa, on the pages for what they give
            "answers" -> add(GroupPane.GifterQuestions.route(groupId))
            "wishlist" -> add(GroupPane.GifterWishlist.route(groupId))
            // the receiver's letters are read by the Santa, in the inbox
            "letters" -> add(GroupPane.Inbox.route(groupId))
            // an announcement the owner sent is read on the Announcements page
            "announcements" -> if (target == NotificationData.TARGET_MESSAGES) add(GroupPane.Announcements.route(groupId))
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
