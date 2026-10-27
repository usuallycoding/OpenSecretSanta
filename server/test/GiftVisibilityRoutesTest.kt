package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.ReceiverDto
import com.usuallycoding.opensecretsanta.contract.SantaRevealDto
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GiftVisibilityRoutesTest {
    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp() = application { module(fx.config) }

    private suspend fun HttpClient.getAs(path: String, token: String?): HttpResponse =
        get(path) { if (token != null) header(HttpHeaders.Authorization, "Bearer $token") }

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    private val names = listOf("alice", "bob", "carol", "dave")

    /** alice -> bob -> carol -> dave -> alice in a group in [state]. */
    private fun world(state: String): Int {
        fx.addUser("admin-a", admin = true)
        for (name in names) fx.addUser(name)
        fx.users.updateProfile(Username.parse("bob"), Profile("Bob Builder", "bob@example.com", "555-0102", "2 Side St", "Leave at the back door"))
        fx.users.updateProfile(Username.parse("alice"), Profile(fullName = "Alice Archer", address = "1 Main St", phone = "555-0101"))
        val id = fx.addGroup("admin-a", *names.toTypedArray(), state = state)
        if (state == "active" || state == "archived") fx.assign(id, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        return id
    }

    // --- receiver ---

    @Test
    fun aSantaReadsTheirReceiversDetailsInAnActiveGroup() = testApplication {
        startApp()
        val id = world("active")

        val response = client.getAs(ApiRoutes.groupReceiver(id.toLong()), fx.token("alice"))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            ReceiverDto("bob", "Bob Builder", "2 Side St", "555-0102", "Leave at the back door"),
            Json.decodeFromString<ReceiverDto>(response.bodyAsText()),
        )
    }

    @Test
    fun everyoneGetsTheirOwnReceiver() = testApplication {
        startApp()
        val id = world("active")

        val receivers = names.map { Json.decodeFromString<ReceiverDto>(client.getAs(ApiRoutes.groupReceiver(id.toLong()), fx.token(it)).bodyAsText()).username }

        assertEquals(listOf("bob", "carol", "dave", "alice"), receivers)
    }

    @Test
    fun unsetFieldsAreEmptyAndThereIsNoEmailOrCredentialData() = testApplication {
        startApp()
        val id = world("active")

        val body = client.getAs(ApiRoutes.groupReceiver(id.toLong()), fx.token("dave")).bodyAsText() // dave -> alice (no delivery info)

        val keys = Json.parseToJsonElement(body).jsonObject.keys
        assertEquals(setOf("username", "fullName", "address", "phone", "deliveryInfo"), keys)
        assertEquals(null, Json.decodeFromString<ReceiverDto>(body).deliveryInfo)
        for (forbidden in listOf("bob@example.com", "email", "hash", "password", "isAdmin")) assertFalse(body.contains(forbidden, ignoreCase = true), forbidden)
    }

    @Test
    fun theDetailsAreLive() = testApplication {
        startApp()
        val id = world("active")
        fx.users.updateProfile(Username.parse("bob"), Profile(fullName = "Bob Builder", address = "99 New Road"))

        val receiver = Json.decodeFromString<ReceiverDto>(client.getAs(ApiRoutes.groupReceiver(id.toLong()), fx.token("alice")).bodyAsText())

        assertEquals("99 New Road", receiver.address)
        assertEquals(null, receiver.phone)
    }

    @Test
    fun theReceiverIsStillReadableAfterArchive() = testApplication {
        startApp()
        val id = world("archived")

        assertEquals(HttpStatusCode.OK, client.getAs(ApiRoutes.groupReceiver(id.toLong()), fx.token("alice")).status)
    }

    @Test
    fun thereIsNoReceiverBeforeActivation() = testApplication {
        startApp()
        val id = world("pending")

        val response = client.getAs(ApiRoutes.groupReceiver(id.toLong()), fx.token("alice"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("invalid_state", error(response.bodyAsText()).code)
    }

    @Test
    fun draftsStrangersTheNonParticipatingOwnerAndUnknownGroupsAreNotFound() = testApplication {
        startApp()
        val draft = world("draft")
        val active = fx.addGroup("admin-a", *names.toTypedArray(), state = "active", name = "Second").also {
            fx.assign(it, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        }
        fx.addUser("outsider")

        val attempts = listOf(
            client.getAs(ApiRoutes.groupReceiver(draft.toLong()), fx.token("alice")),
            client.getAs(ApiRoutes.groupReceiver(active.toLong()), fx.token("outsider")),
            client.getAs(ApiRoutes.groupReceiver(active.toLong()), fx.token("admin-a")),
            client.getAs(ApiRoutes.groupReceiver(9999), fx.token("alice")),
            client.getAs("/groups/nope/receiver", fx.token("alice")),
        )

        for (response in attempts) assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun authenticationAndThePasswordChangeGateApply() = testApplication {
        startApp()
        val id = world("active")
        fx.addUser("newbie", mustChange = true)

        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.groupReceiver(id.toLong()), null).status)
        val gated = client.getAs(ApiRoutes.groupReceiver(id.toLong()), fx.token("newbie"))
        assertEquals(HttpStatusCode.Forbidden, gated.status)
        assertEquals("password_change_required", error(gated.bodyAsText()).code)
    }

    // --- Santa reveal ---

    @Test
    fun afterArchiveTheSantaIsRevealedWithUsernameAndFullNameOnly() = testApplication {
        startApp()
        val id = world("archived")

        val response = client.getAs(ApiRoutes.groupSanta(id.toLong()), fx.token("bob")) // bob's Santa is alice

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(SantaRevealDto("alice", "Alice Archer"), Json.decodeFromString<SantaRevealDto>(response.bodyAsText()))
        val body = response.bodyAsText()
        assertEquals(setOf("username", "fullName"), Json.parseToJsonElement(body).jsonObject.keys)
        for (forbidden in listOf("1 Main St", "555-0101", "address", "phone")) assertFalse(body.contains(forbidden), forbidden)
    }

    @Test
    fun everyoneLearnsOnlyTheirOwnSanta() = testApplication {
        startApp()
        val id = world("archived")

        val santas = names.map { Json.decodeFromString<SantaRevealDto>(client.getAs(ApiRoutes.groupSanta(id.toLong()), fx.token(it)).bodyAsText()).username }

        assertEquals(listOf("dave", "alice", "bob", "carol"), santas)
    }

    @Test
    fun theSantaStaysHiddenWhileTheGroupIsActiveAndTheRefusalNamesNobody() = testApplication {
        startApp()
        val id = world("active")

        for (name in names) {
            val response = client.getAs(ApiRoutes.groupSanta(id.toLong()), fx.token(name))
            assertEquals(HttpStatusCode.Conflict, response.status, name)
            assertEquals("invalid_state", error(response.bodyAsText()).code)
            for (other in names) assertFalse(response.bodyAsText().contains(other), "refusal for $name mentions $other: ${response.bodyAsText()}")
        }
    }

    @Test
    fun theRevealIsRefusedToStrangersAndBeforeActivation() = testApplication {
        startApp()
        val pending = world("pending")
        fx.addUser("outsider")
        val archived = fx.addGroup("admin-a", *names.toTypedArray(), state = "archived", name = "Done").also {
            fx.assign(it, "alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice")
        }

        assertEquals(HttpStatusCode.Conflict, client.getAs(ApiRoutes.groupSanta(pending.toLong()), fx.token("alice")).status)
        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.groupSanta(archived.toLong()), fx.token("outsider")).status)
        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.groupSanta(archived.toLong()), fx.token("admin-a")).status)
    }
}
