package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import com.usuallycoding.opensecretsanta.ui.wishlist.PendingPreviewRefresher
import com.usuallycoding.opensecretsanta.ui.wishlist.hasPendingPreviews
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
    private val api: ApiClient,
    scope: CoroutineScope,
    pendingPreviewDelays: List<Long> = PendingPreviewRefresher.DEFAULT_DELAYS_MILLIS,
) : ViewModel(scope) {
    private val loader = Loader(scope) { load() }
    private val previewRefresher = PendingPreviewRefresher(scope, ::refreshPendingPreviews, pendingPreviewDelays)

    val state: StateFlow<LoadState<List<OversightItem>>> = loader.state

    fun refresh(): Job = loader.refresh()

    private suspend fun load(): ApiResult<List<OversightItem>> =
        api.adminParticipantWishlist(groupId, username).map { items ->
            items.map { it.toOversightItem() }.also { mapped ->
                previewRefresher.sync(hasPendingPreviews(mapped.map { it.linkPreviews }))
            }
        }

    private suspend fun refreshPendingPreviews(): Boolean {
        val current = loader.state.value.value ?: return false
        val items = when (val result = api.adminParticipantWishlist(groupId, username)) {
            is ApiResult.Success -> result.value.map { it.toOversightItem() }
            is ApiResult.Failure -> return hasPendingPreviews(current.map { it.linkPreviews })
        }
        loader.replace(items)
        return hasPendingPreviews(items.map { it.linkPreviews })
    }
}
