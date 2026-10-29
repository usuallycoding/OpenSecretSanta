package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

/** The users the administrator manages, in the order the server returns them. */
class AdminUsersViewModel(
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminUsers().map { users -> users.map { it.toManagedUser() } } }

    // Compose state, not a flow: a text field that keeps its own copy of what is typed takes the text it is given back after
    // every keystroke, and a flow delivers it a frame late, so letters typed meanwhile were taken away again
    private var typedSearch by mutableStateOf("")

    /** What the administrator typed to find a user, exactly as typed. It only narrows what is shown. */
    val search: String get() = typedSearch

    fun setSearch(text: String) {
        typedSearch = text
    }

    fun clearSearch() {
        typedSearch = ""
    }

    val state: StateFlow<LoadState<List<ManagedUser>>> = loader.state

    private val _ui = MutableStateFlow(AdminUsersUi())
    val ui: StateFlow<AdminUsersUi> = _ui.asStateFlow()

    fun refresh(): Job = loader.refresh()

    /** The delete icon of [username] was chosen: nothing is sent until it is confirmed, and only for a user the server said can be deleted. */
    fun askDelete(username: String) {
        val user = state.value.value?.firstOrNull { it.username == username }
        if (user?.canDelete == true && !_ui.value.busy) _ui.update { it.copy(confirmingDelete = username, notice = null) }
    }

    fun cancelDelete() = _ui.update { it.copy(confirmingDelete = null) }

    fun confirmDelete(): Job? {
        val username = _ui.value.confirmingDelete ?: return null
        if (_ui.value.busy) return null
        _ui.update { it.copy(confirmingDelete = null, busy = true) }
        return scope.launch {
            when (val result = api.deleteUser(username)) {
                is ApiResult.Success -> _ui.update { it.copy(busy = false) }
                is ApiResult.Failure -> {
                    val error = result.error
                    _ui.update {
                        it.copy(
                            busy = false,
                            notice = when {
                                error is ApiError.Rejected && error.status == 409 -> UsersNotice.DeleteRefused
                                // no longer this administrator's to manage: the reload simply leaves them out
                                error is ApiError.Rejected && error.status == 404 -> null
                                error == ApiError.Network -> UsersNotice.Unreachable
                                else -> UsersNotice.DeleteFailed
                            },
                        )
                    }
                }
            }
            loader.refresh().join()
        }
    }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }
}
