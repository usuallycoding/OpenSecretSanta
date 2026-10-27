package com.usuallycoding.opensecretsanta.ui.gift

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/** Who the person's Santa was. The server only tells once the group is archived. */
data class SantaInfo(val username: String, val fullName: String?)

class SantaRevealViewModel(
    val groupId: Long,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) {
        when (val result = api.santa(groupId)) {
            is ApiResult.Success -> ApiResult.Success(SantaInfo(result.value.username, result.value.fullName?.trim()?.ifEmpty { null }))
            is ApiResult.Failure -> result
        }
    }

    val state: StateFlow<LoadState<SantaInfo>> = loader.state

    /** A stale screen: the group is not archived yet, so there is nothing to reveal, which is not a failure. */
    val notYet: Boolean get() = (loader.state.value.error as? ApiError.Rejected)?.status == 409

    fun refresh(): Job = loader.refresh()
}
