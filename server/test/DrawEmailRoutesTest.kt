package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.domain.email.EmailMessage
import com.usuallycoding.opensecretsanta.domain.email.Mailer
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The emails of a draw, through the activation route, with a stand-in mailer. */
class DrawEmailRoutesTest {
    private class FakeMailer(private val accepts: (EmailMessage) -> Boolean = { true }) : Mailer {
        override val enabled: Boolean = true
        val sent = mutableListOf<EmailMessage>()
        var calls = 0
        var throwing = false

        override fun send(messages: List<EmailMessage>): List<Boolean> {
            calls++
            if (throwing) throw IllegalStateException("the mail service exploded")
            sent += messages
            return messages.map(accepts)
        }
    }

    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp(mailer: Mailer? = null) = application { module(fx.config, mailer = mailer) }

    private suspend fun HttpClient.send(method: String, path: String, token: String?, body: CreateGroupRequest? = null): HttpResponse =
        request(path) {
            this.method = HttpMethod.parse(method)
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(body))
            }
        }

    /** A Pending group of these people (each with an account) owned by the administrator. */
    private suspend fun HttpClient.pending(vararg names: String): AdminGroupDto {
        val created = send("POST", ApiRoutes.ADMIN_GROUPS, fx.token("admin"), CreateGroupRequest("Party", description = "Bring snacks", location = "HQ", participants = names.toList()))
        val group = Json.decodeFromString<AdminGroupDto>(created.bodyAsText())
        assertEquals(HttpStatusCode.OK, send("POST", ApiRoutes.adminGroupPending(group.id), fx.token("admin")).status)
        return group
    }

    private suspend fun HttpClient.activate(id: Long) = send("POST", ApiRoutes.adminGroupActivate(id), fx.token("admin"))

    private suspend fun HttpClient.assignments(id: Long): Map<String, String> =
        Json.decodeFromString<List<AssignmentDto>>(send("GET", ApiRoutes.adminGroupAssignments(id), fx.token("admin")).bodyAsText())
            .associate { it.santa to it.receiver }

    private fun setup(vararg withEmail: String, without: List<String> = emptyList()) {
        fx.addUser("admin", admin = true)
        (withEmail.toList() + without).forEach { fx.addUser(it) }
        withEmail.forEach { fx.users.updateProfile(Username.parse(it), Profile(email = "$it@example.org")) }
    }

    private fun state(id: Long) = fx.database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT state FROM groups WHERE id = $id").use { it.next(); it.getString(1) } }
    }

    @Test
    fun theActivationEmailsEveryoneTheirOwnReceiverAndSaysSo() = testApplication {
        val mailer = FakeMailer()
        startApp(mailer)
        setup("alice", "bob", "carol", "dave")
        val group = client.pending("alice", "bob", "carol", "dave")

        val response = client.activate(group.id)

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val change = Json.decodeFromString<GroupChangeResponse>(response.bodyAsText())
        assertEquals("active", change.group.state)
        assertEquals(EmailSummaryDto(sent = 4, failed = 0, noAddress = 0), change.emails)
        val draw = client.assignments(group.id)
        for ((santa, receiver) in draw) {
            val email = mailer.sent.single { it.to == "$santa@example.org" }
            assertTrue("You are Secret Santa for: $receiver" in email.text, santa)
            assertTrue("Bring snacks" in email.text)
            assertTrue("Where: HQ" in email.text)
        }
    }

    @Test
    fun failedEmailsAndPeopleWithoutAnAddressAreCounted() = testApplication {
        val mailer = FakeMailer(accepts = { it.to != "bob@example.org" })
        startApp(mailer)
        setup("alice", "bob", "carol", without = listOf("dave", "erin"))
        val group = client.pending("alice", "bob", "carol", "dave", "erin")

        val change = Json.decodeFromString<GroupChangeResponse>(client.activate(group.id).bodyAsText())

        assertEquals(EmailSummaryDto(sent = 2, failed = 1, noAddress = 2), change.emails)
    }

    @Test
    fun aServiceThatCannotBeReachedLeavesTheGroupActiveAndCountsEveryEmailFailed() = testApplication {
        val mailer = FakeMailer(accepts = { false })
        startApp(mailer)
        setup("alice", "bob", "carol", "dave")
        val group = client.pending("alice", "bob", "carol", "dave")

        val response = client.activate(group.id)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(EmailSummaryDto(sent = 0, failed = 4, noAddress = 0), Json.decodeFromString<GroupChangeResponse>(response.bodyAsText()).emails)
        assertEquals("active", state(group.id))
        assertEquals(4, client.assignments(group.id).size)
    }

    @Test
    fun aMailerThatExplodesStillLeavesTheGroupActiveWithItsDraw() = testApplication {
        val mailer = FakeMailer().apply { throwing = true }
        startApp(mailer)
        setup("alice", "bob", "carol", "dave")
        val group = client.pending("alice", "bob", "carol", "dave")

        val response = client.activate(group.id)

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val body = response.bodyAsText()
        assertFalse("exploded" in body)
        assertEquals(4, Json.decodeFromString<GroupChangeResponse>(body).emails!!.failed)
        assertEquals("active", state(group.id))
        assertEquals(4, client.assignments(group.id).size)
    }

    @Test
    fun aRefusedActivationSendsNothing() = testApplication {
        val mailer = FakeMailer()
        startApp(mailer)
        setup("alice", "bob", "carol")
        val group = client.pending("alice", "bob", "carol")

        val response = client.activate(group.id)

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(0, mailer.calls)
        assertEquals("pending", state(group.id))
    }

    @Test
    fun aSecondActivationIsRefusedAndSendsNoSecondRound() = testApplication {
        val mailer = FakeMailer()
        startApp(mailer)
        setup("alice", "bob", "carol", "dave")
        val group = client.pending("alice", "bob", "carol", "dave")
        client.activate(group.id)
        assertEquals(1, mailer.calls)

        val again = client.activate(group.id)

        assertEquals(HttpStatusCode.Conflict, again.status)
        assertEquals(1, mailer.calls)
        assertEquals(4, mailer.sent.size)
    }

    @Test
    fun withoutEmailSettingsTheGroupIsDrawnAndTheAnswerHasNoSummary() = testApplication {
        startApp() // the fixture's configuration has no email settings
        setup("alice", "bob", "carol", "dave")
        val group = client.pending("alice", "bob", "carol", "dave")

        val response = client.activate(group.id)

        assertEquals(HttpStatusCode.OK, response.status)
        assertNull(Json.decodeFromString<GroupChangeResponse>(response.bodyAsText()).emails)
        assertFalse("\"emails\":{" in response.bodyAsText())
        assertEquals("active", state(group.id))
    }

    @Test
    fun otherChangesNeverCarryAnEmailSummary() = testApplication {
        val mailer = FakeMailer()
        startApp(mailer)
        setup("alice", "bob", "carol", "dave")
        val group = client.pending("alice", "bob", "carol", "dave")
        client.activate(group.id)

        val archive = client.send("POST", ApiRoutes.adminGroupArchive(group.id), fx.token("admin"))

        assertEquals(HttpStatusCode.OK, archive.status)
        assertNull(Json.decodeFromString<GroupChangeResponse>(archive.bodyAsText()).emails)
        assertEquals(1, mailer.calls)
    }
}
