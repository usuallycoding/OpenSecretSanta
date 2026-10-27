package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.HealthResponse
import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What the owner's oversight views must not change or open up, and what they deliberately show. */
class AdminOversightPrivacyTest {
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

    private val names = listOf("owner", "alice", "bob", "carol")

    /** owner -> alice -> bob -> carol -> owner. The owner is an admin and also a participant. */
    private fun world(): Int {
        fx.addUser("owner", admin = true)
        for (n in names.drop(1)) fx.addUser(n)
        val id = fx.addGroup("owner", *names.toTypedArray(), state = "active")
        fx.assign(id, "owner" to "alice", "alice" to "bob", "bob" to "carol", "carol" to "owner")
        return id
    }

    private suspend fun HttpClient.fill(group: Int) {
        val g = group.toLong()
        for (n in names) {
            assertEquals(HttpStatusCode.Created, send("POST", ApiRoutes.groupWishlistItems(g), fx.token(n), WishlistItemRequest("SECRET-WISH-$n")).status)
        }
        for ((index, pair) in listOf("owner" to "alice", "alice" to "bob", "bob" to "carol", "carol" to "owner").withIndex()) {
            val (santa, receiver) = pair
            assertEquals(HttpStatusCode.Created, send("POST", ApiRoutes.groupReceiverQuestions(g), fx.token(santa), AskQuestionRequest("SECRET-QUESTION-$index")).status)
            assertEquals(HttpStatusCode.OK, send("PUT", ApiRoutes.groupSantaQuestionAnswer(g, 1), fx.token(receiver), AnswerRequest("SECRET-ANSWER-$index")).status)
        }
    }

    /** Every participant-facing response of every participant, as raw text. */
    private suspend fun HttpClient.participantResponses(group: Int): Map<String, String> {
        val g = group.toLong()
        val paths = listOf(
            ApiRoutes.group(g), ApiRoutes.groupWishlist(g), ApiRoutes.groupReceiver(g), ApiRoutes.groupReceiverWishlist(g),
            ApiRoutes.groupReceiverQuestions(g), ApiRoutes.groupSantaQuestions(g), ApiRoutes.groupSanta(g),
        )
        return names.flatMap { n -> paths.map { p -> "$n $p" to getAs(p, fx.token(n)).let { "${it.status.value} ${it.bodyAsText()}" } } }.toMap()
    }

    private suspend fun HttpClient.ownerReadsEverything(group: Int) {
        val owner = fx.token("owner")
        repeat(3) {
            for (n in names) assertEquals(HttpStatusCode.OK, getAs(ApiRoutes.adminGroupParticipantWishlist(group.toLong(), n), owner).status)
            assertEquals(HttpStatusCode.OK, getAs(ApiRoutes.adminGroupQuestions(group.toLong()), owner).status)
            assertEquals(HttpStatusCode.OK, getAs(ApiRoutes.adminGroupAssignments(group.toLong()), owner).status)
        }
    }

    @Test
    fun readingLeavesEveryParticipantFacingResponseUnchanged() = testApplication {
        startApp()
        val id = world()
        client.fill(id)
        val before = client.participantResponses(id)

        client.ownerReadsEverything(id)

        assertEquals(before, client.participantResponses(id))
    }

    @Test
    fun noOtherAdminEndpointReturnsWishlistOrQuestionText() = testApplication {
        startApp()
        val id = world()
        client.fill(id)
        fx.addUser("admin-b", admin = true)
        val owner = fx.token("owner")

        val bodies = listOf(
            client.getAs(ApiRoutes.ADMIN_USERS, owner),
            client.getAs(ApiRoutes.adminUser("alice"), owner),
            client.getAs(ApiRoutes.ADMIN_GROUPS, owner),
            client.getAs(ApiRoutes.adminGroup(id.toLong()), owner),
            client.getAs(ApiRoutes.adminGroupAssignments(id.toLong()), owner),
            // another admin, even asking about the owner's group, gets nothing at all
            client.getAs(ApiRoutes.adminGroupQuestions(id.toLong()), fx.token("admin-b")),
            client.getAs(ApiRoutes.adminGroupParticipantWishlist(id.toLong(), "alice"), fx.token("admin-b")),
        ).map { it.bodyAsText() }

        for (body in bodies) assertFalse("SECRET" in body, body)
    }

    @Test
    fun anOwnerWhoParticipatesLearnsTheirSantaThroughAdminEndpointsOnly() = testApplication {
        startApp()
        val id = world()
        client.fill(id)
        val owner = fx.token("owner")

        val santaEndpoint = client.getAs(ApiRoutes.groupSanta(id.toLong()), owner)
        assertEquals(HttpStatusCode.Conflict, santaEndpoint.status)
        assertEquals("invalid_state", Json.decodeFromString<ErrorResponse>(santaEndpoint.bodyAsText()).code)
        assertFalse("carol" in santaEndpoint.bodyAsText())
        assertFalse("carol" in client.getAs(ApiRoutes.groupSantaQuestions(id.toLong()), owner).bodyAsText())

        val threads = Json.decodeFromString<List<AdminQuestionThreadDto>>(client.getAs(ApiRoutes.adminGroupQuestions(id.toLong()), owner).bodyAsText())
        assertEquals("carol", threads.first { it.receiver == "owner" }.santa)
        val pairs = Json.decodeFromString<List<AssignmentDto>>(client.getAs(ApiRoutes.adminGroupAssignments(id.toLong()), owner).bodyAsText())
        assertTrue(AssignmentDto("carol", "owner") in pairs)
    }

    @Test
    fun noAdminEndpointWritesAWishlistQuestionOrAnswer() = testApplication {
        startApp()
        val id = world()
        client.fill(id)
        val owner = fx.token("owner")
        val g = id.toLong()
        val before = client.participantResponses(id)

        val attempts = listOf(
            "POST" to ApiRoutes.adminGroupParticipantWishlist(g, "alice"),
            "PUT" to ApiRoutes.adminGroupParticipantWishlist(g, "alice"),
            "DELETE" to ApiRoutes.adminGroupParticipantWishlist(g, "alice"),
            "POST" to ApiRoutes.adminGroupQuestions(g),
            "PUT" to ApiRoutes.adminGroupQuestions(g),
            "DELETE" to ApiRoutes.adminGroupQuestions(g),
            "PUT" to "${ApiRoutes.adminGroupQuestions(g)}/1/answer",
            "POST" to "${ApiRoutes.adminGroup(g)}/participants/alice/wishlist/items",
        )
        for ((method, path) in attempts) {
            val status = client.send(method, path, owner, mapOf("text" to "x")).status
            assertTrue(status == HttpStatusCode.NotFound || status == HttpStatusCode.MethodNotAllowed, "$method $path -> $status")
        }

        assertEquals(before, client.participantResponses(id))
    }

    @Test
    fun healthStillReportsTheCurrentSchemaVersion() = testApplication {
        startApp()
        world()

        val health = Json.decodeFromString<HealthResponse>(client.getAs(ApiRoutes.HEALTH, null).bodyAsText())

        assertEquals(HealthResponse("ok", schemaVersion = 6), health)
    }
}
