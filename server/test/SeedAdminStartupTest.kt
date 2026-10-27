package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.LoginResponse
import com.usuallycoding.opensecretsanta.contract.ManagedUserDto
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.config.ConfigurationException
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
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeedAdminStartupTest {
    private val path = newTempDbPath()

    private suspend fun HttpClient.login(username: String, password: String): HttpResponse =
        post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest(username, password)))
        }

    private fun boot(config: com.usuallycoding.opensecretsanta.server.config.ServerConfig, block: suspend HttpClient.() -> Unit) =
        testApplication {
            application { module(config) }
            client.get(ApiRoutes.HEALTH) // forces the application to start
            client.block()
        }

    private fun causes(e: Throwable) = generateSequence(e) { it.cause }

    @Test
    fun firstBootCreatesTheAdminWhoMustChangeTheirPassword() = boot(testConfig(path, adminUsername = "boss", adminPassword = "initial-pass")) {
        val response = login("Boss", "initial-pass")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<LoginResponse>(response.bodyAsText())
        assertTrue(body.mustChangePassword)
        val me = Json.decodeFromString<AccountDto>(
            get(ApiRoutes.ME) { header(HttpHeaders.Authorization, "Bearer ${body.token}") }.bodyAsText(),
        )
        assertEquals(AccountDto("boss", isAdmin = true, mustChangePassword = true), me)
    }

    @Test
    fun theSeededAdminIsGatedUntilThePasswordChanges() = boot(testConfig(path, adminPassword = "initial-pass")) {
        val token = Json.decodeFromString<LoginResponse>(login("admin", "initial-pass").bodyAsText()).token

        val response = get(ApiRoutes.ADMIN_USERS) { header(HttpHeaders.Authorization, "Bearer $token") }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("password_change_required", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    @Test
    fun theDefaultUsernameIsAdmin() = boot(testConfig(path, adminPassword = "initial-pass")) {
        assertEquals(HttpStatusCode.OK, login("admin", "initial-pass").status)
    }

    @Test
    fun laterBootsIgnoreTheVariables() {
        boot(testConfig(path, adminUsername = "boss", adminPassword = "initial-pass")) {}

        boot(testConfig(path, adminUsername = "someone-else", adminPassword = "different-pass")) {
            assertEquals(HttpStatusCode.OK, login("boss", "initial-pass").status)
            assertEquals(HttpStatusCode.Unauthorized, login("boss", "different-pass").status)
            assertEquals(HttpStatusCode.Unauthorized, login("someone-else", "different-pass").status)
        }
    }

    @Test
    fun laterBootsNeedNoAdminPassword() {
        boot(testConfig(path, adminPassword = "initial-pass")) {}

        boot(testConfig(path, adminPassword = null)) {
            assertEquals(HttpStatusCode.OK, login("admin", "initial-pass").status)
        }
    }

    @Test
    fun existingUsersPreventSeeding() {
        val fx = TestFixtures(testConfig(path, adminPassword = "initial-pass"))
        fx.addUser("alice")

        boot(fx.config) {
            assertEquals(HttpStatusCode.Unauthorized, login("admin", "initial-pass").status)
        }
        assertNull(fx.users.find(Username.parse("admin")))
    }

    @Test
    fun emptyDatabaseWithoutAdminPasswordFailsNamingTheVariable() {
        val failure = assertFailsWith<Throwable> { boot(testConfig(path, adminPassword = null)) {} }

        val configuration = causes(failure).filterIsInstance<ConfigurationException>().firstOrNull()
        assertEquals("ADMIN_PASSWORD", configuration?.variable, "cause chain: ${causes(failure).toList()}")
    }

    @Test
    fun weakAdminPasswordFailsNamingTheVariable() {
        val failure = assertFailsWith<Throwable> { boot(testConfig(path, adminPassword = "short")) {} }

        assertEquals("ADMIN_PASSWORD", causes(failure).filterIsInstance<ConfigurationException>().first().variable)
    }

    @Test
    fun theSeededAdminsPasswordIsNeverRetrievable() {
        boot(testConfig(path, adminUsername = "boss", adminPassword = "initial-pass")) {}
        val fx = TestFixtures(testConfig(path))
        fx.addUser("deputy", admin = true)
        fx.addGroup("deputy", "boss")

        boot(testConfig(path)) {
            val view = Json.decodeFromString<ManagedUserDto>(
                get(ApiRoutes.adminUser("boss")) { header(HttpHeaders.Authorization, "Bearer ${fx.token("deputy")}") }.bodyAsText(),
            )
            assertTrue(view.user.mustChangePassword)
            assertNull(view.temporaryPassword)
            assertFalse(fx.users.credentials(Username.parse("boss"))!!.passwordHash.contains("initial-pass"))
        }
    }
}
