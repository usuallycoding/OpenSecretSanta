package com.usuallycoding.opensecretsanta.domain.wishlist

import com.usuallycoding.opensecretsanta.domain.error.DomainException

object WishlistLimits {
    const val MAX_ITEMS = 200
    const val MAX_TITLE_LENGTH = 200
    const val MAX_NOTE_LENGTH = 1000
    const val MAX_URL_LENGTH = 2000
    const val MAX_LINKS = 10
}

/** An item as its owner sees it, with the id needed to edit, delete and reorder it. */
data class WishlistItem(
    val id: Long,
    val title: String,
    val note: String?,
    val urls: List<String>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/** An item as a Santa sees it. There is deliberately no id: a Santa can read the list but never address an item. */
data class SharedWishlistItem(
    val title: String,
    val note: String?,
    val urls: List<String>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/**
 * The editable part of an item, validated once for both adding and editing. [urls] are the item's links in order: trimmed,
 * without blanks or repeats. An [onlyFirstLink] entry comes from an app that knows one link: it replaces just the first link
 * of an item and leaves the others, see [linksOver].
 */
class WishlistEntry private constructor(
    val title: String,
    val note: String?,
    val urls: List<String>,
    val onlyFirstLink: Boolean,
) {
    /** The links an item with [current] links has after this entry is applied to it. */
    fun linksOver(current: List<String>): List<String> =
        if (onlyFirstLink) (urls + current.drop(1)).distinct() else urls

    companion object {
        fun of(title: String?, note: String?, urls: List<String>): WishlistEntry {
            val cleanTitle = cleanTitle(title)
            val cleanNote = cleanNote(note)
            val clean = urls.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (clean.size > WishlistLimits.MAX_LINKS) {
                throw DomainException.ValidationFailed("urls", "An item can have at most ${WishlistLimits.MAX_LINKS} links.")
            }
            clean.forEachIndexed { index, link -> checkLink(link, "urls[$index]") }
            return WishlistEntry(cleanTitle, cleanNote, clean, onlyFirstLink = false)
        }

        /** An entry from an app that sends the one link `url`: it stands for the first link only. */
        fun ofFirstLink(title: String?, note: String?, url: String?): WishlistEntry {
            val cleanTitle = cleanTitle(title)
            val cleanNote = cleanNote(note)
            val link = optional(url)
            if (link != null) checkLink(link, "url")
            return WishlistEntry(cleanTitle, cleanNote, listOfNotNull(link), onlyFirstLink = true)
        }

        private fun cleanTitle(title: String?): String {
            val clean = title?.trim().orEmpty()
            if (clean.isEmpty() || clean.length > WishlistLimits.MAX_TITLE_LENGTH) {
                throw DomainException.ValidationFailed("title", "Title must be 1 to ${WishlistLimits.MAX_TITLE_LENGTH} characters long.")
            }
            return clean
        }

        private fun cleanNote(note: String?): String? {
            val clean = optional(note)
            if (clean != null && clean.length > WishlistLimits.MAX_NOTE_LENGTH) {
                throw DomainException.ValidationFailed("note", "Note must be at most ${WishlistLimits.MAX_NOTE_LENGTH} characters long.")
            }
            return clean
        }

        private fun checkLink(link: String, field: String) {
            if (link.length > WishlistLimits.MAX_URL_LENGTH) {
                throw DomainException.ValidationFailed(field, "Link must be at most ${WishlistLimits.MAX_URL_LENGTH} characters long.")
            }
            if (!WebLink.isValid(link)) {
                throw DomainException.ValidationFailed(field, "Link must be an http or https address, for example https://shop.example/item.")
            }
        }

        private fun optional(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }
    }
}

/**
 * The only kind of link a wishlist accepts: an absolute `http` or `https` address with a plain host,
 * so that a client can show it as a link without ever running anything. Deliberately stricter than a
 * browser: ASCII host names only (internationalized names must be given in `xn--` form), no user-info
 * (`user@host`), no whitespace or control characters. What follows the host is accepted as given.
 */
internal object WebLink {
    private const val MAX_HOST_LENGTH = 253
    private const val MAX_LABEL_LENGTH = 63
    private const val MAX_PORT = 65535

    fun isValid(link: String): Boolean {
        if (link.any { it.isWhitespace() || it.code < 0x20 || it.code == 0x7f }) return false

        val separator = link.indexOf("://")
        if (separator <= 0) return false
        val scheme = link.substring(0, separator).lowercase()
        if (scheme != "http" && scheme != "https") return false

        val rest = link.substring(separator + 3)
        val authority = rest.takeWhile { it != '/' && it != '?' && it != '#' }
        if (authority.isEmpty() || '@' in authority) return false

        val colon = authority.lastIndexOf(':')
        val host = if (colon >= 0) authority.substring(0, colon) else authority
        if (colon >= 0) {
            val port = authority.substring(colon + 1)
            if (port.isEmpty() || port.length > 5 || !port.all { it in '0'..'9' } || port.toInt() > MAX_PORT) return false
        }
        return isValidHost(host)
    }

    private fun isValidHost(host: String): Boolean {
        if (host.isEmpty() || host.length > MAX_HOST_LENGTH) return false
        return host.split('.').all { label ->
            label.length in 1..MAX_LABEL_LENGTH &&
                label.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' } &&
                !label.startsWith('-') &&
                !label.endsWith('-')
        }
    }
}
