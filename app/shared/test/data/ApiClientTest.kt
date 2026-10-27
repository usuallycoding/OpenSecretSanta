package com.usuallycoding.opensecretsanta.data

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.AuthContext
import com.usuallycoding.opensecretsanta.data.api.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAuth(override var token: String? = null) : AuthContext {
    val rejections = mutableListOf<ApiError>()
    override fun rejected(error: ApiError) {
        rejections += error
    }
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String, extra: Map<String, String> = emptyMap()): HttpResponseData =
    respond(
        content = body,
        status = status,
        headers = io.ktor.http.Headers.build {
            append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            extra.forEach { (k, v) -> append(k, v) }
        },
    )

class ApiClientTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()

    private fun client(auth: FakeAuth = FakeAuth(), handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): ApiClient {
        val engine = MockEngine { request ->
            requests += request
            bodies += request.body.toByteArray().decodeToString()
            handler(request)
        }
        return ApiClient(createHttpClient(engine), "https://api.example.org/", auth)
    }

    private fun error(code: String, message: String = "Something", field: String? = null) =
        """{"code":"$code","message":"$message"${if (field != null) ""","field":"$field"""" else ""}}"""

    private val account = """{"username":"alice","isAdmin":false,"mustChangePassword":false}"""

    @Test
    fun loginPostsCredentialsWithoutAnyToken() = runTest {
        val api = client(FakeAuth(token = "stale")) { json(HttpStatusCode.OK, """{"token":"t1","mustChangePassword":true}""") }

        val result = api.login("Alice", "secret-pass")

        assertEquals(ApiResult.Success(com.usuallycoding.opensecretsanta.contract.LoginResponse("t1", true)), result)
        assertEquals(HttpMethod.Post, requests.single().method)
        assertEquals("https://api.example.org${ApiRoutes.LOGIN}", requests.single().url.toString())
        assertNull(requests.single().headers[HttpHeaders.Authorization])
        assertTrue("\"Alice\"" in bodies.single() && "secret-pass" in bodies.single())
    }

    @Test
    fun theAccountIsReadWithTheBearerToken() = runTest {
        val api = client(FakeAuth(token = "t1")) { json(HttpStatusCode.OK, account) }

        val result = api.account()

        assertTrue(result is ApiResult.Success)
        assertEquals("alice", result.value.username)
        assertEquals("Bearer t1", requests.single().headers[HttpHeaders.Authorization])
        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("", bodies.single())
    }

    @Test
    fun noTokenMeansNoAuthorizationHeader() = runTest {
        val api = client(FakeAuth(token = null)) { json(HttpStatusCode.OK, account) }

        api.account()

        assertNull(requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun changingThePasswordPutsBothPasswordsWithTheToken() = runTest {
        val api = client(FakeAuth(token = "t1")) { json(HttpStatusCode.OK, """{"token":"t2"}""") }

        val result = api.changePassword("old-password", "new-password-1")

        assertEquals("t2", (result as ApiResult.Success).value.token)
        assertEquals(HttpMethod.Put, requests.single().method)
        assertEquals("Bearer t1", requests.single().headers[HttpHeaders.Authorization])
        assertTrue("old-password" in bodies.single() && "new-password-1" in bodies.single())
    }

    @Test
    fun thePasswordIsNeverSentByTheAccountCall() = runTest {
        val api = client(FakeAuth(token = "t1")) { json(HttpStatusCode.OK, account) }

        api.account()

        assertFalse("password" in bodies.single().lowercase())
    }

    @Test
    fun aWrongPasswordIsUnauthorizedAndDoesNotEndASession() = runTest {
        val auth = FakeAuth(token = null)
        val api = client(auth) { json(HttpStatusCode.Unauthorized, error("invalid_credentials")) }

        assertEquals(ApiResult.Failure(ApiError.Unauthorized), api.login("alice", "nope"))
        assertTrue(auth.rejections.isEmpty())
    }

    @Test
    fun aRejectedTokenIsReportedToTheSession() = runTest {
        val auth = FakeAuth(token = "old")
        val api = client(auth) { json(HttpStatusCode.Unauthorized, error("unauthorized")) }

        assertEquals(ApiResult.Failure(ApiError.Unauthorized), api.account())
        assertEquals(listOf<ApiError>(ApiError.Unauthorized), auth.rejections)
    }

    @Test
    fun aBareUnauthorizedWithoutABodyStillCounts() = runTest {
        val auth = FakeAuth(token = "old")
        val api = client(auth) { respond("", HttpStatusCode.Unauthorized) }

        assertEquals(ApiResult.Failure(ApiError.Unauthorized), api.account())
        assertEquals(1, auth.rejections.size)
    }

    @Test
    fun theTemporaryPasswordGateIsRecognisedAndReported() = runTest {
        val auth = FakeAuth(token = "t1")
        val api = client(auth) { json(HttpStatusCode.Forbidden, error("password_change_required")) }

        assertEquals(ApiResult.Failure(ApiError.PasswordChangeRequired), api.account())
        assertEquals(listOf<ApiError>(ApiError.PasswordChangeRequired), auth.rejections)
    }

    @Test
    fun otherForbiddenAnswersAreJustRejected() = runTest {
        val auth = FakeAuth(token = "t1")
        val api = client(auth) { json(HttpStatusCode.Forbidden, error("forbidden", "Nope")) }

        assertEquals(ApiResult.Failure(ApiError.Rejected(403, "forbidden", "Nope")), api.account())
        assertTrue(auth.rejections.isEmpty())
    }

    @Test
    fun aValidationErrorKeepsTheFieldAndMessage() = runTest {
        val api = client(FakeAuth("t1")) { json(HttpStatusCode.BadRequest, error("validation_failed", "Too short.", field = "newPassword")) }

        assertEquals(ApiResult.Failure(ApiError.Validation("newPassword", "Too short.")), api.changePassword("a", "b"))
    }

    @Test
    fun aLockoutCarriesTheRetryAfterSeconds() = runTest {
        val api = client { json(HttpStatusCode.TooManyRequests, error("too_many_attempts"), mapOf(HttpHeaders.RetryAfter to "900")) }

        assertEquals(ApiResult.Failure(ApiError.RateLimited(900)), api.login("alice", "x"))
    }

    @Test
    fun aLockoutWithoutAUsableRetryAfterHasNoSeconds() = runTest {
        val noHeader = client { json(HttpStatusCode.TooManyRequests, error("too_many_attempts")) }
        val badHeader = client { json(HttpStatusCode.TooManyRequests, error("too_many_attempts"), mapOf(HttpHeaders.RetryAfter to "soon")) }

        assertEquals(ApiResult.Failure(ApiError.RateLimited(null)), noHeader.login("alice", "x"))
        assertEquals(ApiResult.Failure(ApiError.RateLimited(null)), badHeader.login("alice", "x"))
    }

    @Test
    fun otherErrorBodiesAreRejectedWithTheirCode() = runTest {
        val api = client(FakeAuth("t1")) { json(HttpStatusCode.Conflict, error("invalid_state", "Not now.")) }

        assertEquals(ApiResult.Failure(ApiError.Rejected(409, "invalid_state", "Not now.")), api.account())
    }

    @Test
    fun anUnreachableServerIsANetworkError() = runTest {
        val api = client { throw RuntimeException("connection refused") }

        assertEquals(ApiResult.Failure(ApiError.Network), api.login("alice", "x"))
    }

    @Test
    fun anUnparsableAnswerIsUnexpected() = runTest {
        val badSuccess = client { json(HttpStatusCode.OK, "not json at all") }
        val badError = client { respond("<html>Bad gateway</html>", HttpStatusCode.BadGateway) }
        val wrongShape = client { json(HttpStatusCode.OK, """{"unexpected":1}""") }

        assertEquals(ApiResult.Failure(ApiError.Unexpected), badSuccess.login("alice", "x"))
        assertEquals(ApiResult.Failure(ApiError.Unexpected), badError.login("alice", "x"))
        assertEquals(ApiResult.Failure(ApiError.Unexpected), wrongShape.login("alice", "x"))
    }

    @Test
    fun unknownFieldsInAnAnswerAreIgnored() = runTest {
        val api = client { json(HttpStatusCode.OK, """{"token":"t1","mustChangePassword":false,"extra":"x"}""") }

        assertTrue(api.login("alice", "x") is ApiResult.Success)
    }

    @Test
    fun theBaseUrlMayEndWithASlash() = runTest {
        val api = client { json(HttpStatusCode.OK, account) }

        api.account()

        assertEquals("https://api.example.org/me", requests.single().url.toString())
    }
}
