package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * One participant's list as the group's owner reads it: the items in the participant's order. There
 * is nothing here that changes the list, and reading is never recorded.
 */
class AdminParticipantWishlistViewModel(
    val groupId: Long,
    val username: String,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminParticipantWishlist(groupId, username).map { items -> items.map { it.toOversightItem() } } }

    val state: StateFlow<LoadState<List<OversightItem>>> = loader.state

    fun refresh(): Job = loader.refresh()
}
