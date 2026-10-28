package com.usuallycoding.opensecretsanta.ui

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

    private fun item(title: String, note: String? = null, url: String? = null) =
        SharedWishlistItemDto(title, note, url, "2026-12-01T10:00:00Z", "2026-12-01T10:00:00Z")

    /** The first load runs on the client's own threads, so wait for it before looking at the state. */
    private suspend fun viewModel() = ReceiverWishlistViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    @Test
    fun theReceiversItemsAreShownInTheReceiversOrder() = runTest {
        server.receiverWishlist = listOf(item("Gloves", "Black", "https://shop.example/gloves"), item("Book"), item("Tea", "Loose"))

        val items = viewModel().state.value.value!!

        assertEquals(
            listOf(
                SharedWishItem("Gloves", "Black", "https://shop.example/gloves", "1 Dec 2026, 12:00", null),
                SharedWishItem("Book", null, null, "1 Dec 2026, 12:00", null),
                SharedWishItem("Tea", "Loose", null, "1 Dec 2026, 12:00", null),
            ),
            items,
        )
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

        assertTrue(server.calls.isNotEmpty() && server.calls.all { it == "GET /groups/1/receiver/wishlist" }, server.calls.toString())
    }
}
