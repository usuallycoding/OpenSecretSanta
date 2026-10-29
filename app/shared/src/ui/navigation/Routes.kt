package com.usuallycoding.opensecretsanta.ui.navigation

import kotlinx.serialization.Serializable

/** Where the signed-in participant view can be. Typed routes: the arguments are fields, not strings. */
@Serializable
data object GroupsRoute

@Serializable
data class GroupRoute(val id: Long)

@Serializable
data object ProfileRoute

/** The settings page: appearance, notifications, changing the password and signing out. Both views share it, and it belongs to neither. */
@Serializable
data object SettingsRoute

/** The page for the relationship in which the person gives: the receiver's list and the person's questions to them. */
@Serializable
data class GifterRoute(val groupId: Long)

/** The page for the relationship in which the person receives: their own list and the questions from their Santa. */
@Serializable
data class ReceiverRoute(val groupId: Long)

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
