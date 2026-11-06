package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.LetterDto
import com.usuallycoding.opensecretsanta.contract.SendLetterRequest
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
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LetterRoutesTest {
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

    private suspend fun HttpClient.write(group: Int, who: String, title: String, text: String, token: String? = fx.token(who)) =
        send("POST", ApiRoutes.groupSantaLetters(group.toLong()), token, SendLetterRequest(title, text))

    private suspend fun HttpClient.sent(group: Int, who: String) = getAs(ApiRoutes.groupSantaLetters(group.toLong()), fx.token(who))

    private suspend fun HttpClient.inbox(group: Int, who: String) = getAs(ApiRoutes.groupReceiverLetters(group.toLong()), fx.token(who))

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    private val names = listOf("alice", "bob", "carol", "dave")

    /** alice gives to bob, bob to carol, carol to dave, dave to alice. */
    private fun world(state: String = "active"): Int {
        fx.addUser("admin-a", admin = true)
        for (name in names) fx.addUser(name)
        val id = fx.addGroup("admin-a", *names.toTypedArray(), state = state)
        if (state == "active" || state == "archived") fx.assign(id, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        return id
    }

    private fun count(): Int = fx.database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM letters").use { it.next(); it.getInt(1) } }
    }

    private fun letters(body: String) = Json.decodeFromString<List<LetterDto>>(body)

    @Test
    fun aLetterIsSentNumberedAndCarriesOnlyItsOwnFields() = testApplication {
        startApp()
        val id = world()

        val first = client.write(id, "bob", "Hello", "Thank you for the gifts")
        val second = client.write(id, "bob", "Again", "More thanks")

        assertEquals(HttpStatusCode.Created, first.status, first.bodyAsText())
        val one = Json.decodeFromString<LetterDto>(first.bodyAsText())
        assertEquals(listOf(1, 2), listOf(one.number, Json.decodeFromString<LetterDto>(second.bodyAsText()).number))
        assertEquals("Hello", one.title)
        assertEquals("Thank you for the gifts", one.text)
        assertTrue(Instant.parse(one.sentAt).toEpochMilli() > 0)
        assertEquals(setOf("number", "title", "text", "sentAt"), Json.parseToJsonElement(first.bodyAsText()).jsonObject.keys)
    }

    @Test
    fun numberingIsPerReceiver() = testApplication {
        startApp()
        val id = world()

        client.write(id, "bob", "t", "x")
        val carols = Json.decodeFromString<LetterDto>(client.write(id, "carol", "t", "x").bodyAsText())

        assertEquals(1, carols.number)
    }

    @Test
    fun aBlankOrTooLongTitleOrTextIsRefusedNamingTheFieldAndNothingIsStored() = testApplication {
        startApp()
        val id = world()

        for ((title, text, field) in listOf(
            Triple("", "text", "title"), Triple("  ", "text", "title"), Triple("t".repeat(101), "text", "title"),
            Triple("title", "", "text"), Triple("title", "   ", "text"), Triple("title", "x".repeat(2001), "text"),
        )) {
            val response = client.write(id, "bob", title, text)
            assertEquals(HttpStatusCode.BadRequest, response.status, "$title / $text".take(20))
            assertEquals("validation_failed", error(response.bodyAsText()).code)
            assertEquals(field, error(response.bodyAsText()).field)
        }
        assertEquals(0, count())
        assertEquals(HttpStatusCode.Created, client.write(id, "bob", "t".repeat(100), "x".repeat(2000)).status)
    }

    @Test
    fun theThirtyFirstLetterIsRefusedAndAnotherReceiverIsUnaffected() = testApplication {
        startApp()
        val id = world()
        repeat(30) { assertEquals(HttpStatusCode.Created, client.write(id, "bob", "t$it", "x").status) }

        val refused = client.write(id, "bob", "one more", "x")

        assertEquals(HttpStatusCode.Conflict, refused.status)
        assertEquals("invalid_state", error(refused.bodyAsText()).code)
        assertEquals(30, count())
        assertEquals(HttpStatusCode.Created, client.write(id, "carol", "t", "x").status)
    }

    @Test
    fun lettersAreSentOnlyWhileTheGroupIsActive() = testApplication {
        startApp()
        val pending = world(state = "pending")
        val archived = fx.addGroup("admin-a", *names.toTypedArray(), state = "archived")
        fx.assign(archived, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")

        for (id in listOf(pending, archived)) {
            val response = client.write(id, "bob", "t", "x")
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals("invalid_state", error(response.bodyAsText()).code)
        }
        assertEquals(0, count())
    }

    @Test
    fun aStrangerAnUnknownGroupAndNoSignInAreRefused() = testApplication {
        startApp()
        val id = world()
        fx.addUser("erin")

        assertEquals(HttpStatusCode.NotFound, client.write(id, "erin", "t", "x").status)
        assertEquals(HttpStatusCode.NotFound, client.write(9999, "bob", "t", "x").status)
        assertEquals(HttpStatusCode.Unauthorized, client.write(id, "bob", "t", "x", token = null).status)
        assertEquals(HttpStatusCode.NotFound, client.sent(id, "erin").status)
        assertEquals(HttpStatusCode.NotFound, client.inbox(id, "erin").status)
    }

    @Test
    fun theReceiverReadsTheirLettersAndTheSantaReadsThemToo() = testApplication {
        startApp()
        val id = world()
        client.write(id, "bob", "One", "x")
        client.write(id, "bob", "Two", "y")
        client.write(id, "carol", "From carol", "z")

        assertEquals(listOf("One", "Two"), letters(client.sent(id, "bob").bodyAsText()).map { it.title })
        // alice is bob's Santa; bob is carol's: each reads only their own receiver's letters
        assertEquals(listOf("One", "Two"), letters(client.inbox(id, "alice").bodyAsText()).map { it.title })
        assertEquals(listOf("From carol"), letters(client.inbox(id, "bob").bodyAsText()).map { it.title })
        assertEquals("[]", client.inbox(id, "carol").bodyAsText())
        assertEquals("[]", client.sent(id, "alice").bodyAsText())
    }

    @Test
    fun noResponseNamesAnyone() = testApplication {
        startApp()
        val id = world()
        client.write(id, "bob", "Hello", "Thank you")

        val bodies = listOf(client.sent(id, "bob"), client.inbox(id, "alice"), client.write(id, "carol", "Hi", "x")).map { it.bodyAsText() }

        for (body in bodies) for (name in names) assertFalse(name in body, "$name in $body")
        for (element in Json.parseToJsonElement(bodies[0]).jsonArray) {
            assertEquals(setOf("number", "title", "text", "sentAt"), element.jsonObject.keys)
        }
    }

    @Test
    fun anArchivedGroupsLettersAreStillReadableAndAPendingGroupsAreRefused() = testApplication {
        startApp()
        val id = world()
        client.write(id, "bob", "Kept", "x")
        fx.sql("UPDATE groups SET state = 'archived' WHERE id = $id")

        assertEquals(listOf("Kept"), letters(client.sent(id, "bob").bodyAsText()).map { it.title })
        assertEquals(listOf("Kept"), letters(client.inbox(id, "alice").bodyAsText()).map { it.title })

        val pending = fx.addGroup("admin-a", *names.toTypedArray(), state = "pending")
        for (response in listOf(client.sent(pending, "bob"), client.inbox(pending, "alice"))) {
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals("invalid_state", error(response.bodyAsText()).code)
        }
    }

    @Test
    fun lettersAreOneWayAndFinal() = testApplication {
        startApp()
        val id = world()
        client.write(id, "bob", "Hello", "x")

        val attempts = listOf(
            "PUT" to "${ApiRoutes.groupSantaLetters(id.toLong())}/1",
            "PATCH" to "${ApiRoutes.groupSantaLetters(id.toLong())}/1",
            "DELETE" to "${ApiRoutes.groupSantaLetters(id.toLong())}/1",
            "POST" to ApiRoutes.groupReceiverLetters(id.toLong()),
            "PUT" to ApiRoutes.groupReceiverLetters(id.toLong()),
            "DELETE" to ApiRoutes.groupReceiverLetters(id.toLong()),
        )
        for ((method, path) in attempts) {
            for (who in listOf("alice", "bob")) {
                val status = client.send(method, path, fx.token(who), SendLetterRequest("t", "x")).status
                assertTrue(status == HttpStatusCode.NotFound || status == HttpStatusCode.MethodNotAllowed, "$method $path as $who -> $status")
            }
        }
        assertEquals(listOf("Hello"), letters(client.sent(id, "bob").bodyAsText()).map { it.title })
    }

    @Test
    fun theGroupsLettersGoWithTheGroup() = testApplication {
        startApp()
        val id = world()
        client.write(id, "bob", "Hello", "x")
        assertEquals(1, count())

        fx.sql("PRAGMA foreign_keys = ON")
        val deleted = client.send<Unit>("DELETE", "${ApiRoutes.adminGroup(id.toLong())}?confirm=true", fx.token("admin-a"))

        assertEquals(HttpStatusCode.NoContent, deleted.status, deleted.bodyAsText())
        assertEquals(0, count())
    }
}
