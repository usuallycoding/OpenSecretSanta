package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.NotificationSettingsRequest
import com.usuallycoding.opensecretsanta.contract.RegisterDeviceRequest
import com.usuallycoding.opensecretsanta.contract.RemoveDeviceRequest
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** The notification calls against a server that behaves like the real one (see [FakeNotifications]). */
class NotificationApiTest {
    private val server = ScriptedServer().also { it.session() }
    private val api get() = server.api

    @Test
    fun theSettingsAreReadWithTheAvailabilityOfPush() = runTest {
        server.notifications.available = false

        val settings = (api.notificationSettings() as ApiResult.Success).value

        assertEquals(false, settings.available)
        assertEquals(listOf(true, true, true, true), listOf(settings.announcements, settings.wishlist, settings.questions, settings.answers))
        assertEquals(listOf("GET /notifications"), server.calls)
    }

    @Test
    fun savingSendsAllFourSwitchesAndGivesBackWhatTheServerSaved() = runTest {
        val request = NotificationSettingsRequest(announcements = true, wishlist = false, questions = true, answers = false)

        val saved = (api.saveNotificationSettings(request) as ApiResult.Success).value

        assertEquals(request, server.notifications.settings)
        assertEquals(false, saved.wishlist)
        assertEquals("PUT /notifications", server.calls.last())
        assertEquals(request, Json.decodeFromString(server.bodies.last()))
    }

    @Test
    fun aDeviceIsRegisteredWithItsTokenAndPlatformAndRemovedAgain() = runTest {
        assertIs<ApiResult.Success<Unit>>(api.registerDevice("tok-1", "android"))
        assertEquals(mapOf("tok-1" to "android"), server.notifications.devices)
        assertEquals(RegisterDeviceRequest("tok-1", "android"), Json.decodeFromString(server.bodies.last()))
        assertEquals("PUT /notifications/devices", server.calls.last())

        assertIs<ApiResult.Success<Unit>>(api.removeDevice("tok-1"))
        assertEquals(emptyMap(), server.notifications.devices)
        assertEquals(RemoveDeviceRequest("tok-1"), Json.decodeFromString(server.bodies.last()))
        assertEquals("POST /notifications/devices/remove", server.calls.last())
    }

    @Test
    fun aRefusedSaveIsAFailureWithTheServersReason() = runTest {
        server.notifications.failNextSaveWith(HttpStatusCode.InternalServerError)

        val result = api.saveNotificationSettings(NotificationSettingsRequest(false, false, false, false))

        assertIs<ApiResult.Failure>(result)
        assertIs<ApiError.Rejected>(result.error)
    }

    @Test
    fun anUnreachableServerIsANetworkFailure() = runTest {
        server.unreachable = true

        assertEquals(ApiResult.Failure(ApiError.Network), api.notificationSettings())
        assertEquals(ApiResult.Failure(ApiError.Network), api.registerDevice("tok-1", "web"))
    }
}
