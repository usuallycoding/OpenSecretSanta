package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
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
import kotlin.test.assertNull

class AdminOversightRoutesTest {
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

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    private val names = listOf("alice", "bob", "carol", "dave")

    /** alice -> bob -> carol -> dave -> alice, owned by admin-a, who does not participate. */
    private fun world(state: String = "active"): Int {
        fx.addUser("admin-a", admin = true)
        for (n in names) fx.addUser(n)
        val id = fx.addGroup("admin-a", *names.toTypedArray(), state = state)
        if (state == "active" || state == "archived") {
            fx.assign(id, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        }
        return id
    }

    private suspend fun HttpClient.wish(group: Int, user: String, title: String) {
        val response = send("POST", ApiRoutes.groupWishlistItems(group.toLong()), fx.token(user), WishlistItemRequest(title, "Note of $user", "https://shop.example/$user"))
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
    }

    private suspend fun HttpClient.ask(group: Int, santa: String, text: String) {
        val response = send("POST", ApiRoutes.groupReceiverQuestions(group.toLong()), fx.token(santa), AskQuestionRequest(text))
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
    }

    private suspend fun HttpClient.answer(group: Int, receiver: String, number: Int, text: String) {
        val response = send("PUT", ApiRoutes.groupSantaQuestionAnswer(group.toLong(), number), fx.token(receiver), AnswerRequest(text))
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
    }

    private suspend fun HttpClient.ownerReadsWishlist(group: Int, who: String, token: String = fx.token("admin-a")) =
        getAs(ApiRoutes.adminGroupParticipantWishlist(group.toLong(), who), token)

    private suspend fun HttpClient.ownerReadsThreads(group: Int, token: String = fx.token("admin-a")) =
        getAs(ApiRoutes.adminGroupQuestions(group.toLong()), token)

    @Test
    fun theOwnerReadsAWishlistWithoutIds() = testApplication {
        startApp()
        val id = world()
        client.wish(id, "bob", "First wish")
        client.wish(id, "bob", "Second wish")

        val response = client.ownerReadsWishlist(id, "bob")

        assertEquals(HttpStatusCode.OK, response.status)
        val items = Json.decodeFromString<List<SharedWishlistItemDto>>(response.bodyAsText())
        assertEquals(listOf("First wish", "Second wish"), items.map { it.title })
        assertEquals("Note of bob", items.first().note)
        for (element in Json.parseToJsonElement(response.bodyAsText()).jsonArray) {
            assertEquals(setOf("title", "note", "url", "createdAt", "updatedAt"), element.jsonObject.keys)
        }
    }

    @Test
    fun aPendingGroupsWishlistsAreReadableAndAnEmptyOneIsAnEmptyArray() = testApplication {
        startApp()
        val id = world(state = "pending")
        client.wish(id, "bob", "Early wish")

        assertEquals(listOf("Early wish"), Json.decodeFromString<List<SharedWishlistItemDto>>(client.ownerReadsWishlist(id, "bob").bodyAsText()).map { it.title })
        assertEquals("[]", client.ownerReadsWishlist(id, "alice").bodyAsText())
    }

    @Test
    fun anArchivedGroupsWishlistsAreStillReadable() = testApplication {
        startApp()
        val id = world()
        client.wish(id, "bob", "Kept")
        fx.sql("UPDATE groups SET state = 'archived' WHERE id = $id")

        assertEquals(listOf("Kept"), Json.decodeFromString<List<SharedWishlistItemDto>>(client.ownerReadsWishlist(id, "bob").bodyAsText()).map { it.title })
    }

    @Test
    fun aDraftHasNoWishlists() = testApplication {
        startApp()
        val id = world(state = "draft")

        val response = client.ownerReadsWishlist(id, "bob")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("invalid_state", error(response.bodyAsText()).code)
    }

    @Test
    fun aNonParticipantAnInvalidNameAnUnknownGroupAndANonNumericIdAreNotFound() = testApplication {
        startApp()
        val id = world()
        fx.addUser("erin")

        assertEquals(HttpStatusCode.NotFound, client.ownerReadsWishlist(id, "erin").status)
        assertEquals(HttpStatusCode.NotFound, client.ownerReadsWishlist(id, "nobody").status)
        assertEquals(HttpStatusCode.NotFound, client.ownerReadsWishlist(id, "x").status)
        assertEquals(HttpStatusCode.NotFound, client.ownerReadsWishlist(9999, "bob").status)
        assertEquals(HttpStatusCode.NotFound, client.getAs("/admin/groups/abc/participants/bob/wishlist", fx.token("admin-a")).status)
    }

    @Test
    fun theOwnerReadsEveryThreadNamingBothPeople() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "What size?")
        client.ask(id, "alice", "Colour?")
        client.answer(id, "bob", 1, "Medium")
        client.ask(id, "dave", "Allergies?")

