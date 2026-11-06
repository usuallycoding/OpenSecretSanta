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
 * One participant's delivery info for the group as the group's owner reads it. There is nothing here that changes it, and reading is
 * never recorded. It loads apart from the person's account, so a failure of one does not hide the other.
 */
class AdminParticipantDeliveryInfoViewModel(
    val groupId: Long,
    val username: String,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminParticipantDeliveryInfo(groupId, username).map { it.toOversight() } }

    val state: StateFlow<LoadState<OversightDeliveryInfo>> = loader.state

    fun refresh(): Job = loader.refresh()
}
