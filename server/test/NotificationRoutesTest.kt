package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsDto
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsRequest
import com.usuallycoding.opensecretsanta.contract.RegisterDeviceRequest
import com.usuallycoding.opensecretsanta.contract.RemoveDeviceRequest
import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteDeviceRepository
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

/** The notification switches and device registration, through the real routes. */
class NotificationRoutesTest {
    private val fx = TestFixtures().also {
        it.addUser("alice")
        it.addUser("bob")
        it.addUser("temp", mustChange = true)
    }
    private val devices = SqliteDeviceRepository(fx.database)

    private fun ApplicationTestBuilder.startApp(push: Boolean = false) = application {
        module(fx.config, pushSender = if (push) RecordingSender() else null)
    }

    private suspend fun HttpClient.rawSend(method: String, path: String, token: String?, json: String? = null): HttpResponse =
        request(path) {
            this.method = HttpMethod.parse(method)
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            if (json != null) {
                contentType(ContentType.Application.Json)
                setBody(json)
            }
        }

    private suspend inline fun <reified T> HttpClient.send(method: String, path: String, token: String?, body: T): HttpResponse =
        rawSend(method, path, token, Json.encodeToString(body))

    private suspend fun HttpClient.settings(user: String) =
        Json.decodeFromString<NotificationSettingsDto>(rawSend("GET", ApiRoutes.NOTIFICATIONS, fx.token(user)).bodyAsText())

    private suspend fun HttpClient.register(user: String, token: String, platform: String = "android") =
        send("PUT", ApiRoutes.NOTIFICATION_DEVICES, fx.token(user), RegisterDeviceRequest(token, platform))

    private fun tokensOf(user: String) = devices.devicesOf(listOf(Username.parse(user))).map { it.token }

    // --- the switches ---

    @Test
    fun everythingIsOnByDefaultAndPushIsReportedUnavailableWithoutAKey() = testApplication {
        startApp()

        assertEquals(NotificationSettingsDto(available = false, announcements = true, wishlist = true, questions = true, answers = true), client.settings("alice"))
    }

    @Test
    fun pushIsReportedAvailableWhenTheServerCanSend() = testApplication {
        startApp(push = true)

        assertEquals(true, client.settings("alice").available)
    }

    @Test
    fun changingTheSwitchesIsSavedPerPerson() = testApplication {
        startApp()

        val response = client.send("PUT", ApiRoutes.NOTIFICATIONS, fx.token("alice"), NotificationSettingsRequest(announcements = true, wishlist = false, questions = false, answers = true))

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val expected = NotificationSettingsDto(available = false, announcements = true, wishlist = false, questions = false, answers = true)
        assertEquals(expected, Json.decodeFromString<NotificationSettingsDto>(response.bodyAsText()))
        assertEquals(expected, client.settings("alice"))
        assertEquals(true, client.settings("bob").wishlist, "someone else's switches are untouched")
    }

    @Test
    fun savingWorksWithoutAKeyToo() = testApplication {
        startApp(push = false)

        val response = client.send("PUT", ApiRoutes.NOTIFICATIONS, fx.token("alice"), NotificationSettingsRequest(false, false, false, false))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(false, client.settings("alice").announcements)
    }

    @Test
    fun aBodyThatIsNotExactlyFourBooleansIsRefusedAndChangesNothing() = testApplication {
        startApp()
        val bodies = listOf(
            """{"announcements":false,"wishlist":false,"questions":false}""", // one missing
            """{"announcements":false,"wishlist":false,"questions":false,"answers":false,"extra":true}""", // a switch that does not exist
            """{"announcements":"yes","wishlist":false,"questions":false,"answers":false}""", // not a boolean
            """{"announcements":null,"wishlist":false,"questions":false,"answers":false}""",
            "not json",
        )
        for (body in bodies) {
            val response = client.rawSend("PUT", ApiRoutes.NOTIFICATIONS, fx.token("alice"), body)
            assertEquals(HttpStatusCode.BadRequest, response.status, body)
        }
        assertEquals(true, client.settings("alice").announcements)
    }

