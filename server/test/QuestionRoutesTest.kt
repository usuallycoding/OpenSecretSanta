package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.QuestionDto
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

class QuestionRoutesTest {
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

    private suspend fun HttpClient.ask(group: Int, user: String, text: String) =
        send("POST", ApiRoutes.groupReceiverQuestions(group.toLong()), fx.token(user), AskQuestionRequest(text))

    private suspend fun HttpClient.answer(group: Int, user: String, number: Int, text: String) =
        send("PUT", ApiRoutes.groupSantaQuestionAnswer(group.toLong(), number), fx.token(user), AnswerRequest(text))

    private suspend fun HttpClient.mine(group: Int, user: String): List<QuestionDto> =
        Json.decodeFromString(getAs(ApiRoutes.groupReceiverQuestions(group.toLong()), fx.token(user)).bodyAsText())

    private suspend fun HttpClient.fromSanta(group: Int, user: String): List<QuestionDto> =
        Json.decodeFromString(getAs(ApiRoutes.groupSantaQuestions(group.toLong()), fx.token(user)).bodyAsText())

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    private val names = listOf("alice", "bob", "carol", "dave")

    /** alice -> bob -> carol -> dave -> alice in a group in [state]. */
    private fun world(state: String = "active"): Int {
        fx.addUser("admin-a", admin = true)
        for (name in names) fx.addUser(name)
        val id = fx.addGroup("admin-a", *names.toTypedArray(), state = state)
        if (state == "active" || state == "archived") fx.assign(id, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        return id
    }

    private fun setState(id: Int, state: String) = fx.sql("UPDATE groups SET state = '$state' WHERE id = $id")

    private fun count(): Int = fx.database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM questions").use { it.next(); it.getInt(1) } }
    }

    // --- asking ---

    @Test
    fun questionsAreNumberedFromOneAndCarryTheirText() = testApplication {
        startApp()
        val id = world()

        val first = client.ask(id, "alice", "What size are you?")
        val second = client.ask(id, "alice", "Any allergies?")

        assertEquals(HttpStatusCode.Created, first.status)
        val one = Json.decodeFromString<QuestionDto>(first.bodyAsText())
        val two = Json.decodeFromString<QuestionDto>(second.bodyAsText())
        assertEquals(listOf(1, 2), listOf(one.number, two.number))
        assertEquals("What size are you?", one.text)
        assertNull(one.answer)
        assertTrue(Instant.parse(one.askedAt).toEpochMilli() > 0)
    }

    @Test
    fun numberingIsPerSanta() = testApplication {
        startApp()
        val id = world()

        client.ask(id, "alice", "from alice")
        val bobs = Json.decodeFromString<QuestionDto>(client.ask(id, "bob", "from bob").bodyAsText())

        assertEquals(1, bobs.number)
    }

    @Test
    fun blankOrOverLongTextIsRejectedNamingTheFieldAndNothingIsStored() = testApplication {
        startApp()
        val id = world()

        for (bad in listOf("", "   ", "q".repeat(1001))) {
            val response = client.ask(id, "alice", bad)
            assertEquals(HttpStatusCode.BadRequest, response.status, bad.take(10))
            assertEquals("text", error(response.bodyAsText()).field)
        }
        assertEquals(0, count())
        assertEquals(HttpStatusCode.Created, client.ask(id, "alice", "q".repeat(1000)).status)
    }

    @Test
    fun theThirtyFirstQuestionIsRefused() = testApplication {
        startApp()
        val id = world()
        repeat(30) { assertEquals(HttpStatusCode.Created, client.ask(id, "alice", "Question $it").status) }

        val response = client.ask(id, "alice", "one too many")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("invalid_state", error(response.bodyAsText()).code)
        assertEquals(30, client.mine(id, "alice").size)
        assertEquals(HttpStatusCode.Created, client.ask(id, "bob", "bob still can").status)
    }

