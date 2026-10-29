package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.push.PushDevice
import com.usuallycoding.opensecretsanta.data.push.PushEnable
import com.usuallycoding.opensecretsanta.data.push.PushRegistration
import com.usuallycoding.opensecretsanta.data.push.TurnOn
import com.usuallycoding.opensecretsanta.data.settings.MemorySettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A device whose permission and token a test controls, and that remembers what it was asked. */
class FakePushDevice(
    override val platform: String = "android",
    override val isSupported: Boolean = true,
) : PushDevice {
    /** `granted`, `denied` or `undecided` (asking turns it into the answer in [willAnswer]). */
    var permission = "undecided"
    var willAnswer = "granted"
    var token: String? = "tok-1"
    var failing = false
    var asked = 0
    var disabled = 0

    override suspend fun isPermitted() = permission == "granted"

    override suspend fun enable(): PushEnable {
        if (failing) throw IllegalStateException("the push service is down")
        if (permission == "undecided") {
            asked++
            permission = willAnswer
        }
        if (permission != "granted") return PushEnable.Denied
        return token?.let { PushEnable.Ready(it) } ?: PushEnable.Failed
    }

    override suspend fun currentToken(): String? {
        if (failing) throw IllegalStateException("the push service is down")
        return if (permission == "granted") token else null
    }

    override suspend fun disable() {
        disabled++
        token = null
    }
}

class PushRegistrationTest {
    /** `runTest` runs on virtual time, which would end a real timeout at once; the sign-out wait must run on the clock. */
    private suspend fun realTime(block: suspend () -> Unit) = withContext(Dispatchers.Default) { block() }

    private val server = ScriptedServer().also { it.session() }
    private val device = FakePushDevice()
    private val settings = MemorySettingsStore()
    private val registration = PushRegistration(device, lazyOf(server.api), settings)

    @Test
    fun turningOnAsksForThePermissionAndRegistersTheToken() = runTest {
        assertEquals(TurnOn.Done, registration.turnOn())

        assertEquals(1, device.asked)
        assertEquals(mapOf("tok-1" to "android"), server.notifications.devices)
        assertTrue(registration.isOn())
    }

    @Test
    fun aRefusedPermissionRegistersNothingAndStaysOff() = runTest {
        device.willAnswer = "denied"

        assertEquals(TurnOn.Denied, registration.turnOn())

        assertEquals(emptyMap(), server.notifications.devices)
        assertFalse(registration.isOn())
    }

    @Test
    fun aPermissionRefusedBeforeIsNotAskedAgain() = runTest {
        device.permission = "denied"

        assertEquals(TurnOn.Denied, registration.turnOn())

        assertEquals(0, device.asked)
    }

    @Test
    fun aServerThatCannotBeReachedLeavesItOffWithTheReason() = runTest {
        server.unreachable = true

        assertEquals(TurnOn.Failed(ApiError.Network), registration.turnOn())

        assertFalse(registration.isOn())
    }

