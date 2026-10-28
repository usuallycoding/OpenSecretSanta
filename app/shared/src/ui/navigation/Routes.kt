package com.usuallycoding.opensecretsanta.ui.navigation

import kotlinx.serialization.Serializable

/** Where the signed-in participant view can be. Typed routes: the arguments are fields, not strings. */
@Serializable
data object GroupsRoute

@Serializable
data class GroupRoute(val id: Long)

@Serializable
data object ProfileRoute

/** The person's own wishlist for one group. */
@Serializable
data class WishlistRoute(val groupId: Long)

/** Who the person gives to, and the questions they asked them. */
@Serializable
data class ReceiverRoute(val groupId: Long)

/** The questions the person's Santa asked, and the person's answers. */
@Serializable
data class SantaQuestionsRoute(val groupId: Long)

/** Who the person's Santa was, once the group is archived. */
@Serializable
data class SantaRoute(val groupId: Long)

/** What the person's receiver keeps for one group, read-only. */
@Serializable
data class ReceiverWishlistRoute(val groupId: Long)

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
