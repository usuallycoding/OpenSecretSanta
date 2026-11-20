package com.usuallycoding.opensecretsanta.domain.wishlist

/** Resolution state of a cached wishlist link preview. */
enum class LinkPreviewStatus {
    PENDING,
    READY,
    FAILED,
}

/** Cached metadata for a single URL; thumbnail bytes are stored and served separately via [id]. */
data class LinkPreview(
    val id: Long,
    val url: String,
    val status: LinkPreviewStatus,
    val title: String? = null,
    val siteName: String? = null,
    val hasImage: Boolean = false,
    val fetchedAtMillis: Long? = null,
    val retryAfterMillis: Long? = null,
)

/** Cached thumbnail payload served to authenticated clients. */
data class LinkPreviewImage(
    val bytes: ByteArray,
    val contentType: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinkPreviewImage) return false
        return contentType == other.contentType && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + contentType.hashCode()
}

/** Persistent store for cached link previews and thumbnails. */
interface LinkPreviewStore {
    fun findByUrls(urls: Collection<String>): Map<String, LinkPreview>
    fun findImageById(id: Long): LinkPreviewImage?
    fun markPending(url: String): LinkPreview
    fun saveReady(url: String, title: String, siteName: String?, image: LinkPreviewImage?, fetchedAtMillis: Long): LinkPreview
    fun saveFailed(url: String, fetchedAtMillis: Long, retryAfterMillis: Long): LinkPreview
    fun findUncachedWishlistUrls(): List<String>
    fun deleteUnreferenced(): Int
}

/** Non-blocking entry point for resolving and querying cached link previews. */
interface LinkPreviewResolver {
    /**
     * Returns currently known previews for [urls] and enqueues background fetches for any URL that has no entry
     * or whose failure cooldown has elapsed. Never blocks on network I/O.
     */
    fun lookupAndEnqueue(urls: Collection<String>): Map<String, LinkPreview>

    /** Enqueues background fetches for [urls] that do not have a fresh cache entry. */
    fun enqueue(urls: Collection<String>)

    /** Returns the cached thumbnail image for [id], or null if missing or absent. */
    fun findImage(id: Long): LinkPreviewImage?
}
