package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The wishlist calls against a server that behaves like the real one (see [FakeWishlist]). */
class WishlistApiTest {
    private val server = ScriptedServer().also { it.session() }
    private val api get() = server.api

    private fun success(result: ApiResult<*>) = (result as ApiResult.Success).value

    @Test
    fun theListComesBackInTheOrderThePersonChoseWithIds() = runTest {
        val first = server.wishlist.add("Socks", note = "Grey", url = "https://shop.example/socks")
        val second = server.wishlist.add("Tea")

        val items = success(api.wishlist(1)) as List<*>

        assertEquals(listOf(first, second), server.wishlist.ids)
        assertEquals(2, items.size)
        assertEquals(listOf("GET /groups/1/wishlist"), server.calls)
    }

    @Test
    fun anAddedItemIsCreatedAtTheEndWithTheRequestAsBody() = runTest {
        server.wishlist.add("Socks")

        val created = success(api.addWishlistItem(1, WishlistItemRequest("Tea", "Loose leaf", "https://tea.example")))

        assertEquals(listOf("Socks", "Tea"), server.wishlist.titles)
        assertEquals("Tea", (created as com.usuallycoding.opensecretsanta.contract.WishlistItemDto).title)
        assertEquals("POST /groups/1/wishlist/items", server.calls.last())
        assertEquals(WishlistItemRequest("Tea", "Loose leaf", "https://tea.example"), Json.decodeFromString(server.bodies.last()))
    }

    @Test
    fun anEditKeepsTheItemsPlace() = runTest {
        server.wishlist.add("Socks")
        val tea = server.wishlist.add("Tea")
        server.wishlist.add("Book")

        api.updateWishlistItem(1, tea, WishlistItemRequest("Green tea"))

        assertEquals(listOf("Socks", "Green tea", "Book"), server.wishlist.titles)
        assertEquals("PUT /groups/1/wishlist/items/$tea", server.calls.last())
    }

    @Test
    fun aDeleteAnswersNoContentAndParsesAsSuccess() = runTest {
        val socks = server.wishlist.add("Socks")
        server.wishlist.add("Tea")

        val result = api.deleteWishlistItem(1, socks)

        assertEquals(ApiResult.Success(Unit), result)
        assertEquals(listOf("Tea"), server.wishlist.titles)
        assertEquals("DELETE /groups/1/wishlist/items/$socks", server.calls.last())
    }

    @Test
    fun aReorderSendsEveryIdInTheNewOrderAndReturnsThatOrder() = runTest {
        val a = server.wishlist.add("A")
        val b = server.wishlist.add("B")
        val c = server.wishlist.add("C")

        val returned = success(api.reorderWishlist(1, listOf(c, a, b))) as List<*>

        assertEquals(listOf("C", "A", "B"), server.wishlist.titles)
        assertEquals(3, returned.size)
        assertEquals(ReorderWishlistRequest(listOf(c, a, b)), Json.decodeFromString(server.bodies.last()))
        assertEquals("PUT /groups/1/wishlist/order", server.calls.last())
    }

    @Test
    fun aValidationErrorNamesItsField() = runTest {
        val result = api.addWishlistItem(1, WishlistItemRequest("   "))

        assertEquals(ApiResult.Failure(ApiError.Validation("title", "Title must be 1 to 200 characters long.")), result)
    }

    @Test
    fun aStaleReorderIsRefusedNamingItemIds() = runTest {
        val a = server.wishlist.add("A")
        val b = server.wishlist.add("B")
        server.wishlist.add("C added on another device")

        val result = api.reorderWishlist(1, listOf(b, a))

        assertEquals(ApiResult.Failure(ApiError.Validation("itemIds", "The ids must be exactly your current items.")), result)
        assertEquals(listOf("A", "B", "C added on another device"), server.wishlist.titles)
    }

    @Test
    fun anItemThatIsGoneIsNotFound() = runTest {
        val gone = server.wishlist.add("Socks")
        server.wishlist.removeBehindTheScenes(gone)

        assertEquals(ApiResult.Failure(ApiError.Rejected(404, "not_found", "Item not found.")), api.updateWishlistItem(1, gone, WishlistItemRequest("Socks")))
        assertEquals(ApiResult.Failure(ApiError.Rejected(404, "not_found", "Item not found.")), api.deleteWishlistItem(1, gone))
    }

    @Test
    fun aListThatCannotBeChangedAnswers409() = runTest {
        server.wishlist.add("Socks")
        server.wishlist.editable = false

        val result = api.addWishlistItem(1, WishlistItemRequest("Tea"))

        assertEquals(ApiResult.Failure(ApiError.Rejected(409, "invalid_state", "The wishlist can no longer be changed.")), result)
        assertEquals(listOf("Socks"), server.wishlist.titles)
        assertEquals(listOf("Socks"), (success(api.wishlist(1)) as List<*>).filterIsInstance<WishlistItemDto>().map { it.title }) // still readable
    }

    @Test
    fun aFullListAnswers409() = runTest {
        repeat(50) { server.wishlist.add("Item $it") }

        val result = api.addWishlistItem(1, WishlistItemRequest("One too many"))

        assertEquals(ApiResult.Failure(ApiError.Rejected(409, "invalid_state", "The wishlist is full.")), result)
        assertEquals(50, server.wishlist.ids.size)
    }

    @Test
    fun aQueuedFailureAnswersTheNextChangeOnly() = runTest {
        server.wishlist.failNextChangeWith(HttpStatusCode.Forbidden, "forbidden", "nope")

        assertEquals(ApiResult.Failure(ApiError.Rejected(403, "forbidden", "nope")), api.addWishlistItem(1, WishlistItemRequest("Tea")))
        assertTrue(api.addWishlistItem(1, WishlistItemRequest("Tea")) is ApiResult.Success)
    }

    @Test
    fun theReceiversListComesWithoutIdsInTheReceiversOrder() = runTest {
        server.receiverWishlist = listOf(
            SharedWishlistItemDto("Gloves", "Black", "https://shop.example/gloves", "2026-12-01T10:00:00Z", "2026-12-02T10:00:00Z"),
            SharedWishlistItemDto("Book", null, null, "2026-12-01T11:00:00Z", "2026-12-01T11:00:00Z"),
        )

        val items = (success(api.receiverWishlist(1)) as List<*>).filterIsInstance<SharedWishlistItemDto>()

        assertEquals(listOf("Gloves", "Book"), items.map { it.title })
        assertEquals("GET /groups/1/receiver/wishlist", server.calls.last())
    }

    @Test
    fun noPasswordTravelsWithAnyWishlistCall() = runTest {
        val id = server.wishlist.add("Socks")
        api.wishlist(1); api.addWishlistItem(1, WishlistItemRequest("Tea")); api.updateWishlistItem(1, id, WishlistItemRequest("Socks 2"))
        api.reorderWishlist(1, server.wishlist.ids); api.deleteWishlistItem(1, id); api.receiverWishlist(1)

        assertFalse(server.bodies.any { "password" in it.lowercase() })
    }
}
