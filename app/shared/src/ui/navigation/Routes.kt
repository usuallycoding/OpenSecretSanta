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

/**
 * The Giving page of a wide window, with the receiver's wishlist, the person's questions to them and the Inbox side by side. On a small
 * screen its address opens the receiver's wishlist page, which is what its first tab was.
 */
@Serializable
data class GifterRoute(val groupId: Long)

/** The Receiving page of a wide window, with the person's wishlist, the questions from their Santa and the Outbox side by side. See [GifterRoute]. */
@Serializable
data class ReceiverRoute(val groupId: Long)

/** The receiver's wishlist, read-only, as a page of its own; on a wide window the group's page with it in a pane. */
@Serializable
data class GifterWishlistRoute(val groupId: Long)

/** The person's questions to their receiver and the answers, as a page of its own; on a wide window in a pane. */
@Serializable
data class GifterQuestionsRoute(val groupId: Long)

/** The person's own wishlist, as a page of its own; on a wide window in a pane. */
@Serializable
data class ReceiverWishlistRoute(val groupId: Long)

/** The questions the person's Santa has asked, with the person's answers, as a page of its own; on a wide window in a pane. */
@Serializable
data class ReceiverQuestionsRoute(val groupId: Long)

/** The page where a receiver reads the letters they have sent their Santa and writes new ones while the group is Active. */
@Serializable
data class SendLetterRoute(val groupId: Long)

/** The page where a Santa reads the letters their receiver has sent them. */
@Serializable
data class InboxRoute(val groupId: Long)

/**
 * The Announcements page, where a participant reads the announcements the group's owner has sent. A wide window has room for them beside the group's
 * page, so this address opens that page with them in a pane.
 */
@Serializable
data class AnnouncementsRoute(val groupId: Long)

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

/** The draw rules of one of the administrator's groups, as a page of its own on a narrow window. */
@Serializable
data class AdminGroupRulesRoute(val id: Long)

/** One pair of the owner's drawn group, as a page on a narrow window and a pane beside the group's page on a wide one. */
@Serializable
data class AdminGroupPairRoute(val id: Long, val santa: String, val receiver: String)

/** The Announcements page for the group's owner, who reads the announcements from the admin overview; on a wide window they open in a pane there. */
@Serializable
data class AdminAnnouncementsRoute(val id: Long)