    // --- devices ---

    @Test
    fun aDeviceIsRegisteredAndRegisteringItAgainKeepsOne() = testApplication {
        startApp()

        assertEquals(HttpStatusCode.NoContent, client.register("alice", "tok-1").status)
        assertEquals(HttpStatusCode.NoContent, client.register("alice", "tok-1").status)
        assertEquals(HttpStatusCode.NoContent, client.register("alice", "tok-2", "WEB").status)

        assertEquals(listOf("tok-1", "tok-2"), tokensOf("alice"))
        assertEquals(listOf(DevicePlatform.ANDROID, DevicePlatform.WEB), devices.devicesOf(listOf(Username.parse("alice"))).map { it.platform })
    }

    @Test
    fun aTokenMovesToTheNextPersonWhoRegistersIt() = testApplication {
        startApp()
        client.register("alice", "shared")

        client.register("bob", "shared")

        assertEquals(emptyList(), tokensOf("alice"))
        assertEquals(listOf("shared"), tokensOf("bob"))
    }

    @Test
    fun aDeviceIsRemovedByItsOwnerOnly() = testApplication {
        startApp()
        client.register("alice", "tok-1")

        val others = client.send("POST", ApiRoutes.NOTIFICATION_DEVICES_REMOVE, fx.token("bob"), RemoveDeviceRequest("tok-1"))
        assertEquals(HttpStatusCode.NoContent, others.status)
        assertEquals(listOf("tok-1"), tokensOf("alice"))

        val own = client.send("POST", ApiRoutes.NOTIFICATION_DEVICES_REMOVE, fx.token("alice"), RemoveDeviceRequest("tok-1"))
        assertEquals(HttpStatusCode.NoContent, own.status)
        assertEquals(emptyList(), tokensOf("alice"))
    }

    @Test
    fun removingAnUnknownTokenIsQuietlyFine() = testApplication {
        startApp()

        assertEquals(HttpStatusCode.NoContent, client.send("POST", ApiRoutes.NOTIFICATION_DEVICES_REMOVE, fx.token("alice"), RemoveDeviceRequest("never-registered")).status)
    }

    @Test
    fun anUnknownPlatformOrABadTokenIsRefused() = testApplication {
        startApp()

        val platform = client.register("alice", "tok-1", "ios")
        assertEquals(HttpStatusCode.BadRequest, platform.status)
        assertEquals("platform", Json.decodeFromString<ErrorResponse>(platform.bodyAsText()).field)

        for (bad in listOf("", "   ", "has space", "x".repeat(5000))) {
            val response = client.register("alice", bad)
            assertEquals(HttpStatusCode.BadRequest, response.status, bad.take(20))
            assertEquals("token", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).field)
        }
        assertEquals(emptyList(), tokensOf("alice"))
    }

    // --- who may ---

    @Test
    fun everyRouteNeedsASignIn() = testApplication {
        startApp()
        val calls = listOf(
            "GET" to ApiRoutes.NOTIFICATIONS,
            "PUT" to ApiRoutes.NOTIFICATIONS,
            "PUT" to ApiRoutes.NOTIFICATION_DEVICES,
            "POST" to ApiRoutes.NOTIFICATION_DEVICES_REMOVE,
        )
        for ((method, path) in calls) {
            assertEquals(HttpStatusCode.Unauthorized, client.rawSend(method, path, token = null, json = "{}").status, "$method $path")
        }
        assertEquals(HttpStatusCode.Unauthorized, client.rawSend("GET", ApiRoutes.NOTIFICATIONS, token = "garbage").status)
    }

    @Test
    fun aTemporaryPasswordMustBeChangedFirst() = testApplication {
        startApp()

        assertEquals(HttpStatusCode.Forbidden, client.rawSend("GET", ApiRoutes.NOTIFICATIONS, fx.token("temp")).status)
        assertEquals(HttpStatusCode.Forbidden, client.register("temp", "tok-1").status)
        assertEquals(emptyList(), tokensOf("temp"))
    }
}
