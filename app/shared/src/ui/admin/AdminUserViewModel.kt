package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One user the administrator manages. The server returns the temporary password for as long as it is
 * the temporary one, so the loaded user holds it exactly as long as the server does; the password a
 * reset just made is shown once in its own dialog ([AdminUserUi.created]) and nowhere else.
 * The one thing that changes anyone's rights is handing over the signed-in administrator's own role, which takes it from them.
 */
class AdminUserViewModel(
    val username: String,
    private val me: String,
    private val api: ApiClient,
    private val session: SessionManager,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminUser(username).map { it.toManagedUser() } }
    private val _ui = MutableStateFlow(AdminUserUi())

    val data: StateFlow<LoadState<ManagedUser>> = loader.state
    val ui: StateFlow<AdminUserUi> = _ui.asStateFlow()

    /** The administrator's own account is not reset from here: it would sign them out of their own session. */
    val canReset: Boolean get() = username != me

    /** The administrator's own page offers no handover to themself. */
    val canHandOver: Boolean get() = username != me

    /** The person still has a temporary password, which the administrator can read: they cannot take over until they change it. */
    val handOverBlocked: Boolean get() = data.value.value?.mustChangePassword == true

    fun refresh(): Job = loader.refresh()

    fun askHandOver() {
        if (canHandOver && !handOverBlocked && !_ui.value.busy) _ui.update { it.copy(handingOver = true, handOverError = null, notice = null) }
    }

    fun cancelHandOver() = _ui.update { it.copy(handingOver = false, handOverError = null) }

    /**
     * Hands the role over with the administrator's own [password]. A wrong password or too many attempts is said in the
     * open confirmation; any other refusal closes it. Returns the running work, or null when nothing is sent.
     */
    fun confirmHandOver(password: String): Job? {
        val ui = _ui.value
        if (!ui.handingOver || ui.busy || password.isEmpty() || !canHandOver || handOverBlocked) return null
        _ui.update { it.copy(busy = true, handOverError = null) }
        return scope.launch {
            when (val result = api.handOverAdmin(username, password)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(busy = false, handingOver = false) }
                    // the screen is left behind by this: the person is a participant from here on
                    session.handedOver(username)
                }
                is ApiResult.Failure -> handOverFailed(result.error)
            }
        }
    }

    private suspend fun handOverFailed(error: ApiError) {
        when {
            error is ApiError.Validation && error.field == "password" -> _ui.update { it.copy(busy = false, handOverError = error.message) }
            error is ApiError.RateLimited -> _ui.update {
                it.copy(busy = false, handOverError = Strings.handOverPaused(error.retryAfterSeconds?.let { seconds -> maxOf(1, (seconds + 59) / 60) }))
            }
            error == ApiError.Network -> _ui.update { it.copy(busy = false, handOverError = Strings.SERVER_UNREACHABLE) }
            error is ApiError.Rejected && error.status == 404 -> {
                _ui.update { it.copy(busy = false, handingOver = false) }
                loader.refresh().join()
            }
            error is ApiError.Rejected && error.status == 409 -> {
                _ui.update { it.copy(busy = false, handingOver = false, notice = UserNotice.HandOverRefused) }
                loader.refresh().join()
            }
            else -> _ui.update { it.copy(busy = false, handingOver = false, notice = UserNotice.Failed) }
        }
    }

    fun askReset() {
        if (canReset && !_ui.value.busy) _ui.update { it.copy(confirming = true, notice = null) }
    }

    fun cancelReset() = _ui.update { it.copy(confirming = false) }

    fun confirmReset(): Job? {
        if (!_ui.value.confirming || _ui.value.busy) return null
        _ui.update { it.copy(confirming = false, busy = true) }
        return scope.launch {
            when (val result = api.resetPassword(username)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(created = CreatedAccount(result.value.username, result.value.temporaryPassword)) }
                    loader.refresh().join()
                    _ui.update { it.copy(busy = false) }
                }
                is ApiResult.Failure -> failed(result.error)
            }
        }
    }

    fun dismissCreated() = _ui.update { it.copy(created = null) }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    private suspend fun failed(error: ApiError) {
        if (error is ApiError.Rejected && error.status == 404) {
            // no longer this administrator's to manage: the reload shows the user as gone
            _ui.update { it.copy(busy = false) }
            loader.refresh().join()
        } else {
            _ui.update { it.copy(busy = false, notice = if (error == ApiError.Network) UserNotice.Unreachable else UserNotice.Failed) }
        }
    }
}
