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

/** What the person's receiver keeps for one group, read-only. */
@Serializable
data class ReceiverWishlistRoute(val groupId: Long)
