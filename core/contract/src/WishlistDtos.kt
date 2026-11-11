package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

/**
 * One of the caller's own wishlist items; times are ISO-8601 UTC instants. [urls] are its links in order; [url] is the first
 * of them, kept (deprecated) for apps that know one link.
 */
@Serializable
data class WishlistItemDto(
    val id: Long,
    val title: String,
    val note: String? = null,
    val url: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val urls: List<String> = emptyList(),
)

/**
 * Replaces all the editable fields of an item; each of [urls], at most 10, must be an `http` or `https` address. An app that
 * predates several links sends only the deprecated [url]: it then stands for the first link, and the others are left as they are.
 */
@Serializable
data class WishlistItemRequest(
    val title: String,
    val note: String? = null,
    val url: String? = null,
    val urls: List<String>? = null,
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
    val urls: List<String> = emptyList(),
)
