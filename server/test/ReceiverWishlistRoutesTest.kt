package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ReceiverWishlistRoutesTest {
    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp() = application { module(fx.config) }

    private suspend inline fun <reified T> HttpClient.send(method: String, path: String, token: String?, body: T? = null): HttpResponse =
        request(path) {
            this.method = HttpMethod.parse(method)
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(body))
            }
        }

    private suspend fun HttpClient.getAs(path: String, token: String?) = send<Unit>("GET", path, token)

    private suspend fun HttpClient.wish(group: Int, user: String, title: String, note: String? = null, url: String? = null): WishlistItemDto {
        val response = send("POST", ApiRoutes.groupWishlistItems(group.toLong()), fx.token(user), WishlistItemRequest(title, note, url))
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return Json.decodeFromString(response.bodyAsText())
    }

    private suspend fun HttpClient.santaReads(group: Int, santa: String): List<SharedWishlistItemDto> =
        Json.decodeFromString(getAs(ApiRoutes.groupReceiverWishlist(group.toLong()), fx.token(santa)).bodyAsText())

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    private val names = listOf("alice", "bob", "carol", "dave")

    /** alice -> bob -> carol -> dave -> alice. */
    private fun world(state: String = "active", name: String = "Party"): Int {
        fx.addUser("admin-a", admin = true)
        for (n in names) fx.addUser(n)
        return group(state, name)
    }

    private fun group(state: String = "active", name: String = "Another", pairs: List<Pair<String, String>>? = null): Int {
        val id = fx.addGroup("admin-a", *names.toTypedArray(), state = state, name = name)
        if (state == "active" || state == "archived") {
            val drawn = pairs ?: listOf("alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
            fx.assign(id, *drawn.toTypedArray())
        }
        return id
    }

    @Test
    fun aSantaReadsTheirReceiversListInOrderWithoutIds() = testApplication {
        startApp()
        val id = world()
        client.wish(id, "bob", "First wish", "Grey, size M", "https://shop.example/socks")
        client.wish(id, "bob", "Second wish")

        val response = client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), fx.token("alice"))

        assertEquals(HttpStatusCode.OK, response.status)
        val items = Json.decodeFromString<List<SharedWishlistItemDto>>(response.bodyAsText())
        assertEquals(listOf("First wish", "Second wish"), items.map { it.title })
        assertEquals("Grey, size M", items.first().note)
        assertEquals("https://shop.example/socks", items.first().url)
        for (element in Json.parseToJsonElement(response.bodyAsText()).jsonArray) {
            assertEquals(setOf("title", "note", "url", "createdAt", "updatedAt"), element.jsonObject.keys)
        }
    }

    @Test
    fun everyoneReadsOnlyTheirOwnReceiversList() = testApplication {
        startApp()
        val id = world()
        for (name in names) client.wish(id, name, "Wish of $name")

        val seen = names.map { client.santaReads(id, it).single().title }

        assertEquals(listOf("Wish of bob", "Wish of carol", "Wish of dave", "Wish of alice"), seen)
    }

    @Test
    fun changesAndReordersAreSeenStraightAway() = testApplication {
        startApp()
        val id = world()
        val a = client.wish(id, "bob", "A")
        val b = client.wish(id, "bob", "B")
        assertEquals(listOf("A", "B"), client.santaReads(id, "alice").map { it.title })

        client.send("PUT", ApiRoutes.groupWishlistOrder(id.toLong()), fx.token("bob"), com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest(listOf(b.id, a.id)))
        client.send("PUT", ApiRoutes.groupWishlistItem(id.toLong(), a.id), fx.token("bob"), WishlistItemRequest("A, edited"))

        assertEquals(listOf("B", "A, edited"), client.santaReads(id, "alice").map { it.title })
    }

    @Test
    fun anEmptyListIsAnEmptyArray() = testApplication {
        startApp()
        val id = world()

        val response = client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), fx.token("alice"))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun aReceiverWithDifferentListsInTwoGroupsIsReadSeparatelyByEachGroupsSanta() = testApplication {
        startApp()
        val first = world(name = "First") // alice gives to bob
        val second = group("active", "Second", pairs = listOf("carol" to "bob", "bob" to "alice", "alice" to "dave", "dave" to "carol"))
        client.wish(first, "bob", "Wish in the first group")
        client.wish(second, "bob", "Wish in the second group")

        assertEquals(listOf("Wish in the first group"), client.santaReads(first, "alice").map { it.title })
        assertEquals(listOf("Wish in the second group"), client.santaReads(second, "carol").map { it.title })
    }

    @Test
    fun theListIsStillReadableAfterArchive() = testApplication {
        startApp()
        val id = world()
        client.wish(id, "bob", "Kept")
        fx.sql("UPDATE groups SET state = 'archived' WHERE id = $id")

        assertEquals(listOf("Kept"), client.santaReads(id, "alice").map { it.title })
    }

    @Test
    fun thereIsNoReceiverBeforeActivation() = testApplication {
        startApp()
        val id = world("pending")
        client.wish(id, "bob", "Prepared early")

        val response = client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), fx.token("alice"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("invalid_state", error(response.bodyAsText()).code)
    }

    @Test
    fun strangersDraftsTheNonParticipatingOwnerAndUnknownGroupsAreNotFound() = testApplication {
        startApp()
        val id = world()
        val draft = group("draft", "Draft")
        fx.addUser("outsider")

        val attempts = listOf(
            client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), fx.token("outsider")),
            client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), fx.token("admin-a")),
            client.getAs(ApiRoutes.groupReceiverWishlist(draft.toLong()), fx.token("alice")),
            client.getAs(ApiRoutes.groupReceiverWishlist(9999), fx.token("alice")),
            client.getAs("/groups/nope/receiver/wishlist", fx.token("alice")),
        )

        for (response in attempts) assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun authenticationAndThePasswordChangeGateApply() = testApplication {
        startApp()
        val id = world()
        fx.addUser("newbie", mustChange = true)

        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), null).status)
        val gated = client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), fx.token("newbie"))
        assertEquals(HttpStatusCode.Forbidden, gated.status)
        assertEquals("password_change_required", error(gated.bodyAsText()).code)
    }

    @Test
    fun readingLeavesNoTraceInAnyReceiverFacingResponse() = testApplication {
        startApp()
        val id = world()
        client.wish(id, "bob", "Socks")
        client.send("POST", ApiRoutes.groupReceiverQuestions(id.toLong()), fx.token("alice"), AskQuestionRequest("What size?"))
        val bob = fx.token("bob")
        val receiverFacing = listOf(
            ApiRoutes.groupWishlist(id.toLong()),
            ApiRoutes.group(id.toLong()),
            ApiRoutes.GROUPS,
            ApiRoutes.groupSantaQuestions(id.toLong()),
            ApiRoutes.groupReceiver(id.toLong()),
            ApiRoutes.groupReceiverWishlist(id.toLong()),
            ApiRoutes.ME,
        )
        val before = receiverFacing.map { client.getAs(it, bob).bodyAsText() }

        repeat(5) { client.santaReads(id, "alice") } // the Santa looks, again and again

        val after = receiverFacing.map { client.getAs(it, bob).bodyAsText() }
        assertEquals(before, after)
        for (body in after) {
            for (forbidden in listOf("lastRead", "readBy", "viewedBy", "viewed", "seenBy", "readAt", "reader")) assertFalse(body.contains(forbidden, ignoreCase = true), forbidden)
        }
    }

    @Test
    fun theSantaAndTheOwnersViewAgreeOnContentButOnlyTheOwnerGetsIds() = testApplication {
        startApp()
        val id = world()
        val item = client.wish(id, "bob", "Socks", "Grey", "https://shop.example")

        val owners = client.getAs(ApiRoutes.groupWishlist(id.toLong()), fx.token("bob")).bodyAsText()
        val santas = client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), fx.token("alice")).bodyAsText()

        assertEquals(true, owners.contains("\"id\":${item.id}"))
        assertFalse(santas.contains("\"id\""))
        assertEquals(item.createdAt, Json.decodeFromString<List<SharedWishlistItemDto>>(santas).single().createdAt)
    }
}
