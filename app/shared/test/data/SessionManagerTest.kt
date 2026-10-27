package com.usuallycoding.opensecretsanta.data

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.createHttpClient
import com.usuallycoding.opensecretsanta.data.session.Account
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import com.usuallycoding.opensecretsanta.data.session.SessionState
import com.usuallycoding.opensecretsanta.data.session.SignedOutReason
import com.usuallycoding.opensecretsanta.data.session.TokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeStore(var stored: String? = null, var failing: Boolean = false) : TokenStore {
    override suspend fun read(): String? {
        if (failing) throw IllegalStateException("keystore unavailable")
        return stored
    }

    override suspend fun write(token: String) {
        if (failing) throw IllegalStateException("keystore unavailable")
        stored = token
    }

    override suspend fun clear() {
        if (failing) throw IllegalStateException("keystore unavailable")
        stored = null
    }
}

/** A tiny server: tokens map to accounts, and every request records what it was called with. */
private class FakeServer {
    var accountFor: (String?) -> String? = { null } // bearer token -> /me body, or null for 401
    var loginAnswer: Pair<HttpStatusCode, String> = HttpStatusCode.OK to """{"token":"t1","mustChangePassword":false}"""
    var passwordAnswer: Pair<HttpStatusCode, String> = HttpStatusCode.OK to """{"token":"t2"}"""
    var gateOnAccount = false
    var unreachable = false
    val calls = mutableListOf<String>()

    private val json = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    val engine = MockEngine { request ->
        calls += "${request.method.value} ${request.url.encodedPath} ${request.headers[HttpHeaders.Authorization].orEmpty()}".trim()
        if (unreachable) throw RuntimeException("down")
        when (request.url.encodedPath) {
            ApiRoutes.LOGIN -> respond(loginAnswer.second, loginAnswer.first, json)
            ApiRoutes.ME_PASSWORD -> respond(passwordAnswer.second, passwordAnswer.first, json)
            ApiRoutes.ME -> {
                val token = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
                val body = accountFor(token)
                when {
                    gateOnAccount -> respond("""{"code":"password_change_required","message":"x"}""", HttpStatusCode.Forbidden, json)
                    body == null -> respond("""{"code":"unauthorized","message":"x"}""", HttpStatusCode.Unauthorized, json)
                    else -> respond(body, HttpStatusCode.OK, json)
                }
            }
            else -> respond("", HttpStatusCode.NotFound)
        }
    }
}

private fun account(name: String = "alice", admin: Boolean = false, must: Boolean = false) =
    """{"username":"$name","fullName":"Full $name","isAdmin":$admin,"mustChangePassword":$must}"""

class SessionManagerTest {
    private val server = FakeServer()
    private val store = FakeStore()
    private lateinit var session: SessionManager

    private fun newSession(): SessionManager {
        lateinit var created: SessionManager
        val api = ApiClient(createHttpClient(server.engine), "https://api.example.org", object : com.usuallycoding.opensecretsanta.data.api.AuthContext {
            override val token get() = created.token
            override fun rejected(error: ApiError) = created.rejected(error)
        })
        created = SessionManager(lazyOf(api), store, CoroutineScope(Dispatchers.Unconfined))
        session = created
        return created
    }

    private val alice = Account("alice", "Full alice", isAdmin = false)

    @Test
    fun startsLoading() {
        assertEquals(SessionState.Loading, newSession().state.value)
    }

    @Test
    fun noStoredTokenMeansSignedOutWithoutAskingTheServer() = runTest {
        newSession().restore()

        assertEquals(SessionState.SignedOut(), session.state.value)
        assertTrue(server.calls.isEmpty())
    }

    @Test
    fun aStoredTokenThatTheServerAcceptsSignsThePersonBackIn() = runTest {
        store.stored = "saved"
        server.accountFor = { if (it == "saved") account() else null }

        newSession().restore()

        assertEquals(SessionState.SignedIn(alice), session.state.value)
        assertEquals("saved", session.token)
        assertEquals(listOf("GET /me Bearer saved"), server.calls)
    }

    @Test
    fun aStoredTokenWithATemporaryPasswordGoesToThePasswordChange() = runTest {
        store.stored = "saved"
        server.accountFor = { account(must = true) }

        newSession().restore()

        assertEquals(SessionState.PasswordChangeRequired(alice), session.state.value)
    }

    @Test
    fun aStaleStoredTokenIsDeletedAndTheSignInScreenIsShownWithANote() = runTest {
        store.stored = "stale"
        server.accountFor = { null }

        newSession().restore()

        assertEquals(SessionState.SignedOut(SignedOutReason.SessionEnded), session.state.value)
        assertNull(store.stored)
        assertNull(session.token)
    }

    @Test
    fun anUnreachableServerAtStartLeavesTheStoredTokenForNextTime() = runTest {
        store.stored = "saved"
        server.unreachable = true

        newSession().restore()

        assertEquals(SessionState.SignedOut(), session.state.value)
        assertEquals("saved", store.stored)
        assertNull(session.token)
    }

    @Test
    fun aStoreThatFailsToReadIsJustNoStoredToken() = runTest {
        store.failing = true

        newSession().restore()

        assertEquals(SessionState.SignedOut(), session.state.value)
    }

