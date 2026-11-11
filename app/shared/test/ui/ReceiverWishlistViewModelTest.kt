package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.ReadMarkDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.SharedWishItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReceiverWishlistViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private fun item(title: String, note: String? = null, vararg urls: String) =
        SharedWishlistItemDto(title, note, urls.firstOrNull(), "2026-12-01T10:00:00Z", "2026-12-01T10:00:00Z", urls.toList())

    /** The first load runs on the client's own threads, so wait for it before looking at the state. */
    private suspend fun viewModel() = ReceiverWishlistViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    @Test
    fun theReceiversItemsAreShownInTheReceiversOrder() = runTest {
        server.receiverWishlist = listOf(item("Gloves", "Black", "https://shop.example/gloves"), item("Book"), item("Tea", "Loose"))
        // the Santa has already seen all of it, so nothing is marked and the list is just the list
        server.readMarks.wishlist = ReadMarkDto(0, "2026-12-31T00:00:00Z")

        val items = viewModel().state.value.value!!

        assertEquals(
            listOf(
                SharedWishItem("Gloves", "Black", listOf("https://shop.example/gloves"), "1 Dec 2026, 12:00", null),
                SharedWishItem("Book", null, emptyList(), "1 Dec 2026, 12:00", null),
                SharedWishItem("Tea", "Loose", emptyList(), "1 Dec 2026, 12:00", null),
            ),
            items,
        )
    }

    @Test
    fun everyLinkOfAnItemIsShownInOrder() = runTest {
        server.receiverWishlist = listOf(item("Gloves", null, "https://a.example", "https://b.example", "https://c.example"))
        server.readMarks.wishlist = ReadMarkDto(0, "2026-12-31T00:00:00Z")

        val items = viewModel().state.value.value!!

        assertEquals(listOf("https://a.example", "https://b.example", "https://c.example"), items.single().urls)
    }

    @Test
    fun aReceiverWhoAddedNothingGivesAnEmptyListNotAnError() = runTest {
        val state = viewModel().state.value

        assertEquals(emptyList(), state.value)
        assertNull(state.error)
    }

    @Test
    fun anUnreachableServerCanBeRetried() = runTest {
        server.receiverWishlist = listOf(item("Gloves"))
        server.unreachable = true
        val model = viewModel()
        assertEquals(ApiError.Network, model.state.value.error)

        server.unreachable = false
        model.refresh().join()

        assertEquals(listOf("Gloves"), model.state.value.value!!.map { it.title })
    }

    @Test
    fun aRefreshShowsWhatTheReceiverChangedMeanwhile() = runTest {
        server.receiverWishlist = listOf(item("Gloves"))
        val model = viewModel()

        server.receiverWishlist = listOf(item("Book"), item("Gloves"))
        model.refresh().join()

        assertEquals(listOf("Book", "Gloves"), model.state.value.value!!.map { it.title })
    }

    @Test
    fun onlyEverReadsAndNeverChangesAnything() = runTest {
        server.receiverWishlist = listOf(item("Gloves"))
        val model = viewModel()
        model.refresh().join()

        // the list, and what the Santa had seen of it; nothing is written unless the screen says the list was shown
        val allowed = setOf("GET /groups/1/receiver/wishlist", "GET /groups/1/receiver/reads")
        assertTrue(server.calls.isNotEmpty() && server.calls.all { it in allowed }, server.calls.toString())
    }
}
