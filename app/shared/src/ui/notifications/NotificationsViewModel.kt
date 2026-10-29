package com.usuallycoding.opensecretsanta.ui.notifications

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsDto
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsRequest
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.push.PushRegistration
import com.usuallycoding.opensecretsanta.data.push.TurnOn
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The four switches of the person, all on until they turn one off. */
data class NotificationSwitches(
    val announcements: Boolean = true,
    val wishlist: Boolean = true,
    val questions: Boolean = true,
    val answers: Boolean = true,
) {
    operator fun get(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.ANNOUNCEMENTS -> announcements
        NotificationCategory.WISHLIST -> wishlist
        NotificationCategory.QUESTIONS -> questions
        NotificationCategory.ANSWERS -> answers
    }

    fun with(category: NotificationCategory, on: Boolean): NotificationSwitches = when (category) {
        NotificationCategory.ANNOUNCEMENTS -> copy(announcements = on)
        NotificationCategory.WISHLIST -> copy(wishlist = on)
        NotificationCategory.QUESTIONS -> copy(questions = on)
        NotificationCategory.ANSWERS -> copy(answers = on)
    }

    fun toRequest() = NotificationSettingsRequest(announcements, wishlist, questions, answers)
}

private fun NotificationSettingsDto.toSwitches() = NotificationSwitches(announcements, wishlist, questions, answers)

/** Something the sheet tells the person after an attempt that did not work. */
enum class NotificationsMessage { PermissionDenied, DeviceFailed, SaveFailed }

data class NotificationsUiState(
    val switches: NotificationSwitches = NotificationSwitches(),
    /** Whether the server can send push at all. */
    val serverAvailable: Boolean = true,
    /** Whether this device or browser can show notifications at all. */
    val deviceSupported: Boolean = true,
    /** The master switch: whether this device is registered for notifications. */
    val deviceOn: Boolean = false,
    val deviceBusy: Boolean = false,
    val message: NotificationsMessage? = null,
) {
    /** Turning the device on needs a server that can send, a device that can show and nothing running already. */
    val deviceSwitchEnabled: Boolean get() = serverAvailable && deviceSupported && !deviceBusy

    /** The four kinds only make sense for a device that gets notifications. */
    val togglesEnabled: Boolean get() = deviceOn && !deviceBusy
}

/**
 * The Notifications sheet: the master switch of this device, which registers it (asking the system's permission) or
 * removes it, and the four kinds, which live on the server and save as they are changed. A kind that cannot be saved goes
 * back to what the server last confirmed.
 */
class NotificationsViewModel(
    private val api: ApiClient,
    private val push: PushRegistration,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val scope = scope
    private val _ui = MutableStateFlow(NotificationsUiState(deviceSupported = push.isSupported))
    private val loader = Loader(scope) { api.notificationSettings().also { result -> if (result is ApiResult.Success) onLoaded(result.value) } }

    private val saving = Mutex()
    private var confirmed = NotificationSwitches()
    private var changesInFlight = 0

    val load: StateFlow<LoadState<NotificationSettingsDto>> = loader.state
    val ui: StateFlow<NotificationsUiState> = _ui.asStateFlow()

    private suspend fun onLoaded(settings: NotificationSettingsDto) {
        val on = push.isOn()
        confirmed = settings.toSwitches()
        _ui.update { current ->
            current.copy(
                switches = if (changesInFlight == 0) confirmed else current.switches,
                serverAvailable = settings.available,
                deviceSupported = push.isSupported,
                deviceOn = on,
            )
        }
    }

    fun refresh(): Job = loader.refresh()

    fun dismissMessage() = _ui.update { it.copy(message = null) }

    /** The master switch. Turning it on asks for the permission if it is needed; a refusal puts it back off with a reason. */
    fun setDevice(on: Boolean): Job? {
        val state = _ui.value
        if (!state.deviceSwitchEnabled || state.deviceOn == on) return null
        _ui.update { it.copy(deviceBusy = true, message = null) }
        return scope.launch {
            if (on) {
                val message = when (push.turnOn()) {
                    TurnOn.Done -> null
                    TurnOn.Denied -> NotificationsMessage.PermissionDenied
                    is TurnOn.Failed -> NotificationsMessage.DeviceFailed
                }
                _ui.update { it.copy(deviceBusy = false, deviceOn = message == null, message = message) }
            } else {
                push.turnOff()
                _ui.update { it.copy(deviceBusy = false, deviceOn = false) }
            }
        }
    }

    /** One of the four kinds. It shows at once and is saved; if the save fails it goes back and the sheet says so. */
    fun setSwitch(category: NotificationCategory, on: Boolean): Job? {
        val state = _ui.value
        if (!state.togglesEnabled || state.switches[category] == on) return null
        changesInFlight++
        _ui.update { it.copy(switches = it.switches.with(category, on), message = null) }
        return scope.launch {
            val result = saving.withLock {
                // the newest choices, so that two quick changes are not saved one over the other
                api.saveNotificationSettings(_ui.value.switches.toRequest())
            }
            changesInFlight--
            when (result) {
                is ApiResult.Success -> {
                    confirmed = result.value.toSwitches()
                    if (changesInFlight == 0) _ui.update { it.copy(switches = confirmed, serverAvailable = result.value.available) }
                }
                is ApiResult.Failure -> _ui.update {
                    it.copy(switches = if (changesInFlight == 0) confirmed else it.switches, message = NotificationsMessage.SaveFailed)
                }
            }
        }
    }
}
