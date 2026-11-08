package com.usuallycoding.opensecretsanta.contract

import com.usuallycoding.opensecretsanta.domain.notification.PushTargets
import kotlinx.serialization.Serializable

/** The person's five switches, and whether the server can send push notifications at all. */
@Serializable
data class NotificationSettingsDto(
    val available: Boolean,
    val announcements: Boolean,
    val wishlist: Boolean,
    val questions: Boolean,
    val answers: Boolean,
    /** Letters from the person's receiver; on when an older server says nothing about it. */
    val letters: Boolean = true,
)

/** Replaces all five switches; the first four must be given, and letters is on when an older client leaves it out. */
@Serializable
data class NotificationSettingsRequest(
    val announcements: Boolean,
    val wishlist: Boolean,
    val questions: Boolean,
    val answers: Boolean,
    val letters: Boolean = true,
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

    /** Present when the category alone does not say where a tap leads; its value is [TARGET_MESSAGES] for an announcement from the owner. */
    const val TARGET = "target"
    const val TARGET_MESSAGES = PushTargets.MESSAGES
}
