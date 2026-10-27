package com.usuallycoding.opensecretsanta.domain.wishlist

import com.usuallycoding.opensecretsanta.domain.error.DomainException

object WishlistLimits {
    const val MAX_ITEMS = 50
    const val MAX_TITLE_LENGTH = 200
    const val MAX_NOTE_LENGTH = 1000
    const val MAX_URL_LENGTH = 2000
}

/** An item as its owner sees it, with the id needed to edit, delete and reorder it. */
data class WishlistItem(
    val id: Long,
    val title: String,
    val note: String?,
    val url: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/** An item as a Santa sees it. There is deliberately no id: a Santa can read the list but never address an item. */
data class SharedWishlistItem(
    val title: String,
    val note: String?,
    val url: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/** The editable part of an item, validated once for both adding and editing. */
class WishlistEntry private constructor(
    val title: String,
    val note: String?,
    val url: String?,
) {
    companion object {
        fun of(title: String?, note: String?, url: String?): WishlistEntry {
            val cleanTitle = title?.trim().orEmpty()
            if (cleanTitle.isEmpty() || cleanTitle.length > WishlistLimits.MAX_TITLE_LENGTH) {
                throw DomainException.ValidationFailed("title", "Title must be 1 to ${WishlistLimits.MAX_TITLE_LENGTH} characters long.")
            }
            val cleanNote = optional(note)
            if (cleanNote != null && cleanNote.length > WishlistLimits.MAX_NOTE_LENGTH) {
                throw DomainException.ValidationFailed("note", "Note must be at most ${WishlistLimits.MAX_NOTE_LENGTH} characters long.")
            }
            val cleanUrl = optional(url)
            if (cleanUrl != null) {
                if (cleanUrl.length > WishlistLimits.MAX_URL_LENGTH) {
                    throw DomainException.ValidationFailed("url", "Link must be at most ${WishlistLimits.MAX_URL_LENGTH} characters long.")
                }
                if (!WebLink.isValid(cleanUrl)) {
                    throw DomainException.ValidationFailed("url", "Link must be an http or https address, for example https://shop.example/item.")
                }
            }
            return WishlistEntry(cleanTitle, cleanNote, cleanUrl)
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
