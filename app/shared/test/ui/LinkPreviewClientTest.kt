package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.LinkPreviewDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.AuthContext
import com.usuallycoding.opensecretsanta.data.api.BoundedImageCache
import com.usuallycoding.opensecretsanta.data.api.createHttpClient
import com.usuallycoding.opensecretsanta.ui.wishlist.PendingPreviewRefresher
import com.usuallycoding.opensecretsanta.ui.wishlist.hostOf
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LinkPreviewClientTest {

    @Test
    fun apiClientFetchesAuthenticatedLinkPreviewImageBytes() = runTest {
        var authHeader: String? = null
        val engine = MockEngine { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            when (request.url.encodedPath) {
                "/link-previews/7/image" -> respond(
                    content = byteArrayOf(10, 20, 30),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "image/jpeg"),
                )
                else -> respond(
                    content = """{"code":"not_found","message":"missing"}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val auth = object : AuthContext {
            override val token: String = "tok-123"
            override fun rejected(error: ApiError) = Unit
        }
        val api = ApiClient(createHttpClient(engine), "https://santa.example", auth)

        val ok = api.linkPreviewImage("/link-previews/7/image")
        assertEquals("Bearer tok-123", authHeader)
        assertContentEquals(byteArrayOf(10, 20, 30), (ok as ApiResult.Success).value)

        val missing = api.linkPreviewImage("/link-previews/99/image")
        assertEquals(404, ((missing as ApiResult.Failure).error as ApiError.Rejected).status)
    }

    @Test
    fun imageCacheCachesDecodesAndEvictsLruEntries() = runTest {
        var fetches = 0
        val cache = BoundedImageCache(
            fetchBytes = { path ->
                fetches++
                if (path == "/fail") ApiResult.Failure(ApiError.Network) else ApiResult.Success(path.encodeToByteArray())
            },
            decode = { bytes ->
                val text = bytes.decodeToString()
                if (text == "/corrupt") null else "decoded:$text"
            },
            maxEntries = 2,
        )

        val first = cache.load("/a")
        assertNotNull(first)
        assertEquals(1, fetches)

        // Cache hit does not refetch.
        assertEquals(first, cache.load("/a"))
        assertEquals(1, fetches)

        // Load /b and /c -> /a is evicted (LRU size 2).
        assertNotNull(cache.load("/b"))
        assertNotNull(cache.load("/c"))
        assertEquals(3, fetches)
        assertNull(cache.get("/a"))
        assertNotNull(cache.get("/b"))
        assertNotNull(cache.get("/c"))

        // Failures and undecodable bytes return null.
        assertNull(cache.load("/fail"))
        assertNull(cache.load("/corrupt"))
    }

    @Test
    fun pendingPreviewRefresherStopsWhenReadyAndCapsRetriesWhenStillPending() = runTest {
        var calls = 0
        val refresher = PendingPreviewRefresher(
            scope = this,
            refresh = {
                calls++
                calls < 2 // becomes ready on 2nd check
            },
            delaysMillis = listOf(100L, 200L, 300L),
        )
        refresher.sync(hasPending = true)
        advanceUntilIdle()
        assertEquals(2, calls)

        var boundedCalls = 0
        val bounded = PendingPreviewRefresher(
            scope = this,
            refresh = {
                boundedCalls++
                true // stays pending
            },
            delaysMillis = listOf(100L, 200L, 300L),
        )
        bounded.sync(hasPending = true)
        advanceUntilIdle()
        assertEquals(3, boundedCalls)
    }

    @Test
    fun hostOfExtractsNormalizedHost() {
        assertEquals("shop.example", hostOf("https://www.shop.example/item?id=1"))
        assertEquals("cozy.example", hostOf("http://cozy.example:8080/socks"))
        assertNull(hostOf("not-a-url"))
        assertEquals(LinkPreviewDto.STATUS_READY, LinkPreviewDto(LinkPreviewDto.STATUS_READY, "Socks").status)
    }
}
