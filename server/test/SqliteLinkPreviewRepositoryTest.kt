package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewImage
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewStatus
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteLinkPreviewRepository
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqliteLinkPreviewRepositoryTest {

    private fun migratedDatabase(): Database {
        val database = Database(newTempDbPath().toString())
        MigrationRunner(database, ClasspathMigrations.load()).migrate()
        return database
    }

    @Test
    fun upgradeFromV018PreservesExistingLinksAndExposesThemForBackfill() {
        val database = Database(newTempDbPath().toString())
        val all = ClasspathMigrations.load()
        MigrationRunner(database, all.filter { it.version < 19 }).migrate()
        database.connection().use { c ->
            c.createStatement().use { s ->
                s.execute("INSERT INTO users (username, password_hash) VALUES ('boss', 'h'), ('alice', 'h')")
                s.execute("INSERT INTO groups (owner_username, name, state) VALUES ('boss', 'Party', 'active')")
                s.execute("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES (1, 'alice', 0, 'Socks', 100, 100)")
                s.execute("INSERT INTO wishlist_item_links (item_id, position, url) VALUES (1, 0, 'https://shop.example/socks'), (1, 1, 'https://shop.example/hat')")
            }
        }

        MigrationRunner(database, all).migrate()

        val repo = SqliteLinkPreviewRepository(database)
        assertEquals(listOf("https://shop.example/hat", "https://shop.example/socks"), repo.findUncachedWishlistUrls())
    }

    @Test
    fun markPendingSaveReadyAndFetchImageRoundTrip() {
        val repo = SqliteLinkPreviewRepository(migratedDatabase())
        val url = "https://shop.example/socks"

        val pending = repo.markPending(url)
        assertEquals(LinkPreviewStatus.PENDING, pending.status)
        assertNull(pending.title)
        assertFalse(pending.hasImage)
        assertNull(repo.findImageById(pending.id))

        val imageBytes = byteArrayOf(1, 2, 3, 4)
        val ready = repo.saveReady(url, "Wool Socks", "Cozy Shop", LinkPreviewImage(imageBytes, "image/jpeg"), fetchedAtMillis = 1000L)
        assertEquals(pending.id, ready.id)
        assertEquals(LinkPreviewStatus.READY, ready.status)
        assertEquals("Wool Socks", ready.title)
        assertEquals("Cozy Shop", ready.siteName)
        assertTrue(ready.hasImage)
        assertEquals(1000L, ready.fetchedAtMillis)
        assertNull(ready.retryAfterMillis)

        val fetchedImage = repo.findImageById(ready.id)
        assertNotNull(fetchedImage)
        assertEquals("image/jpeg", fetchedImage.contentType)
        assertContentEquals(imageBytes, fetchedImage.bytes)

        // Marking pending on an already-ready row does not downgrade it.
        val stillReady = repo.markPending(url)
        assertEquals(LinkPreviewStatus.READY, stillReady.status)
    }

    @Test
    fun saveFailedClearsMetadataAndCanReenterPending() {
        val repo = SqliteLinkPreviewRepository(migratedDatabase())
        val url = "https://blocked.example/item"

        val failed = repo.saveFailed(url, fetchedAtMillis = 1000L, retryAfterMillis = 5000L)
        assertEquals(LinkPreviewStatus.FAILED, failed.status)
        assertEquals(5000L, failed.retryAfterMillis)
        assertFalse(failed.hasImage)

        val retried = repo.markPending(url)
        assertEquals(LinkPreviewStatus.PENDING, retried.status)
        assertNull(retried.retryAfterMillis)
    }

    @Test
    fun batchLookupAndDeleteUnreferencedWork() {
        val database = migratedDatabase()
        database.connection().use { c ->
            c.createStatement().use { s ->
                s.execute("INSERT INTO users (username, password_hash) VALUES ('boss', 'h'), ('alice', 'h')")
                s.execute("INSERT INTO groups (owner_username, name, state) VALUES ('boss', 'Party', 'active')")
                s.execute("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES (1, 'alice', 0, 'Socks', 100, 100)")
                s.execute("INSERT INTO wishlist_item_links (item_id, position, url) VALUES (1, 0, 'https://kept.example/a')")
            }
        }

        val repo = SqliteLinkPreviewRepository(database)
        repo.saveReady("https://kept.example/a", "Kept", "kept.example", null, 1000L)
        repo.saveReady("https://orphan.example/b", "Orphan", "orphan.example", null, 1000L)

        val found = repo.findByUrls(listOf("https://kept.example/a", "https://orphan.example/b", "https://missing.example/c"))
        assertEquals(setOf("https://kept.example/a", "https://orphan.example/b"), found.keys)

        val removed = repo.deleteUnreferenced()
        assertEquals(1, removed)
        assertEquals(setOf("https://kept.example/a"), repo.findByUrls(listOf("https://kept.example/a", "https://orphan.example/b")).keys)
    }
}
