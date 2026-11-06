package com.usuallycoding.opensecretsanta.ui.readmarks

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.ReadMarksDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/** How much is new to the Santa, for the badges on the group page. */
data class UnreadCounts(val wishlist: Int, val answers: Int, val letters: Int)

internal fun ReadMarksDto.toCounts() = UnreadCounts(wishlist.unread, answers.unread, letters.unread)

/**
 * What the Santa has not seen yet in the group, counted by the server: wishlist items new or edited, answers and letters. Loaded
 * for an Active or Archived group; a load that failed or has not finished gives no counts, so no badge.
 */
class ReadMarksViewModel(
    val groupId: Long,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.readMarks(groupId).map { it.toCounts() } }

    val counts: StateFlow<LoadState<UnreadCounts>> = loader.state

    fun refresh(): Job = loader.refresh()
}

/** The counts to badge: none while loading or after a failure, as what is known is then not reliable. */
internal fun LoadState<UnreadCounts>?.known(): UnreadCounts? = this?.takeIf { it.error == null }?.value