    @Test
    fun signingInStoresTheTokenAndReadsTheAccount() = runTest {
        server.accountFor = { if (it == "t1") account("alice", admin = true) else null }

        val result = newSession().signIn("alice", "password1")

        assertEquals(ApiResult.Success(Unit), result)
        assertEquals(SessionState.SignedIn(Account("alice", "Full alice", isAdmin = true)), session.state.value)
        assertEquals("t1", store.stored)
        assertEquals("t1", session.token)
    }

    @Test
    fun signingInWithATemporaryPasswordGoesToThePasswordChange() = runTest {
        server.loginAnswer = HttpStatusCode.OK to """{"token":"t1","mustChangePassword":true}"""
        server.accountFor = { account(must = true) }

        newSession().signIn("alice", "TempPass0001")

        assertEquals(SessionState.PasswordChangeRequired(alice), session.state.value)
    }

    @Test
    fun aWrongPasswordChangesNothing() = runTest {
        server.loginAnswer = HttpStatusCode.Unauthorized to """{"code":"invalid_credentials","message":"x"}"""
        newSession().restore()

        val result = session.signIn("alice", "nope")

        assertEquals(ApiResult.Failure(ApiError.Unauthorized), result)
        assertEquals(SessionState.SignedOut(), session.state.value) // no "signed out" note for a plain wrong password
        assertNull(store.stored)
    }

    @Test
    fun aFailingStoreDoesNotStopASignIn() = runTest {
        store.failing = true
        server.accountFor = { if (it == "t1") account() else null }

        newSession().signIn("alice", "password1")

        assertEquals(SessionState.SignedIn(alice), session.state.value)
        assertEquals("t1", session.token)
    }

    @Test
    fun changingThePasswordSwapsTheTokenAndSignsIn() = runTest {
        server.loginAnswer = HttpStatusCode.OK to """{"token":"t1","mustChangePassword":true}"""
        server.accountFor = { token -> if (token == "t2") account() else account(must = true) }
        newSession().signIn("alice", "TempPass0001")

        val result = session.changePassword("TempPass0001", "new-password-1")

        assertEquals(ApiResult.Success(Unit), result)
        assertEquals(SessionState.SignedIn(alice), session.state.value)
        assertEquals("t2", session.token)
        assertEquals("t2", store.stored)
    }

    @Test
    fun aRefusedPasswordChangeKeepsTheOldTokenAndState() = runTest {
        server.loginAnswer = HttpStatusCode.OK to """{"token":"t1","mustChangePassword":true}"""
        server.accountFor = { account(must = true) }
        newSession().signIn("alice", "TempPass0001")
        server.passwordAnswer = HttpStatusCode.BadRequest to """{"code":"validation_failed","message":"Too short.","field":"newPassword"}"""

        val result = session.changePassword("TempPass0001", "short")

        assertEquals(ApiResult.Failure(ApiError.Validation("newPassword", "Too short.")), result)
        assertEquals("t1", session.token)
        assertEquals(SessionState.PasswordChangeRequired(alice), session.state.value)
    }

    @Test
    fun signingOutClearsMemoryAndTheStoreAndShowsSignIn() = runTest {
        server.accountFor = { account() }
        newSession().signIn("alice", "password1")

        session.signOut()

        assertEquals(SessionState.SignedOut(), session.state.value)
        assertNull(session.token)
        assertNull(store.stored)
    }

    @Test
    fun theNextPersonSeesNothingOfThePreviousOne() = runTest {
        server.accountFor = { token -> if (token == "t1") account("alice") else account("bob") }
        newSession().signIn("alice", "password1")
        session.signOut()
        server.loginAnswer = HttpStatusCode.OK to """{"token":"t9","mustChangePassword":false}"""

        session.signIn("bob", "password2")

        assertEquals(SessionState.SignedIn(Account("bob", "Full bob", false)), session.state.value)
        assertEquals("t9", store.stored)
    }

    @Test
    fun anyRejectedRequestEndsTheSessionWithANote() = runTest {
        server.accountFor = { token -> if (token == "t1") account() else null }
        newSession().signIn("alice", "password1")
        server.accountFor = { null } // e.g. the password was reset on another device

        session.rejected(ApiError.Unauthorized)

        assertEquals(SessionState.SignedOut(SignedOutReason.SessionEnded), session.state.value)
        assertNull(store.stored)
    }

    @Test
    fun theGateHitLaterMovesASignedInPersonToThePasswordChange() = runTest {
        server.accountFor = { account() }
        newSession().signIn("alice", "password1")

        session.rejected(ApiError.PasswordChangeRequired)

        assertEquals(SessionState.PasswordChangeRequired(alice), session.state.value)
        assertEquals("t1", session.token) // still the same token: only the screen changes
    }

    @Test
    fun aFailingStoreDoesNotBlockSigningOut() = runTest {
        server.accountFor = { account() }
        newSession().signIn("alice", "password1")
        store.failing = true

        session.signOut()

        assertEquals(SessionState.SignedOut(), session.state.value)
        assertNull(session.token)
    }

    @Test
    fun theHttpMethodsUsedAreTheDocumentedOnes() = runTest {
        server.accountFor = { account() }
        newSession().signIn("alice", "password1")

        assertEquals(listOf("POST /auth/login", "GET /me Bearer t1"), server.calls)
        assertEquals(HttpMethod.Post.value, "POST")
    }
}
