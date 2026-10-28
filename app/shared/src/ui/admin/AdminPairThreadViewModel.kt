package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/** What one Santa asked one receiver, with the receiver's answers. Empty when nothing has been asked. */
data class PairThread(val questions: List<OversightQuestion>)

/**
 * One pair's thread as the group's owner reads it: the server returns every thread of the group, and
 * this keeps only the one of the chosen Santa and receiver. Nothing here asks, answers or changes a
 * question, and reading is never recorded.
 */
class AdminPairThreadViewModel(
    val groupId: Long,
    val santa: String,
    val receiver: String,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) {
        api.adminQuestions(groupId).map { threads ->
            val thread = threads.firstOrNull { it.santa == santa && it.receiver == receiver }
            PairThread(thread?.questions.orEmpty().map { it.toOversightQuestion() })
        }
    }

    val state: StateFlow<LoadState<PairThread>> = loader.state

    fun refresh(): Job = loader.refresh()
}
