package com.usuallycoding.opensecretsanta.contract

import com.usuallycoding.opensecretsanta.domain.notification.PushTargets
import kotlinx.serialization.Serializable

/**
 * The person's two switches, and whether the server can send push notifications at all. [wishlist], [questions], [answers] and
 * [letters] are what apps from before the daily digest read: each is reported with the value of [daily], and has no default so
 * that it is always sent.
 */
@Serializable
data class NotificationSettingsDto(
    val available: Boolean,
    val announcements: Boolean,
    val daily: Boolean = true,
    val wishlist: Boolean,
    val questions: Boolean,
    val answers: Boolean,
    val letters: Boolean,
)

/**
 * Replaces both switches. [daily] is the switch; an app from before the daily digest gives the four older ones instead
 * ([wishlist], [questions], [answers] and, optionally, [letters]) and the digest is on when any of them is on.
 */
@Serializable
data class NotificationSettingsRequest(
    val announcements: Boolean,
    val daily: Boolean? = null,
    val wishlist: Boolean? = null,
    val questions: Boolean? = null,
    val answers: Boolean? = null,
    val letters: Boolean? = null,
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

    /** The category of the daily digest, which is about no group. */
    const val CATEGORY_DAILY = "daily"

    /** Present when the category alone does not say where a tap leads; its value is [TARGET_MESSAGES] for an announcement from the owner. */
    const val TARGET = "target"
    const val TARGET_MESSAGES = PushTargets.MESSAGES
}
