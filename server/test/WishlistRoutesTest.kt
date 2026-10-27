package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest
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
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WishlistRoutesTest {
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
    private suspend fun HttpClient.deleteAs(path: String, token: String?) = send<Unit>("DELETE", path, token)

    private suspend fun HttpClient.addItem(group: Int, user: String, title: String, note: String? = null, url: String? = null) =
        send("POST", ApiRoutes.groupWishlistItems(group.toLong()), fx.token(user), WishlistItemRequest(title, note, url))

    private suspend fun HttpClient.created(group: Int, user: String, title: String, note: String? = null, url: String? = null): WishlistItemDto {
        val response = addItem(group, user, title, note, url)
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return Json.decodeFromString(response.bodyAsText())
    }

    private suspend fun HttpClient.wishes(group: Int, user: String): List<WishlistItemDto> =
        Json.decodeFromString(getAs(ApiRoutes.groupWishlist(group.toLong()), fx.token(user)).bodyAsText())

    private suspend fun HttpClient.titles(group: Int, user: String = "alice") = wishes(group, user).map { it.title }

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    private val names = listOf("alice", "bob", "carol", "dave")

    private fun world(state: String = "active"): Int {
        fx.addUser("admin-a", admin = true)
        for (name in names) fx.addUser(name)
        val id = fx.addGroup("admin-a", *names.toTypedArray(), state = state)
        if (state == "active" || state == "archived") fx.assign(id, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        return id
    }

    private fun secondGroup(state: String = "active"): Int =
        fx.addGroup("admin-a", *names.toTypedArray(), state = state, name = "Another").also {
            if (state == "active") fx.assign(it, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        }

    private fun count(): Int = fx.database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM wishlist_items").use { it.next(); it.getInt(1) } }
    }

    // --- reading and adding ---

    @Test
    fun aNewListIsEmpty() = testApplication {
        startApp()
        val id = world()

        val response = client.getAs(ApiRoutes.groupWishlist(id.toLong()), fx.token("alice"))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun anAddedItemIsReturnedWithIdAndTimesAndAppendedLast() = testApplication {
        startApp()
        val id = world()

        val first = client.created(id, "alice", "  Wool socks ", "Grey, size M", "https://shop.example/socks")
        val second = client.created(id, "alice", "A book")

        assertEquals("Wool socks", first.title)
        assertEquals("Grey, size M", first.note)
        assertEquals("https://shop.example/socks", first.url)
        assertNull(second.note)
        assertNull(second.url)
        assertTrue(Instant.parse(first.createdAt).toEpochMilli() > 0)
        assertEquals(first.createdAt, first.updatedAt)
        assertEquals(listOf("Wool socks", "A book"), client.titles(id))
        assertTrue(first.id != second.id)
    }

    @Test
    fun validationErrorsNameTheFieldAndStoreNothing() = testApplication {
        startApp()
        val id = world()
        val cases = listOf(
            "title" to WishlistItemRequest("   "),
            "title" to WishlistItemRequest("t".repeat(201)),
            "note" to WishlistItemRequest("ok", note = "n".repeat(1001)),
            "url" to WishlistItemRequest("ok", url = "javascript:alert(1)"),
            "url" to WishlistItemRequest("ok", url = "ftp://host/file"),
            "url" to WishlistItemRequest("ok", url = "not a url"),
            "url" to WishlistItemRequest("ok", url = "https://shop.example/" + "a".repeat(2000)),
        )
        for ((field, request) in cases) {
            val response = client.send("POST", ApiRoutes.groupWishlistItems(id.toLong()), fx.token("alice"), request)
            assertEquals(HttpStatusCode.BadRequest, response.status, "$field: ${request.url?.take(20)}")
            assertEquals(field, error(response.bodyAsText()).field)
        }
        assertEquals(0, count())
    }

    @Test
    fun aMalformedBodyIsABadRequest() = testApplication {
        startApp()
        val id = world()

        val response = client.request(ApiRoutes.groupWishlistItems(id.toLong())) {
            method = HttpMethod.Post
            header(HttpHeaders.Authorization, "Bearer ${fx.token("alice")}")
            contentType(ContentType.Application.Json)
            setBody("{nope")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun theFiftyFirstItemIsRefused() = testApplication {
        startApp()
        val id = world()
        repeat(50) { assertEquals(HttpStatusCode.Created, client.addItem(id, "alice", "Item $it").status) }

        val response = client.addItem(id, "alice", "One too many")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("invalid_state", error(response.bodyAsText()).code)
        assertEquals(50, client.wishes(id, "alice").size)
        assertEquals(HttpStatusCode.Created, client.addItem(id, "bob", "Bob's own list").status)
    }

    // --- editing, deleting, reordering ---

    @Test
    fun editingKeepsThePlaceAndMovesTheLastChangedTime() = testApplication {
        startApp()
        val id = world()
        client.created(id, "alice", "One")
        val second = client.created(id, "alice", "Two")
        client.created(id, "alice", "Three")
        Thread.sleep(5)

        val response = client.send("PUT", ApiRoutes.groupWishlistItem(id.toLong(), second.id), fx.token("alice"), WishlistItemRequest("Two, better", "note", "https://shop.example/two"))

        assertEquals(HttpStatusCode.OK, response.status)
        val edited = Json.decodeFromString<WishlistItemDto>(response.bodyAsText())
        assertEquals(second.createdAt, edited.createdAt)
        assertTrue(Instant.parse(edited.updatedAt) > Instant.parse(edited.createdAt))
        assertEquals(listOf("One", "Two, better", "Three"), client.titles(id))
    }

    @Test
    fun anEditCanClearTheNoteAndLinkAndAnInvalidOneChangesNothing() = testApplication {
        startApp()
        val id = world()
        val item = client.created(id, "alice", "One", "note", "https://shop.example")

        val bad = client.send("PUT", ApiRoutes.groupWishlistItem(id.toLong(), item.id), fx.token("alice"), WishlistItemRequest("", "x", null))
        assertEquals(HttpStatusCode.BadRequest, bad.status)
        assertEquals("note", client.wishes(id, "alice").single().note)

        client.send("PUT", ApiRoutes.groupWishlistItem(id.toLong(), item.id), fx.token("alice"), WishlistItemRequest("One"))
        val cleared = client.wishes(id, "alice").single()
        assertNull(cleared.note)
        assertNull(cleared.url)
    }

    @Test
    fun deletingReturnsNoContentAndKeepsTheOrderOfTheRest() = testApplication {
        startApp()
        val id = world()
        client.created(id, "alice", "One")
        val middle = client.created(id, "alice", "Two")
        client.created(id, "alice", "Three")

        val response = client.deleteAs(ApiRoutes.groupWishlistItem(id.toLong(), middle.id), fx.token("alice"))

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(listOf("One", "Three"), client.titles(id))
    }

    @Test
    fun reorderingAppliesTheCompleteNewOrder() = testApplication {
        startApp()
        val id = world()
        val a = client.created(id, "alice", "A")
        val b = client.created(id, "alice", "B")
        val c = client.created(id, "alice", "C")

        val response = client.send("PUT", ApiRoutes.groupWishlistOrder(id.toLong()), fx.token("alice"), ReorderWishlistRequest(listOf(c.id, a.id, b.id)))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("C", "A", "B"), Json.decodeFromString<List<WishlistItemDto>>(response.bodyAsText()).map { it.title })
        assertEquals(listOf("C", "A", "B"), client.titles(id))
    }

    @Test
    fun aStaleOrWrongOrderIsRefusedNamingItemIdsAndNothingMoves() = testApplication {
        startApp()
        val id = world()
        val a = client.created(id, "alice", "A")
        val b = client.created(id, "alice", "B")
        val bobs = client.created(id, "bob", "Bob's")
        client.created(id, "alice", "Added on the second device")

        for (ids in listOf(listOf(a.id, b.id), listOf(a.id, a.id, b.id), listOf(a.id, b.id, bobs.id), emptyList())) {
            val response = client.send("PUT", ApiRoutes.groupWishlistOrder(id.toLong()), fx.token("alice"), ReorderWishlistRequest(ids))
            assertEquals(HttpStatusCode.BadRequest, response.status, ids.toString())
            assertEquals("itemIds", error(response.bodyAsText()).field)
        }
        assertEquals(listOf("A", "B", "Added on the second device"), client.titles(id))
    }

    // --- ownership and groups ---

    @Test
    fun someoneElsesItemAnOtherGroupsItemAndANonNumericIdAreNotFound() = testApplication {
        startApp()
        val first = world()
        val second = secondGroup()
        val bobs = client.created(first, "bob", "Bob's")
        val inFirst = client.created(first, "alice", "Alice in the first")
        val alice = fx.token("alice")

        val attempts = listOf(
            client.send("PUT", ApiRoutes.groupWishlistItem(first.toLong(), bobs.id), alice, WishlistItemRequest("Hijack")),
            client.deleteAs(ApiRoutes.groupWishlistItem(first.toLong(), bobs.id), alice),
            client.send("PUT", ApiRoutes.groupWishlistItem(second.toLong(), inFirst.id), alice, WishlistItemRequest("Wrong group")),
            client.deleteAs(ApiRoutes.groupWishlistItem(second.toLong(), inFirst.id), alice),
            client.send("PUT", "/groups/$first/wishlist/items/abc", alice, WishlistItemRequest("x")),
            client.deleteAs("/groups/$first/wishlist/items/99999", alice),
        )

        for (response in attempts) assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(listOf("Bob's"), client.titles(first, "bob"))
        assertEquals(listOf("Alice in the first"), client.titles(first, "alice"))
    }

    @Test
    fun oneUserKeepsIndependentListsInDifferentGroups() = testApplication {
        startApp()
        val first = world()
        val second = secondGroup()

        client.created(first, "alice", "Socks")
        client.created(second, "alice", "Book")
        client.created(second, "alice", "Mug")

        assertEquals(listOf("Socks"), client.titles(first))
        assertEquals(listOf("Book", "Mug"), client.titles(second))
    }

    @Test
    fun strangersDraftsAndUnknownGroupsAreNotFound() = testApplication {
        startApp()
        val id = world()
        val draft = secondGroup("draft")
        fx.addUser("outsider")

        val attempts = listOf(
            client.getAs(ApiRoutes.groupWishlist(id.toLong()), fx.token("outsider")),
            client.addItem(id, "outsider", "x"),
            client.getAs(ApiRoutes.groupWishlist(id.toLong()), fx.token("admin-a")), // the owner does not participate
            client.getAs(ApiRoutes.groupWishlist(draft.toLong()), fx.token("alice")),
            client.addItem(draft, "alice", "x"),
            client.getAs(ApiRoutes.groupWishlist(9999), fx.token("alice")),
            client.getAs("/groups/nope/wishlist", fx.token("alice")),
        )

        for (response in attempts) assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(0, count())
    }

    // --- states ---

    @Test
    fun aPendingGroupAcceptsEditsSoPeopleCanPrepareBeforeTheDraw() = testApplication {
        startApp()
        val id = world("pending")

        val item = client.created(id, "alice", "Prepared early")
        client.send("PUT", ApiRoutes.groupWishlistItem(id.toLong(), item.id), fx.token("alice"), WishlistItemRequest("Prepared, edited"))

        assertEquals(listOf("Prepared, edited"), client.titles(id))
    }

    @Test
    fun anArchivedListIsReadOnly() = testApplication {
        startApp()
        val id = world()
        val item = client.created(id, "alice", "Kept")
        fx.sql("UPDATE groups SET state = 'archived' WHERE id = $id")
        val alice = fx.token("alice")

        val attempts = listOf(
            client.addItem(id, "alice", "New"),
            client.send("PUT", ApiRoutes.groupWishlistItem(id.toLong(), item.id), alice, WishlistItemRequest("Changed")),
            client.deleteAs(ApiRoutes.groupWishlistItem(id.toLong(), item.id), alice),
            client.send("PUT", ApiRoutes.groupWishlistOrder(id.toLong()), alice, ReorderWishlistRequest(listOf(item.id))),
        )

        for (response in attempts) {
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals("invalid_state", error(response.bodyAsText()).code)
        }
        assertEquals(listOf("Kept"), client.titles(id))
    }

    @Test
    fun aParticipantRemovedFromAPendingGroupLosesAccessAndGetsTheListBackWhenAddedAgain() = testApplication {
        startApp()
        val id = world("pending")
        client.created(id, "alice", "Kept while away")
        fx.sql("DELETE FROM group_participants WHERE group_id = $id AND username = 'alice'")

        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.groupWishlist(id.toLong()), fx.token("alice")).status)
        assertEquals(HttpStatusCode.NotFound, client.addItem(id, "alice", "while removed").status)

        fx.sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, 'alice', 9)")
        assertEquals(listOf("Kept while away"), client.titles(id))
    }

    // --- authentication, gate, admins ---

    @Test
    fun authenticationAndThePasswordChangeGateApply() = testApplication {
        startApp()
        val id = world()
        fx.addUser("newbie", mustChange = true)

        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.groupWishlist(id.toLong()), null).status)
        val gated = client.getAs(ApiRoutes.groupWishlist(id.toLong()), fx.token("newbie"))
        assertEquals(HttpStatusCode.Forbidden, gated.status)
        assertEquals("password_change_required", error(gated.bodyAsText()).code)
    }

    @Test
    fun noAdminResponseExposesWishlistText() = testApplication {
        startApp()
        val id = world()
        client.created(id, "alice", "A very private wish", "A very private note", "https://shop.example/private")
        val admin = fx.token("admin-a")

        val responses = listOf(
            client.getAs(ApiRoutes.adminGroup(id.toLong()), admin),
            client.getAs(ApiRoutes.ADMIN_GROUPS, admin),
            client.getAs(ApiRoutes.adminGroupAssignments(id.toLong()), admin),
            client.getAs(ApiRoutes.ADMIN_USERS, admin),
            client.getAs(ApiRoutes.adminUser("alice"), admin),
        )

        for (response in responses) {
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertFalse(body.contains("private wish") || body.contains("private note") || body.contains("shop.example"), body)
        }
    }
}
