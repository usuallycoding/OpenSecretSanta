package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

/** One of the caller's own wishlist items; times are ISO-8601 UTC instants. */
@Serializable
data class WishlistItemDto(
    val id: Long,
    val title: String,
    val note: String? = null,
    val url: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

/** Replaces all the editable fields of an item; [url] must be an `http` or `https` address. */
@Serializable
data class WishlistItemRequest(
    val title: String,
    val note: String? = null,
    val url: String? = null,
)

/** The complete list of the caller's item ids for the group, in the desired order. */
@Serializable
data class ReorderWishlistRequest(
    val itemIds: List<Long>,
)

/**
 * An item as a Santa sees it. There is deliberately no id: a Santa can read a list but never address
 * an item in it.
 */
@Serializable
data class SharedWishlistItemDto(
    val title: String,
    val note: String? = null,
    val url: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