    @Test
    fun noTokenFromThePushServiceIsAFailureWithoutAServerCall() = runTest {
        device.token = null

        assertEquals(TurnOn.Failed(), registration.turnOn())

        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun aDeviceThatThrowsIsAFailureNotACrash() = runTest {
        device.failing = true

        assertEquals(TurnOn.Failed(), registration.turnOn())
    }

    @Test
    fun anUnsupportedDeviceCannotBeTurnedOn() = runTest {
        val unsupported = PushRegistration(FakePushDevice(isSupported = false), lazyOf(server.api), settings)

        assertEquals(TurnOn.Failed(), unsupported.turnOn())
        assertFalse(unsupported.isOn())
        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun turningOffRemovesTheTokenFromTheServerAndThrowsItAway() = runTest {
        registration.turnOn()

        registration.turnOff()

        assertEquals(emptyMap(), server.notifications.devices)
        assertEquals(1, device.disabled)
        assertFalse(registration.isOn())
    }

    @Test
    fun turningOffStillThrowsTheTokenAwayWhenTheServerCannotBeReached() = runTest {
        registration.turnOn()
        server.unreachable = true

        registration.turnOff()

        assertEquals(1, device.disabled)
        assertFalse(registration.isOn())
    }

    @Test
    fun theSwitchShowsOffOnceThePermissionIsTakenAway() = runTest {
        registration.turnOn()
        device.permission = "denied"

        assertFalse(registration.isOn())
    }

    @Test
    fun resumingRegistersTheCurrentTokenAgainWhileItIsOn() = runTest {
        registration.turnOn()
        server.notifications.devices.clear() // the server lost it
        device.token = "tok-2"

        registration.resume()

        assertEquals(mapOf("tok-2" to "android"), server.notifications.devices)
    }

    @Test
    fun resumingDoesNothingWhileItIsOff() = runTest {
        registration.resume()

        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun resumingAfterThePermissionWasTakenAwayTurnsItOffForGood() = runTest {
        registration.turnOn()
        device.permission = "denied"
        server.calls.clear()

        registration.resume()
        device.permission = "granted" // allowed again later in the device's settings

        assertEquals(emptyList(), server.calls)
        assertFalse(registration.isOn(), "it must be asked for again, not come back by itself")
    }

    @Test
    fun signingOutRemovesTheDeviceAndForgetsTheChoice() = runTest {
        registration.turnOn()

        realTime { registration.beforeSignOut() }

        assertEquals(emptyMap(), server.notifications.devices)
        assertEquals(1, device.disabled)
        assertFalse(registration.isOn())
        assertNull(settings.get("push.enabled")?.takeIf { it == "true" })
    }

    @Test
    fun signingOutWhileItIsOffTouchesNothing() = runTest {
        realTime { registration.beforeSignOut() }

        assertEquals(emptyList(), server.calls)
        assertEquals(0, device.disabled)
    }

    @Test
    fun signingOutOfflineStillForgetsTheDevice() = runTest {
        registration.turnOn()
        server.unreachable = true

        realTime { registration.beforeSignOut() }

        assertEquals(1, device.disabled)
        assertFalse(registration.isOn())
        assertIs<FakePushDevice>(device)
    }
    // --- signing out through the session ---

    private fun signedInSession(): com.usuallycoding.opensecretsanta.data.session.SessionManager {
        lateinit var sessionServer: ScriptedServer
        sessionServer = server
        val registrationOfTheSession = PushRegistration(device, lazy { sessionServer.api }, settings)
        return server.session(registrationOfTheSession)
    }

    @Test
    fun theNextPersonOnTheSameDeviceStartsWithItOffAndTheFirstPersonGetsNothingMore() = runTest {
        val session = signedInSession()
        session.signIn("alice", "pw")
        // alice turns it on, here
        assertEquals(TurnOn.Done, PushRegistration(device, lazyOf(server.api), settings).turnOn())
        assertEquals(mapOf("tok-1" to "android"), server.notifications.devices)

        session.signOut()
        realTime { kotlinx.coroutines.delay(50) } // the sign-out may take a moment; it is over when the state says so

        assertIs<com.usuallycoding.opensecretsanta.data.session.SessionState.SignedOut>(session.state.value)
        assertEquals(emptyMap(), server.notifications.devices, "alice's device is no longer registered")
        assertEquals(1, device.disabled)
        session.signIn("bob", "pw")
        assertFalse(PushRegistration(device, lazyOf(server.api), settings).isOn(), "bob starts with notifications off")
    }

    @Test
    fun signingOutOfflineStillEndsTheSession() = runTest {
        val session = signedInSession()
        session.signIn("alice", "pw")
        PushRegistration(device, lazyOf(server.api), settings).turnOn()
        server.unreachable = true

        session.signOut()
        realTime {
            for (i in 0 until 200) {
                if (session.state.value is com.usuallycoding.opensecretsanta.data.session.SessionState.SignedOut) break
                kotlinx.coroutines.delay(20)
            }
        }

        assertIs<com.usuallycoding.opensecretsanta.data.session.SessionState.SignedOut>(session.state.value)
        assertEquals(1, device.disabled, "the token is thrown away on the device even when the server is out of reach")
    }

}
