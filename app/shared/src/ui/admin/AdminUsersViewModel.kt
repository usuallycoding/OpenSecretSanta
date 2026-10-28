package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/** The users the administrator manages, in the order the server returns them. */
class AdminUsersViewModel(
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminUsers().map { users -> users.map { it.toManagedUser() } } }

    val state: StateFlow<LoadState<List<ManagedUser>>> = loader.state

    fun refresh(): Job = loader.refresh()
}
