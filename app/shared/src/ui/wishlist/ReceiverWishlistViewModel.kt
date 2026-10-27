package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * What the person's receiver keeps for this group, as a Santa may read it: the items in the
 * receiver's order, without ids. There is nothing here that changes the list, and reading it is
 * never recorded, so the receiver cannot learn who looked.
 */
class ReceiverWishlistViewModel(
    val groupId: Long,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.receiverWishlist(groupId).map { items -> items.map { it.toItem() } } }

    val state: StateFlow<LoadState<List<SharedWishItem>>> = loader.state

    fun refresh(): Job = loader.refresh()
}
