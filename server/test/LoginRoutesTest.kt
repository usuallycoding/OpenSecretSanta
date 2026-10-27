package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.LoginResponse
import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.security.JwtTokenService
import com.usuallycoding.opensecretsanta.server.security.SystemClock
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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
import kotlin.test.assertNotNull

class LoginRoutesTest {
    private class TestClock(var now: Long = System.currentTimeMillis()) : Clock {
        override fun nowMillis() = now
    }

    private val fx = TestFixtures()
    private val clock = TestClock()

    private fun ApplicationTestBuilder.startApp() = application { module(fx.config, clock = clock) }

    private suspend fun HttpClient.login(username: String, password: String): HttpResponse =
        post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest(username, password)))
        }

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    @Test
    fun successReturnsTokenAndMustChangeState() = testApplication {
        startApp()
        fx.addUser("alice", password = "correct-horse", mustChange = true)

        val response = client.login("alice", "correct-horse")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<LoginResponse>(response.bodyAsText())
        assertEquals(true, body.mustChangePassword)
        val claims = assertNotNull(JwtTokenService(fx.config.jwtSecret, SystemClock).verify(body.token))
        assertEquals(Username.parse("alice"), claims.username)
        assertEquals(1, claims.tokenVersion)
    }

    @Test
    fun usernameCaseIsIgnored() = testApplication {
        startApp()
        fx.addUser("alice", password = "correct-horse")

        assertEquals(HttpStatusCode.OK, client.login("Alice", "correct-horse").status)
    }

    @Test
    fun wrongPasswordAndUnknownUserAreIndistinguishable() = testApplication {
        startApp()
        fx.addUser("alice", password = "correct-horse")

        val wrong = client.login("alice", "wrong-password")
        val unknown = client.login("nobody", "wrong-password")

        assertEquals(HttpStatusCode.Unauthorized, wrong.status)
        assertEquals(HttpStatusCode.Unauthorized, unknown.status)
        assertEquals(wrong.bodyAsText(), unknown.bodyAsText())
        assertEquals("invalid_credentials", error(wrong.bodyAsText()).code)
    }

    @Test
    fun malformedBodyIsABadRequest() = testApplication {
        startApp()

        val notJson = client.post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody("{not json")
        }
        val missingField = client.post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, notJson.status)
        assertEquals(HttpStatusCode.BadRequest, missingField.status)
        assertEquals("validation_failed", error(missingField.bodyAsText()).code)
    }

    // --- lockout ---

    @Test
    fun sixthAttemptIsLockedEvenWithTheCorrectPassword() = testApplication {
        startApp()
        fx.addUser("alice", password = "correct-horse")
        repeat(5) { assertEquals(HttpStatusCode.Unauthorized, client.login("alice", "wrong").status) }

        val locked = client.login("alice", "correct-horse")

        assertEquals(HttpStatusCode.TooManyRequests, locked.status)
        assertEquals("900", locked.headers[HttpHeaders.RetryAfter])
        assertEquals("too_many_attempts", error(locked.bodyAsText()).code)
    }

    @Test
    fun unknownUsernameIsLockedWithTheSameResponse() = testApplication {
        startApp()
        fx.addUser("alice", password = "correct-horse")
        repeat(5) { client.login("ghost-user", "wrong") }
        repeat(5) { client.login("alice", "wrong") }

        val ghost = client.login("ghost-user", "wrong")
        val alice = client.login("alice", "correct-horse")

        assertEquals(HttpStatusCode.TooManyRequests, ghost.status)
        assertEquals(ghost.status, alice.status)
        assertEquals(ghost.bodyAsText(), alice.bodyAsText())
        assertEquals(ghost.headers[HttpHeaders.RetryAfter], alice.headers[HttpHeaders.RetryAfter])
    }

    @Test
    fun lockExpiresAfterFifteenMinutes() = testApplication {
        startApp()
        fx.addUser("alice", password = "correct-horse")
        repeat(5) { client.login("alice", "wrong") }
        assertEquals(HttpStatusCode.TooManyRequests, client.login("alice", "correct-horse").status)

        clock.now += 15 * 60 * 1000

        assertEquals(HttpStatusCode.OK, client.login("alice", "correct-horse").status)
    }

    @Test
    fun successfulLoginResetsTheFailureCount() = testApplication {
        startApp()
        fx.addUser("alice", password = "correct-horse")
        repeat(4) { client.login("alice", "wrong") }
        assertEquals(HttpStatusCode.OK, client.login("alice", "correct-horse").status)

        repeat(4) { assertEquals(HttpStatusCode.Unauthorized, client.login("alice", "wrong").status) }

        assertEquals(HttpStatusCode.OK, client.login("alice", "correct-horse").status)
    }

    @Test
    fun lockoutSurvivesARestartBecauseItIsStoredInTheDatabase() {
        testApplication {
            startApp()
            fx.addUser("alice", password = "correct-horse")
            repeat(5) { client.login("alice", "wrong") }
        }
        testApplication {
            startApp()
            assertEquals(HttpStatusCode.TooManyRequests, client.login("alice", "correct-horse").status)
        }
    }

    // --- rate limit ---

    @Test
    fun twentyFirstRequestInAMinuteIsRateLimited() = testApplication {
        startApp()
        fx.addUser("alice", password = "correct-horse")

        repeat(20) { assertEquals(HttpStatusCode.OK, client.login("alice", "correct-horse").status, "request ${it + 1}") }
        val limited = client.login("alice", "correct-horse")

        assertEquals(HttpStatusCode.TooManyRequests, limited.status)
        assertNotNull(limited.headers[HttpHeaders.RetryAfter])
        assertEquals("too_many_attempts", error(limited.bodyAsText()).code)
    }

    @Test
    fun otherEndpointsAreNotRateLimited() = testApplication {
        startApp()

        repeat(30) { assertEquals(HttpStatusCode.OK, client.get(ApiRoutes.HEALTH).status, "request ${it + 1}") }
    }
}
