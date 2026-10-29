package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

/** The person's four switches, and whether the server can send push notifications at all. */
@Serializable
data class NotificationSettingsDto(
    val available: Boolean,
    val announcements: Boolean,
    val wishlist: Boolean,
    val questions: Boolean,
    val answers: Boolean,
)

/** Replaces all four switches; every one must be given. */
@Serializable
data class NotificationSettingsRequest(
    val announcements: Boolean,
    val wishlist: Boolean,
    val questions: Boolean,
    val answers: Boolean,
)

/** Registers a device's push token; [platform] is `android` or `web`. */
@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val platform: String,
)

@Serializable
data class RemoveDeviceRequest(
    val token: String,
)

/** The values of a notification's data message, shared by the server that sends it and the apps that show it. */
object NotificationData {
    const val CATEGORY = "category"
    const val GROUP_ID = "groupId"
    const val TITLE = "title"
    const val BODY = "body"
}
