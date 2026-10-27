package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ChangePasswordRequest
import com.usuallycoding.opensecretsanta.contract.ChangePasswordResponse
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.ProfileUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRoutesTest {
    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp() = application { module(fx.config) }

    private suspend inline fun <reified T> HttpClient.putJson(path: String, token: String, body: T): HttpResponse =
        put(path) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(body))
        }

    private suspend fun HttpClient.getAs(path: String, token: String?): HttpResponse =
        get(path) { if (token != null) header(HttpHeaders.Authorization, "Bearer $token") }

    private suspend fun HttpClient.login(username: String, password: String): HttpResponse =
        post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest(username, password)))
        }

    private suspend fun HttpClient.me(token: String) = Json.decodeFromString<AccountDto>(getAs(ApiRoutes.ME, token).bodyAsText())

    private fun error(response: String) = Json.decodeFromString<ErrorResponse>(response)

    // --- GET /me ---

    @Test
    fun getMeReturnsOwnAccount() = testApplication {
        startApp()
        fx.addUser("alice", admin = true)
        fx.users.updateProfile(
            com.usuallycoding.opensecretsanta.domain.user.Username.parse("alice"),
            com.usuallycoding.opensecretsanta.domain.user.Profile("Alice B", "a@b.co", "123", "1 Main St", "Ring twice"),
        )

        val me = client.me(fx.token("alice"))

        assertEquals(AccountDto("alice", "Alice B", "a@b.co", "123", "1 Main St", "Ring twice", isAdmin = true, mustChangePassword = false), me)
    }

    @Test
    fun getMeNeverExposesCredentials() = testApplication {
        startApp()
        fx.addUser("alice", password = "secret-password", mustChange = true, tempPlain = "TempPass0001")

        val body = client.getAs(ApiRoutes.ME, fx.token("alice")).bodyAsText()

        for (forbidden in listOf("hash", "secret-password", "TempPass0001", "\$2a\$")) assertFalse(body.contains(forbidden), forbidden)
    }

    @Test
    fun getMeRequiresAuthenticationButWorksWithATemporaryPassword() = testApplication {
        startApp()
        fx.addUser("alice", mustChange = true)

        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.ME, null).status)
        assertTrue(client.me(fx.token("alice")).mustChangePassword)
    }

    // --- PUT /me ---

    @Test
    fun profileUpdateIsVisibleOnTheNextRead() = testApplication {
        startApp()
        fx.addUser("alice")
        val token = fx.token("alice")

        val response = client.putJson(ApiRoutes.ME, token, ProfileUpdateRequest(fullName = "Alice B", email = "a@b.co", phone = "123", address = "1 Main St", deliveryInfo = "Ring twice"))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Alice B", client.me(token).fullName)
        assertEquals("Ring twice", client.me(token).deliveryInfo)
    }

    @Test
    fun nullOrBlankFieldsAreCleared() = testApplication {
        startApp()
        fx.addUser("alice")
        val token = fx.token("alice")
        client.putJson(ApiRoutes.ME, token, ProfileUpdateRequest(fullName = "Alice", phone = "123"))

        client.putJson(ApiRoutes.ME, token, ProfileUpdateRequest(fullName = "Alice", phone = "   "))

        assertNull(client.me(token).phone)
        assertEquals("Alice", client.me(token).fullName)
    }

    @Test
    fun invalidEmailIsRejectedAndProfileUnchanged() = testApplication {
        startApp()
        fx.addUser("alice")
        val token = fx.token("alice")
        client.putJson(ApiRoutes.ME, token, ProfileUpdateRequest(fullName = "Before"))

        val response = client.putJson(ApiRoutes.ME, token, ProfileUpdateRequest(fullName = "After", email = "not-an-email"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("email", error(response.bodyAsText()).field)
        assertEquals("Before", client.me(token).fullName)
    }

    @Test
    fun usernameChangeIsRejectedButSameUsernameIsFine() = testApplication {
        startApp()
        fx.addUser("alice")
        val token = fx.token("alice")

        val rejected = client.putJson(ApiRoutes.ME, token, ProfileUpdateRequest(username = "mallory", fullName = "X"))
        val accepted = client.putJson(ApiRoutes.ME, token, ProfileUpdateRequest(username = "Alice", fullName = "Y"))

        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals("username", error(rejected.bodyAsText()).field)
        assertEquals(HttpStatusCode.OK, accepted.status)
        assertEquals("alice", client.me(token).username)
    }

    @Test
    fun updatingTouchesOnlyTheCallersRecord() = testApplication {
        startApp()
        fx.addUser("alice")
        fx.addUser("bob")

        client.putJson(ApiRoutes.ME, fx.token("alice"), ProfileUpdateRequest(fullName = "Alice only"))

        assertNull(client.me(fx.token("bob")).fullName)
    }

    @Test
    fun profileEditIsBlockedWhileThePasswordIsTemporary() = testApplication {
        startApp()
        fx.addUser("alice", mustChange = true)

        val response = client.putJson(ApiRoutes.ME, fx.token("alice"), ProfileUpdateRequest(fullName = "X"))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("password_change_required", error(response.bodyAsText()).code)
    }

    // --- PUT /me/password ---

    @Test
    fun passwordChangeReturnsNewTokenAndInvalidatesTheOldOne() = testApplication {
        startApp()
        fx.addUser("alice", password = "old-password")
        val oldToken = fx.token("alice")

        val response = client.putJson(ApiRoutes.ME_PASSWORD, oldToken, ChangePasswordRequest("old-password", "brand-new-password"))

        assertEquals(HttpStatusCode.OK, response.status)
        val newToken = Json.decodeFromString<ChangePasswordResponse>(response.bodyAsText()).token
        assertNotEquals(oldToken, newToken)
        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.ME, oldToken).status)
        assertEquals(HttpStatusCode.OK, client.getAs(ApiRoutes.ME, newToken).status)
        assertEquals(HttpStatusCode.OK, client.login("alice", "brand-new-password").status)
        assertEquals(HttpStatusCode.Unauthorized, client.login("alice", "old-password").status)
    }

    @Test
    fun changingATemporaryPasswordDiscardsItAndLiftsTheGate() = testApplication {
        startApp()
        val alice = fx.addUser("alice", password = "TempPass0001", mustChange = true, tempPlain = "TempPass0001")

        val response = client.putJson(ApiRoutes.ME_PASSWORD, fx.token("alice"), ChangePasswordRequest("TempPass0001", "my-own-password"))

        assertEquals(HttpStatusCode.OK, response.status)
        assertNull(fx.users.credentials(alice)!!.encryptedTempPassword)
        val newToken = Json.decodeFromString<ChangePasswordResponse>(response.bodyAsText()).token
        assertFalse(client.me(newToken).mustChangePassword)
        assertEquals(HttpStatusCode.OK, client.putJson(ApiRoutes.ME, newToken, ProfileUpdateRequest(fullName = "Now allowed")).status)
    }

    @Test
    fun wrongCurrentPasswordIsForbiddenAndNothingChanges() = testApplication {
        startApp()
        fx.addUser("alice", password = "old-password")
        val token = fx.token("alice")

        val response = client.putJson(ApiRoutes.ME_PASSWORD, token, ChangePasswordRequest("not-it", "brand-new-password"))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(HttpStatusCode.OK, client.getAs(ApiRoutes.ME, token).status)
        assertEquals(HttpStatusCode.OK, client.login("alice", "old-password").status)
    }

    @Test
    fun shortOrUnchangedPasswordIsRejected() = testApplication {
        startApp()
        fx.addUser("alice", password = "old-password")
        val token = fx.token("alice")

        val short = client.putJson(ApiRoutes.ME_PASSWORD, token, ChangePasswordRequest("old-password", "short"))
        val same = client.putJson(ApiRoutes.ME_PASSWORD, token, ChangePasswordRequest("old-password", "old-password"))

        assertEquals(HttpStatusCode.BadRequest, short.status)
        assertEquals(HttpStatusCode.BadRequest, same.status)
        assertEquals("newPassword", error(short.bodyAsText()).field)
        assertEquals(HttpStatusCode.OK, client.login("alice", "old-password").status)
    }

    @Test
    fun passwordChangeRequiresAuthentication() = testApplication {
        startApp()

        val response = client.put(ApiRoutes.ME_PASSWORD) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(ChangePasswordRequest("a", "b")))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
