package com.usuallycoding.opensecretsanta.domain.notification

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Username

/** The four kinds of notification a person can switch on or off. */
enum class NotificationCategory { ANNOUNCEMENTS, WISHLIST, QUESTIONS, ANSWERS }

/** Where a registered device runs; iOS has none. */
enum class DevicePlatform {
    ANDROID,
    WEB,
    ;

    companion object {
        /** `android` or `web`, in any case; anything else is refused naming the field. */
        fun of(raw: String?): DevicePlatform =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
                ?: throw DomainException.ValidationFailed("platform", "The platform must be android or web.")
    }
}

/** Which categories a person wants; everything is on until they switch something off. */
data class NotificationSettings(
    val announcements: Boolean = true,
    val wishlist: Boolean = true,
    val questions: Boolean = true,
    val answers: Boolean = true,
) {
    fun allows(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.ANNOUNCEMENTS -> announcements
        NotificationCategory.WISHLIST -> wishlist
        NotificationCategory.QUESTIONS -> questions
        NotificationCategory.ANSWERS -> answers
    }
}

/** A device that has asked to receive [username]'s notifications. */
data class RegisteredDevice(val token: String, val username: Username, val platform: DevicePlatform)

/** Storage for devices. A push token belongs to one person at a time. */
interface DeviceRepository {
    /** Stores the device; a token that was registered to someone else, or again, ends up with [username] once. */
    fun register(username: Username, token: String, platform: DevicePlatform, atMillis: Long)

    /** Removes [token] if [username] is the one it belongs to; any other token is left alone. */
    fun remove(username: Username, token: String)

    /** Removes tokens the push service reported as no longer valid, whoever they belong to. */
    fun removeInvalid(tokens: Collection<String>)

    fun devicesOf(usernames: Collection<Username>): List<RegisteredDevice>
}

/** Storage for each person's four switches. */
interface NotificationSettingsRepository {
    /** The saved switches, or all on for someone who never changed them. */
    fun find(username: Username): NotificationSettings

    fun findAll(usernames: Collection<Username>): Map<Username, NotificationSettings>

    fun save(username: Username, settings: NotificationSettings)
}

/** One data message for one device. The texts are what the person sees; [groupId] and [category] tell the app where a tap leads. */
data class PushMessage(
    val token: String,
    val category: NotificationCategory,
    val groupId: Long,
    val title: String,
    val body: String,
)

/** What became of one message. */
enum class PushResult {
    Sent,

    /** The push service says the token is no longer valid: forget the device. */
    InvalidToken,

    /** Anything else: not delivered, and not tried again. */
    Failed,
}

/**
 * Sends push messages. [send] answers, for each message in the same order, what became of it; ordinary failures (the
 * service unreachable, a refusal, a timeout) are reported as [PushResult.Failed] and do not throw.
 */
interface PushSender {
    /** False when no push service is configured: nothing is built or sent. */
    val enabled: Boolean

    fun send(messages: List<PushMessage>): List<PushResult>
}

/** The sender of a server without push settings. */
object DisabledPushSender : PushSender {
    override val enabled: Boolean = false

    override fun send(messages: List<PushMessage>): List<PushResult> = messages.map { PushResult.Failed }
}
