package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.LoginResponse
import com.usuallycoding.opensecretsanta.contract.ManagedUserDto
import com.usuallycoding.opensecretsanta.contract.ResetPasswordResponse
import com.usuallycoding.opensecretsanta.domain.user.TempPassword
import com.usuallycoding.opensecretsanta.domain.user.Username
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminRoutesTest {
    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp() = application { module(fx.config) }

    private suspend fun HttpClient.getAs(path: String, token: String?): HttpResponse =
        get(path) { if (token != null) header(HttpHeaders.Authorization, "Bearer $token") }

    private suspend fun HttpClient.postAs(path: String, token: String?): HttpResponse =
        post(path) { if (token != null) header(HttpHeaders.Authorization, "Bearer $token") }

    private suspend fun HttpClient.login(username: String, password: String): HttpResponse =
        post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest(username, password)))
        }

    private suspend fun HttpClient.view(token: String, username: String): ManagedUserDto =
        Json.decodeFromString(getAs(ApiRoutes.adminUser(username), token).bodyAsText())

    private fun error(response: HttpResponse, body: String) = Json.decodeFromString<ErrorResponse>(body)

    /** admin-a owns {alice, shared, carol}; admin-b owns {bob, shared}; outsider is in no group. */
    private fun seedWorld() {
        fx.addUser("admin-a", admin = true)
        fx.addUser("admin-b", admin = true)
        fx.addUser("alice", password = "TempPass0001", mustChange = true, tempPlain = "TempPass0001")
        fx.addUser("bob", password = "TempPass0002", mustChange = true, tempPlain = "TempPass0002")
        fx.addUser("shared", password = "TempPass0003", mustChange = true, tempPlain = "TempPass0003")
        fx.addUser("carol", password = "own-password")
        fx.addUser("outsider")
        fx.addGroup("admin-a", "alice", "shared", "carol")
        fx.addGroup("admin-b", "bob", "shared")
    }

    // --- access control ---

    @Test
    fun anonymousRequestsAreUnauthorized() = testApplication {
        startApp()

        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.ADMIN_USERS, null).status)
    }

    @Test
    fun normalUsersAreForbiddenOnEveryAdminEndpoint() = testApplication {
        startApp()
        seedWorld()
        val token = fx.token("carol")

        assertEquals(HttpStatusCode.Forbidden, client.getAs(ApiRoutes.ADMIN_USERS, token).status)
        assertEquals(HttpStatusCode.Forbidden, client.getAs(ApiRoutes.adminUser("carol"), token).status)
        assertEquals(HttpStatusCode.Forbidden, client.postAs(ApiRoutes.adminUserResetPassword("carol"), token).status)
    }

    @Test
    fun anAdminWithATemporaryPasswordMustChangeItFirst() = testApplication {
        startApp()
        fx.addUser("boss", admin = true, mustChange = true)

        val response = client.getAs(ApiRoutes.ADMIN_USERS, fx.token("boss"))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("password_change_required", error(response, response.bodyAsText()).code)
    }

    // --- list and view ---

    @Test
    fun listIsScopedToOwnGroupsAndSelf() = testApplication {
        startApp()
        seedWorld()

        val list = Json.decodeFromString<List<ManagedUserDto>>(client.getAs(ApiRoutes.ADMIN_USERS, fx.token("admin-a")).bodyAsText())

        assertEquals(listOf("admin-a", "alice", "carol", "shared"), list.map { it.user.username })
    }

    @Test
    fun viewShowsAllFieldsAndTheTemporaryPasswordWhileItIsTemporary() = testApplication {
        startApp()
        seedWorld()

        val view = client.view(fx.token("admin-a"), "alice")

        assertEquals(AccountDto("alice", isAdmin = false, mustChangePassword = true), view.user)
        assertEquals("TempPass0001", view.temporaryPassword)
    }

    @Test
    fun temporaryPasswordIsHiddenOnceTheUserSetTheirOwn() = testApplication {
        startApp()
        seedWorld()

        val view = client.view(fx.token("admin-a"), "carol")

        assertNull(view.temporaryPassword)
        val raw = client.getAs(ApiRoutes.adminUser("carol"), fx.token("admin-a")).bodyAsText()
        assertFalse(raw.contains("own-password") || raw.contains("TempPass"), raw)
    }

    @Test
    fun usersOutsideTheScopeLookNonexistent() = testApplication {
        startApp()
        seedWorld()
        val token = fx.token("admin-a")

        val inOtherGroup = client.getAs(ApiRoutes.adminUser("bob"), token)
        val inNoGroup = client.getAs(ApiRoutes.adminUser("outsider"), token)
        val unknown = client.getAs(ApiRoutes.adminUser("nobody-at-all"), token)

        for (response in listOf(inOtherGroup, inNoGroup, unknown)) assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(unknown.bodyAsText(), inOtherGroup.bodyAsText())
    }

    @Test
    fun aSharedUserIsVisibleToBothOwnersAndAdminsSeeThemselves() = testApplication {
        startApp()
        seedWorld()

        assertEquals("TempPass0003", client.view(fx.token("admin-a"), "shared").temporaryPassword)
        assertEquals("TempPass0003", client.view(fx.token("admin-b"), "SHARED").temporaryPassword)
        assertEquals("admin-a", client.view(fx.token("admin-a"), "admin-a").user.username)
    }

    @Test
    fun anExistingUserNamedOnlyInADraftIsOutOfScopeUntilTheGroupLeavesDraft() = testApplication {
        startApp()
        seedWorld()
        fx.addGroup("admin-a", "outsider", state = "draft", name = "Planning")
        val token = fx.token("admin-a")
        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.adminUser("outsider"), token).status)
        assertEquals(HttpStatusCode.NotFound, client.postAs(ApiRoutes.adminUserResetPassword("outsider"), token).status)

        fx.sql("UPDATE groups SET state = 'pending' WHERE name = 'Planning'")

        assertEquals(HttpStatusCode.OK, client.getAs(ApiRoutes.adminUser("outsider"), token).status)
    }

    // --- reset ---

    @Test
    fun resetIssuesANewTemporaryPasswordWithAllItsEffects() = testApplication {
        startApp()
        seedWorld()
        val adminToken = fx.token("admin-a")
        val oldUserToken = fx.token("carol")
        // lock carol out first, so the reset also has to clear the lock
        repeat(5) { client.login("carol", "wrong") }
        assertEquals(HttpStatusCode.TooManyRequests, client.login("carol", "own-password").status)

        val response = client.postAs(ApiRoutes.adminUserResetPassword("carol"), adminToken)

        assertEquals(HttpStatusCode.OK, response.status)
        val reset = Json.decodeFromString<ResetPasswordResponse>(response.bodyAsText())
        assertEquals("carol", reset.username)
        assertEquals(TempPassword.LENGTH, reset.temporaryPassword.length)
        assertTrue(reset.temporaryPassword.all { it in TempPassword.ALPHABET })
        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.ME, oldUserToken).status)
        val login = client.login("carol", reset.temporaryPassword)
        assertEquals(HttpStatusCode.OK, login.status)
        assertTrue(Json.decodeFromString<LoginResponse>(login.bodyAsText()).mustChangePassword)
        assertEquals(reset.temporaryPassword, client.view(adminToken, "carol").temporaryPassword)
    }

    @Test
    fun theOldPasswordStopsWorkingAfterAReset() = testApplication {
        startApp()
        seedWorld()

        client.postAs(ApiRoutes.adminUserResetPassword("carol"), fx.token("admin-a"))

        assertEquals(HttpStatusCode.Unauthorized, client.login("carol", "own-password").status)
    }

    @Test
    fun resettingSomeoneOutsideTheScopeChangesNothing() = testApplication {
        startApp()
        seedWorld()

        val response = client.postAs(ApiRoutes.adminUserResetPassword("bob"), fx.token("admin-a"))

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(HttpStatusCode.OK, client.login("bob", "TempPass0002").status)
        assertEquals(1, fx.users.find(Username.parse("bob"))!!.tokenVersion)
    }

    // --- no way to make an administrator ---

    @Test
    fun thereIsNoPromoteRoute() = testApplication {
        startApp()
        seedWorld()

        assertEquals(HttpStatusCode.NotFound, client.postAs("/admin/users/outsider/promote", fx.token("admin-a")).status)
        assertFalse(fx.users.find(Username.parse("outsider"))!!.isAdmin)
    }

    @Test
    fun noEndpointRemovesAdminRights() = testApplication {
        startApp()
        seedWorld()

        for (path in listOf("demote", "unpromote", "revoke")) {
            assertEquals(HttpStatusCode.NotFound, client.postAs("/admin/users/admin-b/$path", fx.token("admin-a")).status, path)
        }
        assertTrue(fx.users.find(Username.parse("admin-b"))!!.isAdmin)
        assertNotNull(fx.users.find(Username.parse("admin-b")))
    }
}
