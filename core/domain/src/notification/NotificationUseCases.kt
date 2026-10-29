package com.usuallycoding.opensecretsanta.domain.notification

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.User

/** The most a push token can be; real ones are about 160 characters, so this only refuses nonsense. */
const val MAX_PUSH_TOKEN_LENGTH = 4096

/** The person's four switches, and whether the server can send push at all. */
data class NotificationState(val available: Boolean, val settings: NotificationSettings)

class GetNotificationSettings(
    private val settings: NotificationSettingsRepository,
    private val sender: PushSender,
) {
    operator fun invoke(user: User): NotificationState = NotificationState(sender.enabled, settings.find(user.username))
}

class UpdateNotificationSettings(
    private val settings: NotificationSettingsRepository,
    private val sender: PushSender,
) {
    operator fun invoke(user: User, new: NotificationSettings): NotificationState {
        settings.save(user.username, new)
        return NotificationState(sender.enabled, new)
    }
}

class RegisterDevice(
    private val devices: DeviceRepository,
    private val clock: Clock,
) {
    operator fun invoke(user: User, token: String, platform: DevicePlatform) {
        devices.register(user.username, cleanToken(token), platform, clock.nowMillis())
    }
}

class UnregisterDevice(private val devices: DeviceRepository) {
    operator fun invoke(user: User, token: String) {
        devices.remove(user.username, cleanToken(token))
    }
}

private fun cleanToken(token: String): String {
    val clean = token.trim()
    if (clean.isEmpty() || clean.length > MAX_PUSH_TOKEN_LENGTH || clean.any { it.isWhitespace() }) {
        throw DomainException.ValidationFailed("token", "The push token is not valid.")
    }
    return clean
}
