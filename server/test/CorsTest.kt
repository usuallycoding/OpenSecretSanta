package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CorsTest {
    private val listed = "https://app.example.org"

    private fun fixtures(vararg origins: String) = TestFixtures(testConfig().copy(corsAllowedOrigins = origins.toList()))

    @Test
    fun aPreflightFromAListedOriginIsAllowedWithoutAToken() = testApplication {
        val fx = fixtures(listed, "http://localhost:8081")
        application { module(fx.config) }

        val response = client.request(ApiRoutes.ME_PASSWORD) {
            method = HttpMethod.Options
            header(HttpHeaders.Origin, listed)
            header(HttpHeaders.AccessControlRequestMethod, "PUT")
            header(HttpHeaders.AccessControlRequestHeaders, "authorization, content-type")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listed, response.headers[HttpHeaders.AccessControlAllowOrigin])
        assertTrue("PUT" in response.headers[HttpHeaders.AccessControlAllowMethods].orEmpty())
        val allowedHeaders = response.headers[HttpHeaders.AccessControlAllowHeaders].orEmpty().lowercase()
        assertTrue("authorization" in allowedHeaders && "content-type" in allowedHeaders, allowedHeaders)
        assertNull(response.headers[HttpHeaders.AccessControlAllowCredentials])
    }

    @Test
    fun anOriginWithAPortIsMatchedExactly() = testApplication {
        val fx = fixtures("http://localhost:8081")
        application { module(fx.config) }

        val allowed = client.request(ApiRoutes.ME) { header(HttpHeaders.Origin, "http://localhost:8081") }
        val otherPort = client.request(ApiRoutes.ME) { header(HttpHeaders.Origin, "http://localhost:9999") }
        val otherScheme = client.request(ApiRoutes.ME) { header(HttpHeaders.Origin, "https://localhost:8081") }

        assertEquals("http://localhost:8081", allowed.headers[HttpHeaders.AccessControlAllowOrigin])
        assertNull(otherPort.headers[HttpHeaders.AccessControlAllowOrigin])
        assertNull(otherScheme.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun anUnlistedOriginGetsNoAllowHeaders() = testApplication {
        val fx = fixtures(listed)
        application { module(fx.config) }

        val preflight = client.request(ApiRoutes.ME_PASSWORD) {
            method = HttpMethod.Options
            header(HttpHeaders.Origin, "https://evil.example.org")
            header(HttpHeaders.AccessControlRequestMethod, "PUT")
        }
        val simple = client.request(ApiRoutes.HEALTH) { header(HttpHeaders.Origin, "https://evil.example.org") }

        assertNull(preflight.headers[HttpHeaders.AccessControlAllowOrigin])
        assertNull(simple.headers[HttpHeaders.AccessControlAllowOrigin])
        assertEquals(HttpStatusCode.Forbidden, simple.status) // the plugin also refuses the request itself
    }

    @Test
    fun withoutConfigurationNoCrossOriginHeadersAreSent() = testApplication {
        val fx = fixtures()
        application { module(fx.config) }

        val simple = client.request(ApiRoutes.HEALTH) { header(HttpHeaders.Origin, listed) }
        val preflight = client.request(ApiRoutes.ME_PASSWORD) {
            method = HttpMethod.Options
            header(HttpHeaders.Origin, listed)
            header(HttpHeaders.AccessControlRequestMethod, "PUT")
        }

        assertEquals(HttpStatusCode.OK, simple.status)
        for (response in listOf(simple, preflight)) {
            assertNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
            assertNull(response.headers[HttpHeaders.AccessControlAllowMethods])
        }
    }

    @Test
    fun authenticationIsUnaffectedForAListedOrigin() = testApplication {
        val fx = fixtures(listed)
        application { module(fx.config) }

        val response = client.request(ApiRoutes.ME) { header(HttpHeaders.Origin, listed) }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(listed, response.headers[HttpHeaders.AccessControlAllowOrigin]) // so the browser can read the 401
    }

    @Test
    fun theLoginRateLimitStillAppliesToListedOrigins() = testApplication {
        val fx = fixtures(listed)
        application { module(fx.config) }

        val statuses = (1..25).map {
            client.request(ApiRoutes.LOGIN) {
                method = HttpMethod.Post
                header(HttpHeaders.Origin, listed)
                contentType(ContentType.Application.Json)
                setBody("""{"username":"nobody","password":"wrong-password"}""")
            }.status
        }

        assertTrue(HttpStatusCode.TooManyRequests in statuses, statuses.toString())
    }

    @Test
    fun aSameOriginRequestIsNotBlockedWhenOtherOriginsAreListed() = testApplication {
        val fx = fixtures(listed)
        application { module(fx.config) }

        val response = client.request(ApiRoutes.LOGIN) {
            method = HttpMethod.Post
            header(HttpHeaders.Origin, "http://localhost") // the test host itself
            contentType(ContentType.Application.Json)
            setBody("""{"username":"nobody","password":"wrong-password"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
