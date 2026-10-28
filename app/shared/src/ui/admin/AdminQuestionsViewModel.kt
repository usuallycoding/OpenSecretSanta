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
 * Every question thread of a group, as its owner reads them: one per Santa and receiver pair, naming
 * both. Nothing here asks, answers or changes a question, and reading is never recorded.
 */
class AdminQuestionsViewModel(
    val groupId: Long,
    private val me: String,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminQuestions(groupId).map { threads -> threads.map { it.toThread(me) } } }

    val state: StateFlow<LoadState<List<OversightThread>>> = loader.state

    fun refresh(): Job = loader.refresh()
}
