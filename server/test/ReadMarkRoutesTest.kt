package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.LetterDto
import com.usuallycoding.opensecretsanta.contract.MarkReadRequest
import com.usuallycoding.opensecretsanta.contract.ReadMarkDto
import com.usuallycoding.opensecretsanta.contract.ReadMarksDto
import com.usuallycoding.opensecretsanta.contract.SendLetterRequest
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadMarkRoutesTest {
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

    private suspend fun HttpClient.reads(group: Int, who: String) = getAs(ApiRoutes.groupReceiverReads(group.toLong()), fx.token(who))

    private suspend fun HttpClient.marks(group: Int, who: String) = Json.decodeFromString<ReadMarksDto>(reads(group, who).bodyAsText())

    private suspend fun HttpClient.mark(group: Int, who: String, kind: String, seenUpTo: String?, token: String? = fx.token(who)) =
        send("PUT", ApiRoutes.groupReceiverRead(group.toLong(), kind), token, MarkReadRequest(seenUpTo))

    private suspend fun HttpClient.letter(group: Int, who: String, title: String = "Hello") =
        Json.decodeFromString<LetterDto>(send("POST", ApiRoutes.groupSantaLetters(group.toLong()), fx.token(who), SendLetterRequest(title, "Text")).bodyAsText())

    private suspend fun HttpClient.item(group: Int, who: String, title: String) =
        Json.decodeFromString<WishlistItemDto>(send("POST", ApiRoutes.groupWishlistItems(group.toLong()), fx.token(who), WishlistItemRequest(title)).bodyAsText())

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    private val names = listOf("alice", "bob", "carol", "dave")

    /** alice gives to bob, bob to carol, carol to dave, dave to alice, owned by admin-a, who does not take part. */
    private fun world(state: String = "active"): Int {
        fx.addUser("admin-a", admin = true)
        for (name in names) fx.addUser(name)
        val id = fx.addGroup("admin-a", *names.toTypedArray(), state = state)
        if (state == "active" || state == "archived") fx.assign(id, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        return id
    }

    private fun pause() = Thread.sleep(15)

    @Test
    fun nothingMarkedYetCountsEverythingWithNoTimeSeen() = testApplication {
        startApp()
        val id = world()
        client.item(id, "bob", "One")
        client.item(id, "bob", "Two")
        client.letter(id, "bob")

        val result = client.marks(id, "alice")

        assertEquals(ReadMarkDto(2, null), result.wishlist)
        assertEquals(ReadMarkDto(0, null), result.answers)
        assertEquals(ReadMarkDto(1, null), result.letters)
    }

    @Test
    fun markingAKindLowersItsCountAndLeavesTheOthers() = testApplication {
        startApp()
        val id = world()
        client.item(id, "bob", "Item")
        val letter = client.letter(id, "bob")

        val marked = client.mark(id, "alice", "letters", letter.sentAt)

        assertEquals(HttpStatusCode.OK, marked.status, marked.bodyAsText())
        val dto = Json.decodeFromString<ReadMarkDto>(marked.bodyAsText())
        assertEquals(0, dto.unread)
        assertEquals(Instant.parse(letter.sentAt), Instant.parse(dto.seenUpTo))
        val all = client.marks(id, "alice")
        assertEquals(1, all.wishlist.unread)
        assertNull(all.wishlist.seenUpTo)
        assertEquals(0, all.letters.unread)
    }

    @Test
    fun aLaterChangeCountsAfterTheMark() = testApplication {
        startApp()
        val id = world()
        val first = client.letter(id, "bob", "First")
        client.mark(id, "alice", "letters", first.sentAt)
        pause()

        client.letter(id, "bob", "Second")

        assertEquals(1, client.marks(id, "alice").letters.unread)
    }

    @Test
    fun anEditedItemAndAChangedAnswerCountAgainAfterTheMark() = testApplication {
        startApp()
        val id = world()
        val item = client.item(id, "bob", "Scarf")
        client.send("POST", ApiRoutes.groupReceiverQuestions(id.toLong()), fx.token("alice"), AskQuestionRequest("Colour?"))
        client.send("PUT", ApiRoutes.groupSantaQuestionAnswer(id.toLong(), 1), fx.token("bob"), AnswerRequest("Red"))
        pause()
        val seen = Instant.now().toString()
        client.mark(id, "alice", "wishlist", seen)
        client.mark(id, "alice", "answers", seen)
        assertEquals(0, client.marks(id, "alice").wishlist.unread + client.marks(id, "alice").answers.unread)
        pause()

        client.send("PUT", ApiRoutes.groupWishlistItem(id.toLong(), item.id), fx.token("bob"), WishlistItemRequest("Blue scarf"))
        client.send("PUT", ApiRoutes.groupSantaQuestionAnswer(id.toLong(), 1), fx.token("bob"), AnswerRequest("Blue"))

        val result = client.marks(id, "alice")
        assertEquals(1, result.wishlist.unread)
        assertEquals(1, result.answers.unread)
    }

    @Test
    fun aTimeNeverGoesBackAndNeverIntoTheFuture() = testApplication {
        startApp()
        val id = world()
        val later = Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
        client.mark(id, "alice", "wishlist", later.toString())

        val earlier = Json.decodeFromString<ReadMarkDto>(client.mark(id, "alice", "wishlist", later.minusSeconds(3600).toString()).bodyAsText())
        assertEquals(later, Instant.parse(earlier.seenUpTo))

        val future = Json.decodeFromString<ReadMarkDto>(client.mark(id, "alice", "answers", Instant.now().plusSeconds(86_400).toString()).bodyAsText())
        assertTrue(Instant.parse(future.seenUpTo).isBefore(Instant.now().plusSeconds(5)))
    }

    @Test
    fun markingTheSameTimeTwiceIsFine() = testApplication {
        startApp()
        val id = world()
        val time = Instant.now().minusSeconds(10).toString()

        val first = client.mark(id, "alice", "letters", time).bodyAsText()
        val second = client.mark(id, "alice", "letters", time)

        assertEquals(HttpStatusCode.OK, second.status)
        assertEquals(first, second.bodyAsText())
    }

    @Test
    fun aMissingOrInvalidTimeIsRefusedNamingItAndAnUnknownKindIsNotFound() = testApplication {
        startApp()
        val id = world()

        for (bad in listOf(null, "", "yesterday", "2026-12-24", "2026-13-40T00:00:00Z")) {
            val response = client.mark(id, "alice", "letters", bad)
            assertEquals(HttpStatusCode.BadRequest, response.status, "$bad")
            assertEquals("validation_failed", error(response.bodyAsText()).code)
            assertEquals("seenUpTo", error(response.bodyAsText()).field)
        }
        assertEquals(HttpStatusCode.NotFound, client.mark(id, "alice", "questions", Instant.now().toString()).status)
        assertEquals(HttpStatusCode.NotFound, client.mark(id, "alice", "WISHLIST", Instant.now().toString()).status)
        assertEquals(ReadMarkDto(0, null), client.marks(id, "alice").letters)
    }

    @Test
    fun beforeTheDrawTheMarksAreRefused() = testApplication {
        startApp()
        val id = world(state = "pending")

        for (response in listOf(client.reads(id, "alice"), client.mark(id, "alice", "letters", Instant.now().toString()))) {
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals("invalid_state", error(response.bodyAsText()).code)
        }
    }

    @Test
    fun anArchivedGroupsMarksCanBeReadAndChanged() = testApplication {
        startApp()
        val id = world()
        val letter = client.letter(id, "bob")
        fx.sql("UPDATE groups SET state = 'archived' WHERE id = $id")

        assertEquals(1, client.marks(id, "alice").letters.unread)
        assertEquals(HttpStatusCode.OK, client.mark(id, "alice", "letters", letter.sentAt).status)
        assertEquals(0, client.marks(id, "alice").letters.unread)
    }

    @Test
    fun aStrangerAnUnknownGroupAndNoSignInAreRefused() = testApplication {
        startApp()
        val id = world()
        fx.addUser("erin")

        assertEquals(HttpStatusCode.NotFound, client.reads(id, "erin").status)
        assertEquals(HttpStatusCode.NotFound, client.reads(9999, "alice").status)
        assertEquals(HttpStatusCode.NotFound, client.mark(id, "erin", "letters", Instant.now().toString()).status)
        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.groupReceiverReads(id.toLong()), fx.token("admin-a")).status)
        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.groupReceiverReads(id.toLong()), null).status)
        assertEquals(HttpStatusCode.Unauthorized, client.mark(id, "alice", "letters", Instant.now().toString(), token = null).status)
    }

    @Test
    fun everySantaHasTheirOwnMarks() = testApplication {
        startApp()
        val id = world()
        val toAlice = client.letter(id, "bob") // alice is bob's Santa
        client.letter(id, "carol") // bob is carol's Santa

        assertEquals(1, client.marks(id, "alice").letters.unread)
        assertEquals(1, client.marks(id, "bob").letters.unread)
        client.mark(id, "alice", "letters", toAlice.sentAt)
        assertEquals(0, client.marks(id, "alice").letters.unread)
        assertEquals(1, client.marks(id, "bob").letters.unread)
    }

    @Test
    fun theMarksGoWithTheGroup() = testApplication {
        startApp()
        val id = world()
        client.mark(id, "alice", "letters", Instant.now().minusSeconds(5).toString())
        fun count() = fx.database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM read_marks").use { it.next(); it.getInt(1) } }
        }
        assertEquals(1, count())

        fx.sql("PRAGMA foreign_keys = ON")
        client.send<Unit>("DELETE", "${ApiRoutes.adminGroup(id.toLong())}?confirm=true", fx.token("admin-a"))

        assertEquals(0, count())
    }

    // --- nobody but the Santa learns anything ---

    /** Every response of every participant, and the owner's, as raw text, except the Santa's own reads route. */
    private suspend fun HttpClient.everyoneSees(group: Int): Map<String, String> {
        val g = group.toLong()
        val participantPaths = listOf(
            ApiRoutes.group(g), ApiRoutes.groupDeliveryInfo(g), ApiRoutes.groupWishlist(g), ApiRoutes.groupReceiver(g),
            ApiRoutes.groupReceiverWishlist(g), ApiRoutes.groupReceiverQuestions(g), ApiRoutes.groupSantaQuestions(g), ApiRoutes.groupSanta(g),
            ApiRoutes.groupSantaLetters(g), ApiRoutes.groupReceiverLetters(g),
        )
        val adminPaths = listOf(
            ApiRoutes.adminGroup(g), ApiRoutes.ADMIN_GROUPS, ApiRoutes.adminGroupAssignments(g), ApiRoutes.adminGroupQuestions(g),
            ApiRoutes.adminGroupLetters(g), ApiRoutes.adminGroupParticipantWishlist(g, "bob"), ApiRoutes.ADMIN_USERS,
        )
        val all = names.flatMap { n -> participantPaths.map { p -> "$n $p" to getAs(p, fx.token(n)) } } +
            adminPaths.map { p -> "admin-a $p" to getAs(p, fx.token("admin-a")) }
        return all.associate { (key, response) -> key to "${response.status.value} ${response.bodyAsText()}" }
    }

    @Test
    fun readingAndMarkingChangesNothingAnyoneElseReceives() = testApplication {
        startApp()
        val id = world()
        client.item(id, "bob", "Scarf")
        client.letter(id, "bob")
        client.send("POST", ApiRoutes.groupReceiverQuestions(id.toLong()), fx.token("alice"), AskQuestionRequest("Colour?"))
        client.send("PUT", ApiRoutes.groupSantaQuestionAnswer(id.toLong(), 1), fx.token("bob"), AnswerRequest("Red"))
        val before = client.everyoneSees(id)

        repeat(3) {
            client.getAs(ApiRoutes.groupReceiverWishlist(id.toLong()), fx.token("alice"))
            client.getAs(ApiRoutes.groupReceiverLetters(id.toLong()), fx.token("alice"))
            client.reads(id, "alice")
            for (kind in listOf("wishlist", "answers", "letters")) client.mark(id, "alice", kind, Instant.now().toString())
        }

        assertEquals(before, client.everyoneSees(id))
    }

    @Test
    fun noResponseButTheSantasOwnCarriesAMarkACountOrATimeOfReading() = testApplication {
        startApp()
        val id = world()
        client.letter(id, "bob")
        for (kind in listOf("wishlist", "answers", "letters")) client.mark(id, "alice", kind, Instant.now().toString())

        for ((who, body) in client.everyoneSees(id)) {
            assertFalse("seenUpTo" in body || "\"unread\"" in body, "$who: $body")
        }
        assertNotNull(client.reads(id, "alice").bodyAsText().takeIf { "seenUpTo" in it && "unread" in it })
    }
}
