package com.usuallycoding.opensecretsanta.ui.letters

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * The letters the person's receiver has sent them, newest first. There is nothing here that changes or answers a letter. The
 * receiver's username only titles the page; when it cannot be read the letters are still shown.
 */
class InboxViewModel(
    val groupId: Long,
    private val api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { load() }

    val data: StateFlow<LoadState<InboxData>> = loader.state

    fun refresh(): Job = loader.refresh()

    private suspend fun load(): ApiResult<InboxData> {
        val letters = when (val result = api.lettersFromReceiver(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        val receiver = (api.receiver(groupId) as? ApiResult.Success)?.value?.username
        return ApiResult.Success(InboxData(receiver, letters.newestFirst()))
    }
}
