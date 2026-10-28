package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.presentation.ACCESS_AUTH
import com.usuallycoding.opensecretsanta.server.presentation.PasswordChangeGate
import com.usuallycoding.opensecretsanta.server.presentation.currentUser
import com.usuallycoding.opensecretsanta.server.security.JwtTokenService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class AuthenticationTest {
    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp() = application {
        module(fx.config)
        routing {
            authenticate(ACCESS_AUTH) {
                get("/test/whoami") {
                    val user = call.currentUser()
                    call.respondText("${user.username}:${user.isAdmin}")
                }
                route("/test/gated") {
                    install(PasswordChangeGate)
                    get { call.respondText("open") }
                }
            }
            get("/test/error/{kind}") {
                throw when (call.parameters["kind"]) {
                    "forbidden" -> DomainException.Forbidden()
                    "not-found" -> DomainException.NotFound()
                    "validation" -> DomainException.ValidationFailed("email", "bad email")
                    "too-many" -> DomainException.TooManyAttempts(42)
                    else -> IllegalStateException("kaboom with internal detail")
                }
            }
        }
    }

    private suspend fun HttpClient.getWith(path: String, token: String?): HttpResponse =
        get(path) { if (token != null) header(HttpHeaders.Authorization, "Bearer $token") }

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    // --- token validity ---

    @Test
    fun validTokenAuthenticatesAsThatUser() = testApplication {
        startApp()
        fx.addUser("alice")

        val response = client.getWith("/test/whoami", fx.token("alice"))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("alice:false", response.bodyAsText())
    }

    @Test
    fun missingTokenIsUnauthorizedWithJsonBody() = testApplication {
        startApp()

        val response = client.getWith("/test/whoami", token = null)

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("unauthorized", error(response.bodyAsText()).code)
    }

    @Test
    fun malformedAndTamperedTokensAreUnauthorized() = testApplication {
        startApp()
        fx.addUser("alice")
        val valid = fx.token("alice")
        val tampered = valid.dropLast(3) + "abc"

        for (token in listOf("garbage", "a.b.c", tampered)) {
            assertEquals(HttpStatusCode.Unauthorized, client.getWith("/test/whoami", token).status, token)
        }
    }

    @Test
    fun expiredTokenIsUnauthorized() = testApplication {
        startApp()
        val alice = fx.addUser("alice")
        val expired = JwtTokenService(fx.config.jwtSecret, Clock { System.currentTimeMillis() }, lifetimeMillis = -1_000)
            .issue(alice, 1)

        assertEquals(HttpStatusCode.Unauthorized, client.getWith("/test/whoami", expired).status)
    }

    @Test
    fun tokenOfAUserThatDoesNotExistIsUnauthorized() = testApplication {
        startApp()
        val ghost = JwtTokenService(fx.config.jwtSecret, Clock { System.currentTimeMillis() })
            .issue(Username.parse("ghost"), 1)

        assertEquals(HttpStatusCode.Unauthorized, client.getWith("/test/whoami", ghost).status)
    }

    @Test
    fun tokenIssuedBeforeACredentialChangeIsRejected() = testApplication {
        startApp()
        val alice = fx.addUser("alice")
        val oldToken = fx.token("alice")
        assertEquals(HttpStatusCode.OK, client.getWith("/test/whoami", oldToken).status)

        fx.users.replaceCredentials(alice, "new-hash", null, mustChangePassword = false)

        assertEquals(HttpStatusCode.Unauthorized, client.getWith("/test/whoami", oldToken).status)
        assertEquals(HttpStatusCode.OK, client.getWith("/test/whoami", fx.token("alice")).status)
    }

    // --- forced password change gate ---

    @Test
    fun gateBlocksUsersWithATemporaryPassword() = testApplication {
        startApp()
        fx.addUser("alice", mustChange = true)

        val response = client.getWith("/test/gated", fx.token("alice"))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("password_change_required", error(response.bodyAsText()).code)
    }

    @Test
    fun routesOutsideTheGateStayOpenToThem() = testApplication {
        startApp()
        fx.addUser("alice", mustChange = true)

        assertEquals(HttpStatusCode.OK, client.getWith("/test/whoami", fx.token("alice")).status)
    }

    @Test
    fun gateIsLiftedAfterThePasswordChanged() = testApplication {
        startApp()
        val alice = fx.addUser("alice", mustChange = true)
        assertEquals(HttpStatusCode.Forbidden, client.getWith("/test/gated", fx.token("alice")).status)

        fx.users.replaceCredentials(alice, "new-hash", null, mustChangePassword = false)

        assertEquals(HttpStatusCode.OK, client.getWith("/test/gated", fx.token("alice")).status)
    }

    @Test
    fun usersWithoutTemporaryPasswordPassTheGate() = testApplication {
        startApp()
        fx.addUser("alice")

        assertEquals("open", client.getWith("/test/gated", fx.token("alice")).bodyAsText())
    }

    // --- error mapping ---

    @Test
    fun domainErrorsMapToStatusCodesAndCodes() = testApplication {
        startApp()
        val cases = mapOf(
            "forbidden" to (HttpStatusCode.Forbidden to "forbidden"),
            "not-found" to (HttpStatusCode.NotFound to "not_found"),
            "validation" to (HttpStatusCode.BadRequest to "validation_failed"),
            "too-many" to (HttpStatusCode.TooManyRequests to "too_many_attempts"),
        )
        for ((kind, expected) in cases) {
            val response = client.get("/test/error/$kind")
            assertEquals(expected.first, response.status, kind)
            assertEquals(expected.second, error(response.bodyAsText()).code, kind)
        }
    }

    @Test
    fun validationErrorsNameTheFieldAndTooManyAttemptsSetsRetryAfter() = testApplication {
        startApp()

        assertEquals("email", error(client.get("/test/error/validation").bodyAsText()).field)
        assertEquals("42", client.get("/test/error/too-many").headers[HttpHeaders.RetryAfter])
    }

    @Test
    fun unexpectedErrorsStayGeneric() = testApplication {
        startApp()

        val response = client.get("/test/error/other")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(response.bodyAsText().contains("kaboom"))
        assertNotNull(error(response.bodyAsText()))
    }
}
