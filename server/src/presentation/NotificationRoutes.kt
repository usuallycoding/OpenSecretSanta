package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsDto
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsRequest
import com.usuallycoding.opensecretsanta.contract.RegisterDeviceRequest
import com.usuallycoding.opensecretsanta.contract.RemoveDeviceRequest
import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.notification.GetNotificationSettings
import com.usuallycoding.opensecretsanta.domain.notification.NotificationSettings
import com.usuallycoding.opensecretsanta.domain.notification.NotificationState
import com.usuallycoding.opensecretsanta.domain.notification.RegisterDevice
import com.usuallycoding.opensecretsanta.domain.notification.UnregisterDevice
import com.usuallycoding.opensecretsanta.domain.notification.UpdateNotificationSettings
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

private fun NotificationState.toDto() = NotificationSettingsDto(
    available = available,
    announcements = settings.announcements,
    wishlist = settings.wishlist,
    questions = settings.questions,
    answers = settings.answers,
)

/** A person's four notification switches and their devices. */
fun Route.notificationRoutes(
    getSettings: GetNotificationSettings,
    updateSettings: UpdateNotificationSettings,
    registerDevice: RegisterDevice,
    unregisterDevice: UnregisterDevice,
) {
    get(ApiRoutes.NOTIFICATIONS) {
        val user = call.currentUser()
        call.respond(blocking { getSettings(user) }.toDto())
    }
    put(ApiRoutes.NOTIFICATIONS) {
        val user = call.currentUser()
        val request = call.receive<NotificationSettingsRequest>()
        val settings = NotificationSettings(request.announcements, request.wishlist, request.questions, request.answers)
        call.respond(blocking { updateSettings(user, settings) }.toDto())
    }
    put(ApiRoutes.NOTIFICATION_DEVICES) {
        val user = call.currentUser()
        val request = call.receive<RegisterDeviceRequest>()
        blocking { registerDevice(user, request.token, DevicePlatform.of(request.platform)) }
        call.respond(HttpStatusCode.NoContent)
    }
    post(ApiRoutes.NOTIFICATION_DEVICES_REMOVE) {
        val user = call.currentUser()
        val request = call.receive<RemoveDeviceRequest>()
        blocking { unregisterDevice(user, request.token) }
        call.respond(HttpStatusCode.NoContent)
    }
}
