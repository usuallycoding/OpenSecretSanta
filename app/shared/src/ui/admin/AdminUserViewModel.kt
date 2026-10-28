package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.map
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
 * There is deliberately nothing here that changes anyone's rights.
 */
class AdminUserViewModel(
    val username: String,
    private val me: String,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminUser(username).map { it.toManagedUser() } }
    private val _ui = MutableStateFlow(AdminUserUi())

    val data: StateFlow<LoadState<ManagedUser>> = loader.state
    val ui: StateFlow<AdminUserUi> = _ui.asStateFlow()

    /** The administrator's own account is not reset from here: it would sign them out of their own session. */
    val canReset: Boolean get() = username != me

    fun refresh(): Job = loader.refresh()

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
