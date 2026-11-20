package com.usuallycoding.opensecretsanta.server.data.preview

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreview
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewImage
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewResolver
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewStatus
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewStore
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Coordinates asynchronous, deduplicated link preview fetching, failure cooldowns, startup backfill,
 * and unreferenced cache cleanup without ever blocking HTTP request threads.
 */
class LinkPreviewService(
    private val store: LinkPreviewStore,
    private val fetcher: SafeHttpFetcher,
    private val clock: Clock,
    private val executor: Executor = defaultPreviewExecutor(),
    private val failureCooldownMillis: Long = DEFAULT_FAILURE_COOLDOWN_MILLIS,
) : LinkPreviewResolver {

    private val logger = LoggerFactory.getLogger(LinkPreviewService::class.java)
    private val inFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun lookupAndEnqueue(urls: Collection<String>): Map<String, LinkPreview> {
        val unique = urls.distinct()
        if (unique.isEmpty()) return emptyMap()
        val now = clock.nowMillis()
        val existing = store.findByUrls(unique)
        val result = LinkedHashMap<String, LinkPreview>(unique.size)

        for (url in unique) {
            val current = existing[url]
            when {
                current == null -> {
                    val pending = store.markPending(url)
                    result[url] = pending
                    scheduleFetch(url)
                }
                current.status == LinkPreviewStatus.READY -> {
                    result[url] = current
                }
                current.status == LinkPreviewStatus.PENDING -> {
                    result[url] = current
                    scheduleFetch(url)
                }
                current.status == LinkPreviewStatus.FAILED -> {
                    val retryAfter = current.retryAfterMillis ?: 0L
                    if (now >= retryAfter) {
                        val pending = store.markPending(url)
                        result[url] = pending
                        scheduleFetch(url)
                    }
                }
            }
        }
        return result
    }

    override fun enqueue(urls: Collection<String>) {
        lookupAndEnqueue(urls)
    }

    override fun findImage(id: Long): LinkPreviewImage? = store.findImageById(id)

    /** Runs orphan cleanup and queues any existing `wishlist_item_links` URLs lacking a preview. */
    fun startBackfill() {
        executor.execute {
            try {
                store.deleteUnreferenced()
                val uncached = store.findUncachedWishlistUrls()
                if (uncached.isNotEmpty()) {
                    enqueue(uncached)
                }
            } catch (e: Throwable) {
                logger.warn("Link preview startup backfill failed: ${e.message}")
            }
        }
    }

    /** Removes cached previews no longer referenced by any wishlist item. */
    fun pruneUnreferencedAsync() {
        executor.execute {
            try {
                store.deleteUnreferenced()
            } catch (e: Throwable) {
                logger.warn("Link preview orphan cleanup failed: ${e.message}")
            }
        }
    }

    private fun scheduleFetch(url: String) {
        if (!inFlight.add(url)) return
        try {
            executor.execute {
                try {
                    fetchAndStore(url)
                } catch (e: Throwable) {
                    logger.debug("Link preview fetch failed for {}: {}", url, e.message)
                    val now = clock.nowMillis()
                    runCatching { store.saveFailed(url, now, now + failureCooldownMillis) }
                } finally {
                    inFlight.remove(url)
                }
            }
        } catch (e: Throwable) {
            inFlight.remove(url)
            logger.warn("Could not schedule link preview fetch: ${e.message}")
        }
    }

    private fun fetchAndStore(url: String) {
        val now = clock.nowMillis()
        val html = fetcher.fetchHtml(url)
        val metadata = html?.let { HtmlMetadataParser.parse(it.bytes, it.contentType, it.finalUri) }
        if (metadata == null) {
            store.saveFailed(url, now, now + failureCooldownMillis)
            return
        }
        val thumbnail = metadata.imageUrl?.let { imageUrl ->
            fetcher.fetchImage(imageUrl)?.let { img ->
                ThumbnailResizer.resize(img.bytes, img.contentType)
            }
        }
        store.saveReady(url, metadata.title, metadata.siteName, thumbnail, now)
    }

    companion object {
        const val DEFAULT_FAILURE_COOLDOWN_MILLIS: Long = 6L * 60L * 60L * 1000L

        fun defaultPreviewExecutor(threads: Int = 4): Executor {
            val counter = AtomicInteger(1)
            return Executors.newFixedThreadPool(threads) { runnable ->
                Thread(runnable, "link-preview-${counter.getAndIncrement()}").apply { isDaemon = true }
            }
        }
    }
}
