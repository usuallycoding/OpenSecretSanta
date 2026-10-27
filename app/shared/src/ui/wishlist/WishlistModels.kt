package com.usuallycoding.opensecretsanta.ui.wishlist

import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase

/** One of the person's own items, with the id needed to change it. */
data class WishItem(val id: Long, val title: String, val note: String?, val url: String?)

/** An item of the receiver's list: a Santa can read it but never address an item, so there is no id. */
data class SharedWishItem(val title: String, val note: String?, val url: String?)

/** The person's list for one group together with where the group is, which decides whether it can be changed. */
data class WishlistData(val items: List<WishItem>, val phase: GroupPhase) {
    val editable: Boolean get() = phase == GroupPhase.Pending || phase == GroupPhase.Active
}

enum class WishField(val key: String) { Title("title"), Note("note"), Url("url") }

/** The bottom sheet: adding when [itemId] is null, otherwise editing that item. */
data class ItemEditor(
    val itemId: Long?,
    val title: String = "",
    val note: String = "",
    val url: String = "",
    val errors: Map<WishField, String> = emptyMap(),
    val general: String? = null,
    val saving: Boolean = false,
) {
    val adding: Boolean get() = itemId == null
}

/** Something that happened while the person was busy, and that the screen says once. */
enum class WishlistNotice { ChangedElsewhere, ItemGone, Locked, ListFull, Unreachable }

data class WishlistUiState(
    val editor: ItemEditor? = null,
    /** The item whose deletion is waiting for a confirmation. */
    val deleting: WishItem? = null,
    /** A move is being saved. */
    val moving: Boolean = false,
    val notice: WishlistNotice? = null,
)

internal fun WishlistItemDto.toItem() = WishItem(id, title, note, url)

internal fun SharedWishlistItemDto.toItem() = SharedWishItem(title, note, url)
