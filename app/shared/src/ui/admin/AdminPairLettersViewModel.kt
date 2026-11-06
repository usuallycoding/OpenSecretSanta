package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.letters.AdminLetterBox
import com.usuallycoding.opensecretsanta.ui.letters.toBox
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * Every pair's letters of one group as the group's owner reads them. There is nothing here that sends, changes or deletes a
 * letter, and reading is never recorded. The pair sheet shows the letters of its own receiver only.
 */
class AdminPairLettersViewModel(
    val groupId: Long,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminLetters(groupId).map { boxes -> boxes.map { it.toBox() } } }

    val state: StateFlow<LoadState<List<AdminLetterBox>>> = loader.state

    fun refresh(): Job = loader.refresh()
}
