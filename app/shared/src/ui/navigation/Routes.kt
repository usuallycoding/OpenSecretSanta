package com.usuallycoding.opensecretsanta.ui.navigation

import kotlinx.serialization.Serializable

/** Where the signed-in participant view can be. Typed routes: the arguments are fields, not strings. */
@Serializable
data object GroupsRoute

@Serializable
data class GroupRoute(val id: Long)

/** The page where the person edits what they tell one group about where and how a gift can reach them. */
@Serializable
data class DeliveryInfoRoute(val groupId: Long)

/** The settings page: appearance, notifications, changing the password and signing out. Both views share it, and it belongs to neither. */
@Serializable
data object SettingsRoute

/** Which of a page's two tabs it opens on: the wishlist, which is the default, or the questions. */
enum class PageTab { Wishlist, Questions }

/**
 * The page for the relationship in which the person gives: the receiver's list and the person's questions to them. It opens on its
 * questions tab when [questions] is set. A route's arguments must be plain values: Navigation on the web and iOS cannot carry an enum.
 */
@Serializable
data class GifterRoute(val groupId: Long, val questions: Boolean = false) {
    val tab: PageTab get() = if (questions) PageTab.Questions else PageTab.Wishlist
}

/** The page for the relationship in which the person receives: their own list and the questions from their Santa. See [GifterRoute]. */
@Serializable
data class ReceiverRoute(val groupId: Long, val questions: Boolean = false) {
    val tab: PageTab get() = if (questions) PageTab.Questions else PageTab.Wishlist
}

/** The page where a receiver reads the letters they have sent their Santa and writes new ones while the group is Active. */
@Serializable
data class SendLetterRoute(val groupId: Long)

/** The page where a Santa reads the letters their receiver has sent them. */
@Serializable
data class InboxRoute(val groupId: Long)

/** The administrator's own groups: the first screen of the admin view. */
@Serializable
data object AdminGroupsRoute

/** The users the administrator manages. */
@Serializable
data object AdminUsersRoute

/** One of those users; the username is lowercase letters, digits, dots, underscores and hyphens. */
@Serializable
data class AdminUserRoute(val username: String)

/** The form for a new group. */
@Serializable
data object AdminGroupNewRoute

/** One of the administrator's groups. */
@Serializable
data class AdminGroupRoute(val id: Long)

/** The form for the details of an existing group. */
@Serializable
data class AdminGroupEditRoute(val id: Long)
