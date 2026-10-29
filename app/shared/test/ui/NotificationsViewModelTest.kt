package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.NotificationSettingsRequest
import com.usuallycoding.opensecretsanta.data.push.PushRegistration
import com.usuallycoding.opensecretsanta.data.settings.MemorySettingsStore
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.ui.notifications.NotificationSwitches
import com.usuallycoding.opensecretsanta.ui.notifications.NotificationsMessage
import com.usuallycoding.opensecretsanta.ui.notifications.NotificationsViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationsViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val device = FakePushDevice()
    private val settings = MemorySettingsStore()
    private val push = PushRegistration(device, lazyOf(server.api), settings)

    /** The first load runs on the client's own threads, so wait for it before looking at the state. */
    private suspend fun viewModel(device: FakePushDevice = this.device): NotificationsViewModel {
        val registration = if (device === this.device) push else PushRegistration(device, lazyOf(server.api), settings)
        return NotificationsViewModel(server.api, registration, immediateScope()).also { it.refresh().join() }
    }

    private fun savedSwitches() = with(server.notifications.settings) { NotificationSwitches(announcements, wishlist, questions, answers) }

    // --- loading ---

    @Test
    fun everythingStartsOnForAPersonWhoNeverChangedAnythingAndTheDeviceStartsOff() = runTest {
        val ui = viewModel().ui.value

        assertEquals(NotificationSwitches(), ui.switches)
        assertTrue(ui.serverAvailable && ui.deviceSupported)
        assertFalse(ui.deviceOn)
        assertFalse(ui.togglesEnabled)
    }

    @Test
    fun theSavedChoicesAreShown() = runTest {
        server.notifications.settings = NotificationSettingsRequest(announcements = true, wishlist = true, questions = false, answers = true)

        val ui = viewModel().ui.value

        assertEquals(NotificationSwitches(questions = false), ui.switches)
    }

    @Test
    fun aDeviceThatIsOnShowsOnAndTheTogglesAreEnabled() = runTest {
        push.turnOn()

        val ui = viewModel().ui.value

        assertTrue(ui.deviceOn)
        assertTrue(ui.togglesEnabled)
    }

    @Test
    fun aPermissionTakenAwayShowsTheDeviceOff() = runTest {
        push.turnOn()
        device.permission = "denied"

        assertFalse(viewModel().ui.value.deviceOn)
    }

    // --- the device switch ---

    @Test
    fun turningTheDeviceOnRegistersItAndEnablesTheToggles() = runTest {
        val model = viewModel()

        model.setDevice(true)!!.join()

        val ui = model.ui.value
        assertTrue(ui.deviceOn && ui.togglesEnabled)
        assertFalse(ui.deviceBusy)
        assertNull(ui.message)
        assertEquals(mapOf("tok-1" to "android"), server.notifications.devices)
    }

    @Test
    fun aRefusedPermissionPutsTheSwitchBackOffWithAReason() = runTest {
        device.willAnswer = "denied"
        val model = viewModel()

        model.setDevice(true)!!.join()

        val ui = model.ui.value
        assertFalse(ui.deviceOn)
        assertEquals(NotificationsMessage.PermissionDenied, ui.message)
        assertEquals(emptyMap(), server.notifications.devices)
    }

    @Test
    fun aServerThatCannotBeReachedPutsTheSwitchBackOffWithAReason() = runTest {
        val model = viewModel()
        server.unreachable = true

        model.setDevice(true)!!.join()

        assertFalse(model.ui.value.deviceOn)
        assertEquals(NotificationsMessage.DeviceFailed, model.ui.value.message)
    }

    @Test
    fun turningTheDeviceOffRemovesItAndDisablesTheToggles() = runTest {
        push.turnOn()
        val model = viewModel()

        model.setDevice(false)!!.join()

        val ui = model.ui.value
        assertFalse(ui.deviceOn)
        assertFalse(ui.togglesEnabled)
        assertEquals(emptyMap(), server.notifications.devices)
    }

    @Test
    fun theMessageGoesAwayWhenDismissedOrWhenTheNextTryStarts() = runTest {
        device.willAnswer = "denied"
        val model = viewModel()
        model.setDevice(true)!!.join()
        assertEquals(NotificationsMessage.PermissionDenied, model.ui.value.message)

        model.dismissMessage()
        assertNull(model.ui.value.message)

        device.permission = "undecided"
        device.willAnswer = "granted"
        model.setDevice(true)!!.join()
        assertNull(model.ui.value.message)
        assertTrue(model.ui.value.deviceOn)
    }

    @Test
    fun aServerWithoutPushOffersNoDeviceSwitch() = runTest {
        server.notifications.available = false
        val model = viewModel()

        assertFalse(model.ui.value.serverAvailable)
        assertFalse(model.ui.value.deviceSwitchEnabled)
        assertNull(model.setDevice(true))
        assertEquals(0, device.asked, "the person is not asked for a permission that would be of no use")
        assertEquals(emptyMap(), server.notifications.devices)
    }

    @Test
    fun aDeviceWithoutNotificationsOffersNoDeviceSwitch() = runTest {
        val model = viewModel(FakePushDevice(isSupported = false))

        assertFalse(model.ui.value.deviceSupported)
        assertFalse(model.ui.value.deviceSwitchEnabled)
        assertNull(model.setDevice(true))
    }

    // --- the four toggles ---

    @Test
    fun aToggleIsSavedAtOnceAndStillThereWhenTheSheetIsOpenedAgain() = runTest {
        push.turnOn()
        val model = viewModel()

        model.setSwitch(NotificationCategory.WISHLIST, false)!!.join()

        assertEquals(NotificationSwitches(wishlist = false), model.ui.value.switches)
        assertEquals(NotificationSwitches(wishlist = false), savedSwitches())
        assertEquals(NotificationSwitches(wishlist = false), viewModel().ui.value.switches)
    }

    @Test
    fun theFourTogglesAreIndependent() = runTest {
        push.turnOn()
        val model = viewModel()

        NotificationCategory.entries.forEach { model.setSwitch(it, false)!!.join() }
        assertEquals(NotificationSwitches(false, false, false, false), savedSwitches())

        model.setSwitch(NotificationCategory.ANSWERS, true)!!.join()
        assertEquals(NotificationSwitches(false, false, false, true), savedSwitches())
    }

    @Test
    fun aToggleThatCannotBeSavedGoesBackAndSaysSo() = runTest {
        push.turnOn()
        val model = viewModel()
        server.notifications.failNextSaveWith(HttpStatusCode.InternalServerError)

        model.setSwitch(NotificationCategory.QUESTIONS, false)!!.join()

        assertEquals(NotificationSwitches(), model.ui.value.switches)
        assertEquals(NotificationsMessage.SaveFailed, model.ui.value.message)
        assertEquals(NotificationSwitches(), savedSwitches())
    }

    @Test
    fun aToggleWhoseServerIsAwayGoesBackToo() = runTest {
        push.turnOn()
        val model = viewModel()
        server.unreachable = true

        model.setSwitch(NotificationCategory.ANNOUNCEMENTS, false)!!.join()

        assertTrue(model.ui.value.switches.announcements)
        assertEquals(NotificationsMessage.SaveFailed, model.ui.value.message)
    }

    @Test
    fun aFailedSaveGoesBackToWhatTheServerLastConfirmedNotToWhatWasBeforeTheTap() = runTest {
        push.turnOn()
        val model = viewModel()
        model.setSwitch(NotificationCategory.WISHLIST, false)!!.join()
        server.notifications.failNextSaveWith(HttpStatusCode.InternalServerError)

        model.setSwitch(NotificationCategory.ANSWERS, false)!!.join()

        assertEquals(NotificationSwitches(wishlist = false), model.ui.value.switches)
    }

    @Test
    fun togglesAreInertWhileTheDeviceIsOff() = runTest {
        val model = viewModel()

        assertNull(model.setSwitch(NotificationCategory.WISHLIST, false))

        assertEquals(NotificationSwitches(), model.ui.value.switches)
        assertEquals(emptyList(), server.calls.filter { it.startsWith("PUT /notifications ") || it == "PUT /notifications" })
    }

    @Test
    fun settingASwitchToWhatItAlreadyIsSendsNothing() = runTest {
        push.turnOn()
        val model = viewModel()
        server.calls.clear()

        assertNull(model.setSwitch(NotificationCategory.WISHLIST, true))

        assertEquals(emptyList(), server.calls)
    }
}
