package com.usuallycoding.opensecretsanta.data.api

import androidx.compose.ui.graphics.ImageBitmap
import com.usuallycoding.opensecretsanta.decodeImageBitmap

/**
 * Bounded in-memory LRU cache of decoded link preview images fetched via [fetchBytes].
 */
internal class BoundedImageCache<T : Any>(
    private val fetchBytes: suspend (String) -> ApiResult<ByteArray>,
    private val decode: (ByteArray) -> T?,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    private val entries = LinkedHashMap<String, T>()

    fun get(path: String): T? {
        val hit = entries.remove(path) ?: return null
        entries[path] = hit
        return hit
    }

    suspend fun load(path: String): T? {
        get(path)?.let { return it }
        val bytes = when (val result = fetchBytes(path)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return null
        }
        val decoded = decode(bytes) ?: return null
        put(path, decoded)
        return decoded
    }

    private fun put(path: String, value: T) {
        entries.remove(path)
        entries[path] = value
        while (entries.size > maxEntries) {
            val eldest = entries.keys.firstOrNull() ?: break
            entries.remove(eldest)
        }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 64
    }
}

/**
 * In-memory LRU cache of decoded link preview thumbnails fetched through the authenticated [ApiClient].
 * Keeps thumbnail loading uniform across Android, iOS, and WasmJS without CORS or extra image-loader dependencies.
 */
class LinkPreviewImageCache(api: ApiClient) {
    private val cache = BoundedImageCache(
        fetchBytes = { path -> api.linkPreviewImage(path) },
        decode = ::decodeImageBitmap,
    )

    /** Returns a cached thumbnail immediately if present, otherwise null. */
    fun get(path: String): ImageBitmap? = cache.get(path)

    /** Loads and caches the decoded thumbnail for [path], returning null if unavailable or undecodable. */
    suspend fun load(path: String): ImageBitmap? = cache.load(path)
}
