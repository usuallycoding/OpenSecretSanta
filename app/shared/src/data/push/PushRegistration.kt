package com.usuallycoding.opensecretsanta.data.push

import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.session.SignOutHook
import com.usuallycoding.opensecretsanta.data.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** How turning notifications on for this device went. */
sealed interface TurnOn {
    data object Done : TurnOn

    /** The person refused the permission. */
    data object Denied : TurnOn

    /** No token could be had, or the server did not take it ([error] is the server's side when it was that). */
    data class Failed(val error: ApiError? = null) : TurnOn
}

/**
 * The master switch of this device. It is on only while the person asked for it, here, and the device still allows it;
 * the server holds the token while it is on. The choice is remembered on the device, and forgotten when the person signs
 * out, so that the next person on a shared device starts with it off and never gets someone else's notifications.
 */
class PushRegistration(
    private val device: PushDevice,
    private val api: Lazy<ApiClient>,
    private val settings: SettingsStore,
) : SignOutHook {
    val isSupported: Boolean get() = device.isSupported

    /** Whether the menu offers notifications on this platform at all. */
    val isOffered: Boolean get() = device.isOffered

    private fun wanted(): Boolean = settings.get(KEY) == TRUE

    private fun remember(on: Boolean) = settings.put(KEY, if (on) TRUE else FALSE)

    /** On only while it was asked for and the device still allows it. */
    suspend fun isOn(): Boolean = isSupported && wanted() && safely(false) { device.isPermitted() }

    suspend fun turnOn(): TurnOn {
        if (!isSupported) return TurnOn.Failed()
        return when (val enabled = safely<PushEnable>(PushEnable.Failed) { device.enable() }) {
            PushEnable.Denied -> TurnOn.Denied
            PushEnable.Failed -> TurnOn.Failed()
            is PushEnable.Ready -> when (val registered = api.value.registerDevice(enabled.token, device.platform)) {
                is ApiResult.Success -> {
                    remember(true)
                    TurnOn.Done
                }
                is ApiResult.Failure -> TurnOn.Failed(registered.error)
            }
        }
    }

    /**
     * Turns it off. The device's token is thrown away whatever the server says, which is what stops the notifications; the
     * server is told as well so that it forgets the token at once.
     */
    suspend fun turnOff() {
        remember(false)
        val token = safely<String?>(null) { device.currentToken() }
        if (token != null) safely(Unit) { api.value.removeDevice(token) }
        safely(Unit) { device.disable() }
    }

    /** At start, and when the device's token changes: tells the server about the token this device has now. */
    suspend fun resume() {
        if (!isSupported || !wanted()) return
        if (!safely(false) { device.isPermitted() }) {
            remember(false)
            return
        }
        val token = safely<String?>(null) { device.currentToken() } ?: return
        safely(Unit) { api.value.registerDevice(token, device.platform) }
    }

    override suspend fun beforeSignOut() {
        if (!isSupported || !wanted()) return
        remember(false)
        val token = safely<String?>(null) { device.currentToken() }
        // the server comes first, while the sign-in still works, but it never holds the sign-out up for long
        if (token != null) withTimeoutOrNull(SERVER_WAIT_MILLIS) { safely(Unit) { api.value.removeDevice(token) } }
        withTimeoutOrNull(DEVICE_WAIT_MILLIS) { safely(Unit) { device.disable() } }
    }

    private suspend fun <T> safely(fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) { // a push service that fails must never break the app
            fallback
        }

    private companion object {
        const val KEY = "push.enabled"
        const val TRUE = "true"
        const val FALSE = "false"
        const val SERVER_WAIT_MILLIS = 2_000L
        const val DEVICE_WAIT_MILLIS = 1_500L
    }
}
