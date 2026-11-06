package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import com.usuallycoding.opensecretsanta.ui.readmarks.KIND_WISHLIST
import com.usuallycoding.opensecretsanta.ui.readmarks.ReadTracker
import com.usuallycoding.opensecretsanta.ui.readmarks.newestOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * What the person's receiver keeps for this group, as a Santa may read it: the items in the receiver's order, without ids, each
 * marked new or edited when it came after what the Santa had seen on opening the page. There is nothing here that changes the
 * list. What the Santa has seen is kept on the server for the Santa alone, so the receiver cannot learn who looked.
 */
class ReceiverWishlistViewModel(
    val groupId: Long,
    private val api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val tracker = ReadTracker(api, groupId, scope, KIND_WISHLIST)
    private val loader = Loader(scope) { load() }

    val state: StateFlow<LoadState<List<SharedWishItem>>> = loader.state

    fun refresh(): Job = loader.refresh()

    /** Tells the server the list has been shown, so that the next visit has nothing marked; call it once the list is on screen. */
    fun markSeen(): Job? = tracker.markSeen()

    private suspend fun load(): ApiResult<List<SharedWishItem>> {
        val items = when (val result = api.receiverWishlist(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        tracker.ensureBaseline()
        tracker.shown(newestOf(items.flatMap { listOf(it.createdAt, it.updatedAt) }))
        return ApiResult.Success(items.map { it.toItem(tracker.baseline) })
    }
}
