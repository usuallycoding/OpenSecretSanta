package com.usuallycoding.opensecretsanta.data.push

/** What asking a device to receive push notifications came to. */
sealed interface PushEnable {
    /** Allowed, and this is the device's push token. */
    data class Ready(val token: String) : PushEnable

    /** The person refused the permission, now or before; only the device's settings can change that. */
    data object Denied : PushEnable

    /** The permission is fine but no token could be had (no network, no push service on the device). */
    data object Failed : PushEnable
}

/**
 * What a platform provides for push: Android's Firebase messaging, the browser's service worker. The person is only ever
 * asked for the permission by [enable], never by anything else.
 */
interface PushDevice {
    /** How the server names this platform when a token is registered: `android` or `web`. */
    val platform: String

    /** False where push cannot work at all (iOS, a browser without notifications). */
    val isSupported: Boolean

    /** Whether the platform has the feature at all, so that the menu offers it; a browser that cannot do it still offers it, to say why. */
    val isOffered: Boolean get() = true

    /** Whether the permission is granted right now, asking nothing. */
    suspend fun isPermitted(): Boolean

    /** Asks for the permission if it has not been decided, and gives the push token once it is granted. */
    suspend fun enable(): PushEnable

    /** The push token, without asking for anything; null when not permitted or none can be had. */
    suspend fun currentToken(): String?

    /** Throws this device's push token away, so that nothing more reaches it. */
    suspend fun disable()
}

/** The device of a platform without push. */
object UnsupportedPushDevice : PushDevice {
    override val platform: String = "none"
    override val isSupported: Boolean = false
    override val isOffered: Boolean = false

    override suspend fun isPermitted(): Boolean = false
    override suspend fun enable(): PushEnable = PushEnable.Failed
    override suspend fun currentToken(): String? = null
    override suspend fun disable() = Unit
}
