package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.DeliveryInfoDto
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfo
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
import kotlin.test.assertNull

class DeliveryInfoRoutesTest {
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

    private suspend fun HttpClient.read(id: Int, token: String) = send<Unit>("GET", ApiRoutes.groupDeliveryInfo(id.toLong()), token)

    private suspend fun HttpClient.write(id: Int, token: String, info: DeliveryInfoDto) = send("PUT", ApiRoutes.groupDeliveryInfo(id.toLong()), token, info)

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    private fun world(state: String = "pending"): Int {
        fx.addUser("admin", admin = true)
        listOf("alice", "bob", "carol", "dave").forEach { fx.addUser(it) }
        return fx.addGroup("admin", "alice", "bob", "carol", "dave", state = state)
    }

    @Test
    fun aParticipantReadsTheirOwnDetailsAndStartsWithNone() = testApplication {
        startApp()
        val id = world()

        assertEquals(DeliveryInfoDto(), Json.decodeFromString(client.read(id, fx.token("alice")).bodyAsText()))

        fx.deliveryInfos.save(id.toLong(), Username.parse("alice"), DeliveryInfo(address = "1 Main St"))

        assertEquals("1 Main St", Json.decodeFromString<DeliveryInfoDto>(client.read(id, fx.token("alice")).bodyAsText()).address)
    }

    @Test
    fun theDetailsAreSavedAndShownOnTheNextRead() = testApplication {
        startApp()
        val id = world()
        val token = fx.token("alice")

        val response = client.write(id, token, DeliveryInfoDto("Alice B", "a@b.co", "123", "1 Main St", "Ring twice"))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(DeliveryInfoDto("Alice B", "a@b.co", "123", "1 Main St", "Ring twice"), Json.decodeFromString(client.read(id, token).bodyAsText()))
    }

    private val complete = DeliveryInfoDto("Alice B", "a@b.co", "123", "1 Main St", "Ring twice")

    @Test
    fun theDeliveryInformationCanBeClearedAndIsTheOnlyOptionalField() = testApplication {
        startApp()
        val id = world()
        val token = fx.token("alice")
        client.write(id, token, complete)

        val cleared = client.write(id, token, complete.copy(deliveryInfo = "   "))

        assertEquals(HttpStatusCode.OK, cleared.status)
        val now = Json.decodeFromString<DeliveryInfoDto>(client.read(id, token).bodyAsText())
        assertNull(now.deliveryInfo)
        assertEquals("123", now.phone)
    }

    @Test
    fun eachRequiredFieldIsRefusedWhenBlankNamingItAndNothingChanges() = testApplication {
        startApp()
        val id = world()
        val token = fx.token("alice")
        client.write(id, token, complete)
        val blanked = mapOf(
            "fullName" to complete.copy(fullName = null),
            "email" to complete.copy(email = "  "),
            "phone" to complete.copy(phone = ""),
            "address" to complete.copy(address = null),
        )

        for ((field, info) in blanked) {
            val response = client.write(id, token, info)

            assertEquals(HttpStatusCode.BadRequest, response.status, field)
            assertEquals("validation_failed", error(response.bodyAsText()).code, field)
            assertEquals(field, error(response.bodyAsText()).field, field)
        }
        assertEquals(complete, Json.decodeFromString<DeliveryInfoDto>(client.read(id, token).bodyAsText()))
    }

    @Test
    fun incompleteSavedDetailsAreReadableUntilAllFourAreGiven() = testApplication {
        startApp()
        val id = world()
        val token = fx.token("alice")
        fx.deliveryInfos.save(id.toLong(), Username.parse("alice"), DeliveryInfo(fullName = "Alice", address = "Old St"))

        assertEquals("Old St", Json.decodeFromString<DeliveryInfoDto>(client.read(id, token).bodyAsText()).address)
        assertEquals(HttpStatusCode.BadRequest, client.write(id, token, DeliveryInfoDto(fullName = "Alice", address = "New St")).status)
        assertEquals(HttpStatusCode.OK, client.write(id, token, complete).status)
    }

    @Test
    fun anInvalidEmailAndATooLongAddressAreRefusedNamingTheFieldAndNothingChanges() = testApplication {
        startApp()
        val id = world()
        val token = fx.token("alice")
        client.write(id, token, complete.copy(fullName = "Before"))

        val email = client.write(id, token, complete.copy(fullName = "After", email = "not-an-email"))
        val long = client.write(id, token, complete.copy(fullName = "After", address = "x".repeat(501)))

        assertEquals(HttpStatusCode.BadRequest, email.status)
        assertEquals("email", error(email.bodyAsText()).field)
        assertEquals(HttpStatusCode.BadRequest, long.status)
        assertEquals("address", error(long.bodyAsText()).field)
        assertEquals("Before", Json.decodeFromString<DeliveryInfoDto>(client.read(id, token).bodyAsText()).fullName)
    }

    @Test
    fun anArchivedGroupCanBeReadButNotChanged() = testApplication {
        startApp()
        val id = world("archived")
        fx.deliveryInfos.save(id.toLong(), Username.parse("alice"), DeliveryInfo(address = "Kept"))
        val token = fx.token("alice")

        val refused = client.write(id, token, complete.copy(address = "Too late"))

        assertEquals(HttpStatusCode.Conflict, refused.status)
        assertEquals("invalid_state", error(refused.bodyAsText()).code)
        assertEquals("Kept", Json.decodeFromString<DeliveryInfoDto>(client.read(id, token).bodyAsText()).address)
    }

    @Test
    fun anActiveGroupCanBeChanged() = testApplication {
        startApp()
        val id = world("active")

        assertEquals(HttpStatusCode.OK, client.write(id, fx.token("alice"), complete.copy(address = "2 Side St")).status)
    }

    @Test
    fun aDraftAStrangerAndTheOwnerAreNotFound() = testApplication {
        startApp()
        val draft = world("draft")
        fx.addUser("outsider")
        val pending = fx.addGroup("admin", "alice", state = "pending")

        assertEquals(HttpStatusCode.NotFound, client.read(draft, fx.token("alice")).status)
        assertEquals(HttpStatusCode.NotFound, client.write(draft, fx.token("alice"), DeliveryInfoDto()).status)
        assertEquals(HttpStatusCode.NotFound, client.read(pending, fx.token("outsider")).status)
        assertEquals(HttpStatusCode.NotFound, client.read(pending, fx.token("admin")).status)
    }

    @Test
    fun oneGroupDoesNotShowAnothersDetails() = testApplication {
        startApp()
        val first = world()
        val second = fx.addGroup("admin", "alice", "bob", "carol", "dave")
        client.write(first, fx.token("alice"), complete.copy(address = "Home"))

        assertNull(Json.decodeFromString<DeliveryInfoDto>(client.read(second, fx.token("alice")).bodyAsText()).address)
    }

    @Test
    fun onlyTheCallersOwnDetailsChange() = testApplication {
        startApp()
        val id = world()

        client.write(id, fx.token("alice"), complete.copy(fullName = "Alice only"))

        assertNull(Json.decodeFromString<DeliveryInfoDto>(client.read(id, fx.token("bob")).bodyAsText()).fullName)
    }

    @Test
    fun theRoutesWaitForTheTemporaryPasswordToBeReplaced() = testApplication {
        startApp()
        fx.addUser("admin", admin = true)
        fx.addUser("alice", mustChange = true)
        val id = fx.addGroup("admin", "alice", state = "pending")

        val response = client.read(id, fx.token("alice"))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("password_change_required", error(response.bodyAsText()).code)
    }
}