    @Test
    fun aMalformedBodyIsABadRequest() = testApplication {
        startApp()
        val id = world()

        val response = client.request(ApiRoutes.groupReceiverQuestions(id.toLong())) {
            method = HttpMethod.Post
            header(HttpHeaders.Authorization, "Bearer ${fx.token("alice")}")
            contentType(ContentType.Application.Json)
            setBody("{nope")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // --- states ---

    @Test
    fun nothingCanBeAskedOrReadBeforeTheGroupIsActive() = testApplication {
        startApp()
        val id = world("pending")

        assertEquals(HttpStatusCode.Conflict, client.ask(id, "alice", "too early").status)
        assertEquals(HttpStatusCode.Conflict, client.getAs(ApiRoutes.groupReceiverQuestions(id.toLong()), fx.token("alice")).status)
        assertEquals(HttpStatusCode.Conflict, client.getAs(ApiRoutes.groupSantaQuestions(id.toLong()), fx.token("bob")).status)
        assertEquals(HttpStatusCode.Conflict, client.answer(id, "bob", 1, "too early").status)
    }

    @Test
    fun anArchivedThreadIsReadOnly() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "Colour?")
        client.answer(id, "bob", 1, "Green")
        setState(id, "archived")

        assertEquals(HttpStatusCode.Conflict, client.ask(id, "alice", "too late").status)
        assertEquals(HttpStatusCode.Conflict, client.answer(id, "bob", 1, "Blue").status)
        assertEquals("Green", client.mine(id, "alice").single().answer)
        assertEquals("Green", client.fromSanta(id, "bob").single().answer)
        assertEquals(1, count())
    }

    // --- reading and answering ---

