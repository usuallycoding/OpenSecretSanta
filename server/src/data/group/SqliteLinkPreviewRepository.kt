package com.usuallycoding.opensecretsanta.server.data.group

import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreview
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewImage
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewStatus
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewStore
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types

/**
 * SQLite-backed cache of link previews and resized thumbnails in `link_previews`.
 * Each URL has at most one row, shared across all wishlists.
 */
class SqliteLinkPreviewRepository(private val database: Database) : LinkPreviewStore {

    override fun findByUrls(urls: Collection<String>): Map<String, LinkPreview> {
        val unique = urls.distinct()
        if (unique.isEmpty()) return emptyMap()
        val placeholders = unique.joinToString(",") { "?" }
        return database.connection().use { c ->
            c.prepareStatement(
                "SELECT id, url, status, title, site_name, image_bytes IS NOT NULL AS has_image, fetched_at, retry_after " +
                    "FROM link_previews WHERE url IN ($placeholders)",
            ).use { s ->
                unique.forEachIndexed { index, url -> s.setString(index + 1, url) }
                s.executeQuery().use { rows ->
                    buildMap {
                        while (rows.next()) {
                            val preview = toPreview(rows)
                            put(preview.url, preview)
                        }
                    }
                }
            }
        }
    }

    override fun findImageById(id: Long): LinkPreviewImage? =
        database.connection().use { c ->
            c.prepareStatement(
                "SELECT image_bytes, image_content_type FROM link_previews WHERE id = ? AND status = 'ready' AND image_bytes IS NOT NULL",
            ).use { s ->
                s.setLong(1, id)
                s.executeQuery().use { rows ->
                    if (!rows.next()) return@use null
                    val bytes = rows.getBytes("image_bytes") ?: return@use null
                    val contentType = rows.getString("image_content_type") ?: return@use null
                    LinkPreviewImage(bytes, contentType)
                }
            }
        }

    override fun markPending(url: String): LinkPreview =
        database.transaction { c ->
            c.prepareStatement(
                "INSERT INTO link_previews (url, status) VALUES (?, 'pending') " +
                    "ON CONFLICT(url) DO UPDATE SET status = 'pending', retry_after = NULL " +
                    "WHERE link_previews.status = 'failed'",
            ).use { s ->
                s.setString(1, url)
                s.executeUpdate()
            }
            findByUrlOn(c, url)!!
        }

    override fun saveReady(
        url: String,
        title: String,
        siteName: String?,
        image: LinkPreviewImage?,
        fetchedAtMillis: Long,
    ): LinkPreview =
        database.transaction { c ->
            c.prepareStatement(
                "INSERT INTO link_previews (url, status, title, site_name, image_bytes, image_content_type, fetched_at, retry_after) " +
                    "VALUES (?, 'ready', ?, ?, ?, ?, ?, NULL) " +
                    "ON CONFLICT(url) DO UPDATE SET " +
                    "status = 'ready', title = excluded.title, site_name = excluded.site_name, " +
                    "image_bytes = excluded.image_bytes, image_content_type = excluded.image_content_type, " +
                    "fetched_at = excluded.fetched_at, retry_after = NULL",
            ).use { s ->
                s.setString(1, url)
                s.setString(2, title)
                if (siteName != null) s.setString(3, siteName) else s.setNull(3, Types.VARCHAR)
                if (image != null) {
                    s.setBytes(4, image.bytes)
                    s.setString(5, image.contentType)
                } else {
                    s.setNull(4, Types.BLOB)
                    s.setNull(5, Types.VARCHAR)
                }
                s.setLong(6, fetchedAtMillis)
                s.executeUpdate()
            }
            findByUrlOn(c, url)!!
        }

    override fun saveFailed(url: String, fetchedAtMillis: Long, retryAfterMillis: Long): LinkPreview =
        database.transaction { c ->
            c.prepareStatement(
                "INSERT INTO link_previews (url, status, title, site_name, image_bytes, image_content_type, fetched_at, retry_after) " +
                    "VALUES (?, 'failed', NULL, NULL, NULL, NULL, ?, ?) " +
                    "ON CONFLICT(url) DO UPDATE SET " +
                    "status = 'failed', title = NULL, site_name = NULL, image_bytes = NULL, image_content_type = NULL, " +
                    "fetched_at = excluded.fetched_at, retry_after = excluded.retry_after",
            ).use { s ->
                s.setString(1, url)
                s.setLong(2, fetchedAtMillis)
                s.setLong(3, retryAfterMillis)
                s.executeUpdate()
            }
            findByUrlOn(c, url)!!
        }

    override fun findUncachedWishlistUrls(): List<String> =
        database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery(
                    "SELECT DISTINCT l.url FROM wishlist_item_links l " +
                        "LEFT JOIN link_previews p ON p.url = l.url " +
                        "WHERE p.url IS NULL OR p.status = 'pending' " +
                        "ORDER BY l.url",
                ).use { rows ->
                    generateSequence { if (rows.next()) rows.getString(1) else null }.toList()
                }
            }
        }

    override fun deleteUnreferenced(): Int =
        database.transaction { c ->
            c.createStatement().use { s ->
                s.executeUpdate(
                    "DELETE FROM link_previews WHERE url NOT IN (SELECT DISTINCT url FROM wishlist_item_links)",
                )
            }
        }

    private fun findByUrlOn(c: Connection, url: String): LinkPreview? =
        c.prepareStatement(
            "SELECT id, url, status, title, site_name, image_bytes IS NOT NULL AS has_image, fetched_at, retry_after " +
                "FROM link_previews WHERE url = ?",
        ).use { s ->
            s.setString(1, url)
            s.executeQuery().use { rows -> if (rows.next()) toPreview(rows) else null }
        }

    private fun toPreview(rows: ResultSet): LinkPreview = LinkPreview(
        id = rows.getLong("id"),
        url = rows.getString("url"),
        status = when (val raw = rows.getString("status")) {
            "pending" -> LinkPreviewStatus.PENDING
            "ready" -> LinkPreviewStatus.READY
            "failed" -> LinkPreviewStatus.FAILED
            else -> error("Unknown link preview status: $raw")
        },
        title = rows.getString("title"),
        siteName = rows.getString("site_name"),
        hasImage = rows.getInt("has_image") != 0,
        fetchedAtMillis = rows.getObject("fetched_at")?.let { (it as Number).toLong() },
        retryAfterMillis = rows.getObject("retry_after")?.let { (it as Number).toLong() },
    )
}
