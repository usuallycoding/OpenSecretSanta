package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.LinkPreviewDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewStatus
import com.usuallycoding.opensecretsanta.server.data.group.SqliteLinkPreviewRepository
import com.usuallycoding.opensecretsanta.server.data.preview.HttpTransport
import com.usuallycoding.opensecretsanta.server.data.preview.LinkPreviewService
import com.usuallycoding.opensecretsanta.server.data.preview.RawHttpResponse
import com.usuallycoding.opensecretsanta.server.data.preview.SafeHttpFetcher
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.concurrent.Executor
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkPreviewServiceAndRoutesTest {

    private class FakeClock(var now: Long = 10_000L) : Clock {
        override fun nowMillis(): Long = now
    }

    private class ManualExecutor : Executor {
        val pending = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) {
            pending.addLast(command)
        }
        fun drainAll() {
            while (pending.isNotEmpty()) {
                pending.removeFirst().run()
            }
        }
    }

    private fun samplePngBytes(): ByteArray {
        val img = BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply {
                color = Color.BLUE
                fillRect(0, 0, 40, 40)
                dispose()
            }
        }
        return ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
    }

    private fun fakeFetcher(
        onHtml: (String) -> RawHttpResponse,
        onImage: (String) -> RawHttpResponse = { RawHttpResponse(404, null, null, null, ByteArrayInputStream(ByteArray(0))) },
    ): SafeHttpFetcher {
        val dns: (String) -> List<InetAddress> = { listOf(InetAddress.getByName("93.184.216.34")) }
        val transport = HttpTransport { uri, accept, _, _ ->
            if (accept.startsWith("image/")) onImage(uri.toString()) else onHtml(uri.toString())
        }
        return SafeHttpFetcher(transport = transport, resolveHost = dns)
    }

    @Test
    fun serviceDeduplicatesInFlightRespectsCooldownAndBackfills() {
        val fx = TestFixtures()
        fx.addUser("admin", admin = true)
        fx.addUser("alice")
        val groupId = fx.addGroup("admin", "alice", state = "active")
        fx.database.connection().use { c ->
            c.createStatement().use { s ->
                s.execute("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES ($groupId, 'alice', 0, 'Gift', 100, 100)")
                s.execute("INSERT INTO wishlist_item_links (item_id, position, url) VALUES (1, 0, 'https://shop.example/existing')")
            }
        }

        val clock = FakeClock(1_000L)
        val executor = ManualExecutor()
        var htmlCalls = 0
        var shouldFail = true
        val fetcher = fakeFetcher(
            onHtml = { url ->
                htmlCalls++
                if (shouldFail) {
                    RawHttpResponse(403, null, "text/html", 0, ByteArrayInputStream(ByteArray(0)))
                } else {
                    val body = "<html><head><meta property='og:title' content='Nice Gift $url'></head></html>".toByteArray()
                    RawHttpResponse(200, null, "text/html; charset=utf-8", body.size.toLong(), ByteArrayInputStream(body))
                }
            },
        )
        val repo = SqliteLinkPreviewRepository(fx.database)
        val service = LinkPreviewService(repo, fetcher, clock, executor, failureCooldownMillis = 5_000L)

        // Startup backfill queues existing wishlist_item_links without blocking.
        service.startBackfill()
        assertEquals(1, executor.pending.size)
        executor.drainAll()
        assertEquals(1, htmlCalls)
        assertEquals(LinkPreviewStatus.FAILED, repo.findByUrls(listOf("https://shop.example/existing"))["https://shop.example/existing"]?.status)

        // During cooldown, lookupAndEnqueue does not re-fetch.
        clock.now = 4_000L
        assertEquals(emptyMap(), service.lookupAndEnqueue(listOf("https://shop.example/existing")))
        executor.drainAll()
        assertEquals(1, htmlCalls)

        // After cooldown expires, lookupAndEnqueue queues again and deduplicates concurrent calls.
        shouldFail = false
        clock.now = 7_000L
        val first = service.lookupAndEnqueue(listOf("https://shop.example/existing"))
        val second = service.lookupAndEnqueue(listOf("https://shop.example/existing"))
        assertEquals(LinkPreviewStatus.PENDING, first["https://shop.example/existing"]?.status)
        assertEquals(LinkPreviewStatus.PENDING, second["https://shop.example/existing"]?.status)
        assertEquals(1, executor.pending.size)

        executor.drainAll()
        assertEquals(2, htmlCalls)
        val ready = service.lookupAndEnqueue(listOf("https://shop.example/existing"))["https://shop.example/existing"]
        assertEquals(LinkPreviewStatus.READY, ready?.status)
        assertEquals("Nice Gift https://shop.example/existing", ready?.title)
    }

    @Test
    fun wishlistAndThumbnailRoutesServePendingReadyFailedAndEnforceAuth() = testApplication {
        val fx = TestFixtures()
        fx.addUser("admin", admin = true)
        fx.addUser("alice")
        fx.addUser("bob")
        fx.addUser("tempuser", mustChange = true)
        val groupId = fx.addGroup("admin", "alice", "bob", state = "active")
        fx.assign(groupId, "alice" to "bob", "bob" to "alice")

        val executor = ManualExecutor()
        val png = samplePngBytes()
        val fetcher = fakeFetcher(
            onHtml = { url ->
                when (url) {
                    "https://shop.example/socks" -> {
                        val body = """
                            <html><head>
                              <meta property="og:title" content="Warm Wool Socks">
                              <meta property="og:site_name" content="Cozy Shop">
                              <meta property="og:image" content="https://shop.example/socks.png">
                            </head></html>
                        """.trimIndent().toByteArray()
                        RawHttpResponse(200, null, "text/html; charset=utf-8", body.size.toLong(), ByteArrayInputStream(body))
                    }
                    else -> RawHttpResponse(403, null, "text/html", 0, ByteArrayInputStream(ByteArray(0)))
                }
            },
            onImage = { RawHttpResponse(200, null, "image/png", png.size.toLong(), ByteArrayInputStream(png)) },
        )

        application {
            module(fx.config, linkPreviewFetcher = fetcher, linkPreviewExecutor = executor)
        }

        // Non-blocking save: returns immediately with pending previews before executor drains.
        val postResp = client.post(ApiRoutes.groupWishlistItems(groupId.toLong())) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("alice")}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(WishlistItemRequest("Socks", urls = listOf("https://shop.example/socks", "https://blocked.example/item"))))
        }
        assertEquals(HttpStatusCode.Created, postResp.status)
        val created = Json.decodeFromString<WishlistItemDto>(postResp.bodyAsText())
        assertEquals(listOf("https://shop.example/socks", "https://blocked.example/item"), created.urls)
        assertEquals("https://shop.example/socks", created.url)
        assertEquals(LinkPreviewDto.STATUS_PENDING, created.linkPreviews["https://shop.example/socks"]?.status)
        assertEquals(LinkPreviewDto.STATUS_PENDING, created.linkPreviews["https://blocked.example/item"]?.status)

        // Drain background worker.
        executor.drainAll()

        // Own wishlist now returns ready preview for socks and omits failed blocked link.
        val ownResp = client.get(ApiRoutes.groupWishlist(groupId.toLong())) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("alice")}")
        }
        assertEquals(HttpStatusCode.OK, ownResp.status)
        val ownItems = Json.decodeFromString<List<WishlistItemDto>>(ownResp.bodyAsText())
        val preview = ownItems.single().linkPreviews["https://shop.example/socks"]
        assertNotNull(preview)
        assertEquals(LinkPreviewDto.STATUS_READY, preview.status)
        assertEquals("Warm Wool Socks", preview.title)
        assertEquals("Cozy Shop", preview.siteName)
        assertNotNull(preview.imagePath)
        assertNull(ownItems.single().linkPreviews["https://blocked.example/item"])

        // Santa's receiver wishlist and admin oversight wishlist include the same preview.
        val santaResp = client.get(ApiRoutes.groupReceiverWishlist(groupId.toLong())) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("bob")}")
        }
        val santaItems = Json.decodeFromString<List<SharedWishlistItemDto>>(santaResp.bodyAsText())
        assertEquals(preview, santaItems.single().linkPreviews["https://shop.example/socks"])

        val adminResp = client.get(ApiRoutes.adminGroupParticipantWishlist(groupId.toLong(), "alice")) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("admin")}")
        }
        val adminItems = Json.decodeFromString<List<SharedWishlistItemDto>>(adminResp.bodyAsText())
        assertEquals(preview, adminItems.single().linkPreviews["https://shop.example/socks"])

        // Thumbnail endpoint serves image to authenticated user, refuses unauthenticated and temp-password users, 404s on missing.
        val imgPath = preview.imagePath!!
        assertEquals(HttpStatusCode.Unauthorized, client.get(imgPath).status)

        val tempResp = client.get(imgPath) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("tempuser")}")
        }
        assertEquals(HttpStatusCode.Forbidden, tempResp.status)

        val okImg = client.get(imgPath) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("bob")}")
        }
        assertEquals(HttpStatusCode.OK, okImg.status)
        assertTrue(okImg.headers[HttpHeaders.CacheControl]?.contains("max-age=") == true)
        assertTrue(okImg.bodyAsBytes().isNotEmpty())

        val missingImg = client.get(ApiRoutes.linkPreviewImage(99999L)) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("bob")}")
        }
        assertEquals(HttpStatusCode.NotFound, missingImg.status)
    }
}
