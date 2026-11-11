package com.usuallycoding.opensecretsanta.ui.download

import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Whether the screen offers "Download Android app": only where the platform [enabled] it (the web), and only while the server
 * says an app is there. Anything else, such as a server that cannot be reached or an older one that does not say, means no
 * offer and no message, so [offered] is false until a [refresh] has heard a yes.
 */
class AndroidDownloadOffer(
    private val api: ApiClient,
    private val enabled: Boolean,
    private val scope: CoroutineScope,
) {
    private val _offered = MutableStateFlow(false)
    val offered: StateFlow<Boolean> = _offered.asStateFlow()

    /** Asks the server again; a screen calls it when it appears, so an app uploaded meanwhile shows up. Null where nothing is offered. */
    fun refresh(): Job? {
        if (!enabled) return null
        return scope.launch {
            _offered.value = (api.health() as? ApiResult.Success)?.value?.androidDownload == true
        }
    }

    /** The address the browser opens to download the app. */
    val url: String get() = api.androidDownloadUrl()
}