        val response = client.ownerReadsThreads(id)

        assertEquals(HttpStatusCode.OK, response.status)
        val threads = Json.decodeFromString<List<AdminQuestionThreadDto>>(response.bodyAsText())
        assertEquals(listOf("alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice"), threads.map { it.santa to it.receiver })
        assertEquals(listOf("What size?", "Colour?"), threads[0].questions.map { it.text })
        assertEquals("Medium", threads[0].questions[0].answer)
        assertNull(threads[0].questions[1].answer)
        assertEquals(emptyList(), threads[1].questions)
        assertEquals(listOf("Allergies?"), threads[3].questions.map { it.text })
    }

    @Test
    fun anArchivedGroupsThreadsAreStillReadable() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "What size?")
        fx.sql("UPDATE groups SET state = 'archived' WHERE id = $id")

        val threads = Json.decodeFromString<List<AdminQuestionThreadDto>>(client.ownerReadsThreads(id).bodyAsText())

        assertEquals(listOf("What size?"), threads[0].questions.map { it.text })
    }

    @Test
    fun threadsExistOnlyOnceTheGroupIsActive() = testApplication {
        startApp()
        val draft = world(state = "draft")
        val pending = fx.addGroup("admin-a", *names.toTypedArray(), state = "pending")

        for (id in listOf(draft, pending)) {
            val response = client.ownerReadsThreads(id)
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals("invalid_state", error(response.bodyAsText()).code)
        }
    }

    @Test
    fun anotherAdminGetsNotFoundAndANormalUserForbidden() = testApplication {
        startApp()
        val id = world()
        fx.addUser("admin-b", admin = true)

        for (response in listOf(client.ownerReadsWishlist(id, "bob", fx.token("admin-b")), client.ownerReadsThreads(id, fx.token("admin-b")))) {
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
        for (response in listOf(client.ownerReadsWishlist(id, "bob", fx.token("alice")), client.ownerReadsThreads(id, fx.token("alice")))) {
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
        assertEquals(HttpStatusCode.NotFound, client.ownerReadsThreads(9999).status)
        assertEquals(HttpStatusCode.NotFound, client.getAs("/admin/groups/abc/questions", fx.token("admin-a")).status)
    }

    @Test
    fun aSignedOutCallerIsRefused() = testApplication {
        startApp()
        val id = world()

        assertEquals(HttpStatusCode.Unauthorized, client.ownerReadsWishlist(id, "bob", token = "").status)
        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.adminGroupQuestions(id.toLong()), null).status)
    }

    @Test
    fun anOwnerWithATemporaryPasswordMustChangeItFirst() = testApplication {
        startApp()
        val id = world()
        fx.addUser("admin-t", admin = true, mustChange = true)
        fx.sql("UPDATE groups SET owner_username = 'admin-t' WHERE id = $id")

        for (response in listOf(client.ownerReadsWishlist(id, "bob", fx.token("admin-t")), client.ownerReadsThreads(id, fx.token("admin-t")))) {
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals("password_change_required", error(response.bodyAsText()).code)
        }
    }
}
