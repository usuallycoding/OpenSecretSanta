package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/** The administrator's own groups, in the order the server returns them. */
class AdminGroupsViewModel(
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminGroups().map { groups -> groups.map { it.toGroup() } } }

    val state: StateFlow<LoadState<List<AdminGroup>>> = loader.state

    fun refresh(): Job = loader.refresh()
}
