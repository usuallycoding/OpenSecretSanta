package com.usuallycoding.opensecretsanta.ui.wishlist

import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.changedTime
import com.usuallycoding.opensecretsanta.ui.groups.formatInstant
import com.usuallycoding.opensecretsanta.ui.readmarks.Change
import com.usuallycoding.opensecretsanta.ui.readmarks.SeenBaseline
import com.usuallycoding.opensecretsanta.ui.readmarks.itemChange

/** One of the person's own items, with the id needed to change it; [changed] is set only when it was edited after it was added. */
data class WishItem(val id: Long, val title: String, val note: String?, val urls: List<String>, val added: String, val changed: String?)

/** An item of the receiver's list: a Santa can read it but never address an item, so there is no id. */
data class SharedWishItem(
    val title: String,
    val note: String?,
    val urls: List<String>,
    val added: String,
    val changed: String?,
    /** New or edited since the Santa last looked, when the Santa has marks to compare with; null otherwise. */
    val change: Change? = null,
)

/** The person's list for one group together with where the group is, which decides whether it can be changed. */
data class WishlistData(val items: List<WishItem>, val phase: GroupPhase) {
    val editable: Boolean get() = phase == GroupPhase.Pending || phase == GroupPhase.Active
}

enum class WishField(val key: String) { Title("title"), Note("note") }

/**
 * The bottom sheet: adding when [itemId] is null, otherwise editing that item. [urls] are its link fields, never none and at
 * most [WishlistLimits.MAX_LINKS]; [linkErrors] are by the place of a field, and [linksError] is about the links as a whole.
 */
data class ItemEditor(
    val itemId: Long?,
    val title: String = "",
    val note: String = "",
    val urls: List<String> = listOf(""),
    val errors: Map<WishField, String> = emptyMap(),
    val linkErrors: Map<Int, String> = emptyMap(),
    val linksError: String? = null,
    val general: String? = null,
    val saving: Boolean = false,
) {
    val adding: Boolean get() = itemId == null

    /** Another link field can be added. */
    val canAddLink: Boolean get() = urls.size < WishlistLimits.MAX_LINKS
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

/** The links of an item in order: [urls], or for a server that predates several links the one [url]. */
internal fun linksOf(urls: List<String>, url: String?): List<String> = urls.ifEmpty { listOfNotNull(url?.takeIf { it.isNotBlank() }) }

internal fun WishlistItemDto.toItem() = WishItem(id, title, note, linksOf(urls, url), formatInstant(createdAt), changedTime(createdAt, updatedAt))

internal fun SharedWishlistItemDto.toItem(baseline: SeenBaseline? = null) =
    SharedWishItem(title, note, linksOf(urls, url), formatInstant(createdAt), changedTime(createdAt, updatedAt), itemChange(createdAt, updatedAt, baseline))
