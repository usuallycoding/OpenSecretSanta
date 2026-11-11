package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class WishlistDtoSerializationTest {
    private val json = Json
    private val withDefaults = Json { encodeDefaults = true }

    private inline fun <reified T> roundTrip(value: T) {
        assertEquals(value, json.decodeFromString<T>(json.encodeToString(value)))
        assertEquals(value, json.decodeFromString<T>(withDefaults.encodeToString(value)))
    }

    private inline fun <reified T> keys(value: T): Set<String> =
        (withDefaults.encodeToJsonElement(kotlinx.serialization.serializer<T>(), value) as JsonObject).keys

    @Test
    fun everyDtoRoundTrips() {
        roundTrip(WishlistItemDto(7, "Wool socks", "Grey", "https://shop.example/socks", "2026-12-01T10:15:30Z", "2026-12-02T09:00:00Z"))
        roundTrip(WishlistItemDto(8, "Just a title", createdAt = "2026-12-01T10:15:30Z", updatedAt = "2026-12-01T10:15:30Z"))
        roundTrip(WishlistItemRequest("Wool socks", "Grey", "https://shop.example/socks"))
        roundTrip(WishlistItemRequest("Just a title"))
        roundTrip(WishlistItemRequest("Links", urls = listOf("https://a.example", "https://b.example")))
        roundTrip(WishlistItemRequest("No links", urls = emptyList()))
        roundTrip(WishlistItemDto(9, "Links", urls = listOf("https://a.example", "https://b.example"), createdAt = "c", updatedAt = "u"))
        roundTrip(SharedWishlistItemDto("Links", urls = listOf("https://a.example"), createdAt = "c", updatedAt = "u"))
        roundTrip(ReorderWishlistRequest(listOf(3, 1, 2)))
        roundTrip(ReorderWishlistRequest(emptyList()))
        roundTrip(SharedWishlistItemDto("Wool socks", "Grey", "https://shop.example/socks", "2026-12-01T10:15:30Z", "2026-12-02T09:00:00Z"))
    }

    @Test
    fun requestsTolerateMissingOptionalFields() {
        assertEquals(WishlistItemRequest("Socks"), json.decodeFromString<WishlistItemRequest>("""{"title":"Socks"}"""))
    }

    @Test
    fun anOlderAppsRequestHasNoUrlsAndAnEmptyListIsNotAbsent() {
        assertNull(json.decodeFromString<WishlistItemRequest>("""{"title":"Socks","url":"https://a.example"}""").urls)
        assertEquals(emptyList(), json.decodeFromString<WishlistItemRequest>("""{"title":"Socks","urls":[]}""").urls)
    }

    @Test
    fun anItemWithoutUrlsReadsAsHavingNone() {
        val item = json.decodeFromString<WishlistItemDto>("""{"id":1,"title":"t","url":"https://a.example","createdAt":"c","updatedAt":"u"}""")
        assertEquals(emptyList(), item.urls)
    }

    @Test
    fun theOwnersItemHasAnIdAndTheSantasItemDoesNot() {
        assertEquals(setOf("id", "title", "note", "url", "createdAt", "updatedAt", "urls"), keys(WishlistItemDto(1, "t", createdAt = "c", updatedAt = "u")))
        assertEquals(setOf("title", "note", "url", "createdAt", "updatedAt", "urls"), keys(SharedWishlistItemDto("t", createdAt = "c", updatedAt = "u")))
    }

    @Test
    fun theSantasItemCarriesNothingAboutReadsOrReaders() {
        val keys = keys(SharedWishlistItemDto("t", "n", "https://shop.example", "c", "u"))

        for (forbidden in listOf("read", "viewed", "seen", "reader", "santa", "owner", "user")) {
            assertFalse(keys.any { it.contains(forbidden, ignoreCase = true) }, "a key mentions '$forbidden': $keys")
        }
    }

    @Test
    fun routeBuildersMatchTheirTemplates() {
        assertEquals("/groups/5/wishlist", ApiRoutes.groupWishlist(5))
        assertEquals("/groups/5/wishlist/items", ApiRoutes.groupWishlistItems(5))
        assertEquals("/groups/5/wishlist/items/9", ApiRoutes.groupWishlistItem(5, 9))
        assertEquals("/groups/5/wishlist/order", ApiRoutes.groupWishlistOrder(5))
        assertEquals("/groups/5/receiver/wishlist", ApiRoutes.groupReceiverWishlist(5))
        assertEquals(ApiRoutes.GROUP_WISHLIST_ITEM.replace("{id}", "5").replace("{itemId}", "9"), ApiRoutes.groupWishlistItem(5, 9))
    }
}
