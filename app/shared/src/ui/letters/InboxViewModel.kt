package com.usuallycoding.opensecretsanta.ui.letters

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import com.usuallycoding.opensecretsanta.ui.readmarks.KIND_LETTERS
import com.usuallycoding.opensecretsanta.ui.readmarks.ReadTracker
import com.usuallycoding.opensecretsanta.ui.readmarks.newestOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * The letters the person's receiver has sent them, newest first, each marked new when it was sent after what the Santa had seen on
 * opening the page. There is nothing here that changes or answers a letter. The receiver's username only titles the page; when it
 * cannot be read the letters are still shown.
 */
class InboxViewModel(
    val groupId: Long,
    private val api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val tracker = ReadTracker(api, groupId, scope, KIND_LETTERS)
    private val loader = Loader(scope) { load() }

    val data: StateFlow<LoadState<InboxData>> = loader.state

    fun refresh(): Job = loader.refresh()

    /** Tells the server the letters have been shown, so that the next visit has none marked; call it once the page is on screen. */
    fun markSeen(): Job? = tracker.markSeen()

    private suspend fun load(): ApiResult<InboxData> {
        val letters = when (val result = api.lettersFromReceiver(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        val receiver = (api.receiver(groupId) as? ApiResult.Success)?.value?.username
        tracker.ensureBaseline()
        tracker.shown(newestOf(letters.map { it.sentAt }))
        return ApiResult.Success(InboxData(receiver, letters.newestFirst(tracker.baseline)))
    }
}