    @Test
    fun theSantaSeesAnswersAndTheirEdits() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "Colour?")
        client.ask(id, "alice", "Size?")

        val first = Json.decodeFromString<QuestionDto>(client.answer(id, "bob", 1, "Green").bodyAsText())
        Thread.sleep(5)
        val edited = Json.decodeFromString<QuestionDto>(client.answer(id, "bob", 1, "  Blue  ").bodyAsText())

        assertEquals("Green", first.answer)
        assertEquals(first.answeredAt, first.answerUpdatedAt)
        assertEquals("Blue", edited.answer)
        assertEquals(first.answeredAt, edited.answeredAt)
        assertTrue(Instant.parse(edited.answerUpdatedAt!!) > Instant.parse(edited.answeredAt!!))
        val seen = client.mine(id, "alice")
        assertEquals(listOf("Blue", null), seen.map { it.answer })
        assertEquals(edited.answerUpdatedAt, seen.first().answerUpdatedAt)
    }

    @Test
    fun theReceiverSeesTheQuestionsAndTheirOwnAnswers() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "First")
        client.ask(id, "alice", "Second")
        client.answer(id, "bob", 2, "Reply")

        val received = client.fromSanta(id, "bob")

        assertEquals(listOf(1, 2), received.map { it.number })
        assertEquals(listOf("First", "Second"), received.map { it.text })
        assertEquals(listOf(null, "Reply"), received.map { it.answer })
    }

    @Test
    fun invalidOrUnknownAnswersAreRejectedAndTheOldAnswerSurvives() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "Colour?")
        client.answer(id, "bob", 1, "Green")

        for (bad in listOf("", "  ", "a".repeat(2001))) {
            val response = client.answer(id, "bob", 1, bad)
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("text", error(response.bodyAsText()).field)
        }
        assertEquals(HttpStatusCode.NotFound, client.answer(id, "bob", 2, "no such question").status)
        assertEquals(HttpStatusCode.NotFound, client.send("PUT", "/groups/$id/santa/questions/abc/answer", fx.token("bob"), AnswerRequest("x")).status)
        assertEquals("Green", client.fromSanta(id, "bob").single().answer)
        assertEquals(HttpStatusCode.OK, client.answer(id, "bob", 1, "a".repeat(2000)).status)
    }

    @Test
    fun aReceiverCannotAnswerSomeoneElsesQuestion() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "Alice asks bob")

        // carol's Santa is bob, who asked nothing: question 1 does not exist for carol
        assertEquals(HttpStatusCode.NotFound, client.answer(id, "carol", 1, "hijack").status)
        assertNull(client.mine(id, "alice").single().answer)
    }

    @Test
    fun oneParticipantsTwoThreadsStayApart() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "From alice to bob")
        client.ask(id, "bob", "From bob to carol")

        assertEquals(listOf("From bob to carol"), client.mine(id, "bob").map { it.text })
        assertEquals(listOf("From alice to bob"), client.fromSanta(id, "bob").map { it.text })
    }

    // --- anonymity sweep ---

    @Test
    fun noReceiverFacingResponseContainsAnyParticipantsUsername() = testApplication {
        startApp()
        val id = world()
        for (santa in names) client.ask(id, santa, "A question from the Santa of someone")
        val bodies = mutableListOf<String>()

        for (receiver in names) {
            bodies += client.getAs(ApiRoutes.groupSantaQuestions(id.toLong()), fx.token(receiver)).bodyAsText()
            bodies += client.answer(id, receiver, 1, "My answer").bodyAsText()
            bodies += client.getAs(ApiRoutes.groupSanta(id.toLong()), fx.token(receiver)).bodyAsText() // refused while Active
            bodies += client.answer(id, receiver, 99, "x").bodyAsText() // not found
            bodies += client.answer(id, receiver, 1, "").bodyAsText() // validation error
        }

        for (body in bodies) {
            for (name in names) assertFalse(body.contains(name), "'$name' leaked in: $body")
        }
        assertEquals(5 * names.size, bodies.size)
    }

    @Test
    fun theReceiversThreadHasNoSantaDataAndNumbersAreNotGlobal() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "alice 1")
        client.ask(id, "bob", "bob 1")
        client.ask(id, "bob", "bob 2")

        val carolSees = client.fromSanta(id, "carol") // carol's Santa is bob
        val bobSees = client.fromSanta(id, "bob") // bob's Santa is alice

        assertEquals(listOf(1, 2), carolSees.map { it.number })
        assertEquals(listOf(1), bobSees.map { it.number })
    }

    // --- who may use it ---

    @Test
    fun strangersDraftsAndTheNonParticipatingOwnerAreNotFound() = testApplication {
        startApp()
        val id = world()
        fx.addUser("outsider")
        val draft = fx.addGroup("admin-a", *names.toTypedArray(), state = "draft", name = "Draft")

        for (who in listOf("outsider", "admin-a")) {
            assertEquals(HttpStatusCode.NotFound, client.ask(id, who, "hi").status, who)
            assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.groupReceiverQuestions(id.toLong()), fx.token(who)).status, who)
            assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.groupSantaQuestions(id.toLong()), fx.token(who)).status, who)
            assertEquals(HttpStatusCode.NotFound, client.answer(id, who, 1, "hi").status, who)
        }
        assertEquals(HttpStatusCode.NotFound, client.ask(draft, "alice", "hi").status)
        assertEquals(HttpStatusCode.NotFound, client.ask(9999, "alice", "hi").status)
        assertEquals(0, count())
    }

    @Test
    fun authenticationAndThePasswordChangeGateApply() = testApplication {
        startApp()
        val id = world()
        fx.addUser("newbie", mustChange = true)

        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.groupReceiverQuestions(id.toLong()), null).status)
        val gated = client.ask(id, "newbie", "hi")
        assertEquals(HttpStatusCode.Forbidden, gated.status)
        assertEquals("password_change_required", error(gated.bodyAsText()).code)
    }

    @Test
    fun noAdminResponseExposesQuestionOrAnswerText() = testApplication {
        startApp()
        val id = world()
        client.ask(id, "alice", "A very private question")
        client.answer(id, "bob", 1, "A very private answer")
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
            assertFalse(body.contains("private question") || body.contains("private answer"), body)
        }
        assertNotNull(Json.decodeFromString<List<AdminGroupDto>>(responses[1].bodyAsText()).singleOrNull())
    }
}
