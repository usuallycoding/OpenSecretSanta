package com.usuallycoding.opensecretsanta.ui.load

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.ui.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What a screen that loads something has to show. A refresh keeps the old [value] while it runs and
 * when it fails, so a screen never empties itself just because the server was slow or away.
 *
 * ```
 *  first load      (null, loading, no error)   progress indicator
 *  loaded          (v,    idle,    no error)
 *  failed, no data (null, idle,    error)      message and "Try again"
 *  refreshing      (v,    loading, no error)   v stays, thin progress bar
 *  refresh failed  (v,    idle,    error)      v stays, message and "Try again"
 * ```
 */
data class LoadState<out T>(
    val value: T? = null,
    val loading: Boolean = true,
    val error: ApiError? = null,
)

/** Loads one thing, starting at once, and loads it again on request. Used by every screen that reads from the server. */
class Loader<T : Any>(
    private val scope: CoroutineScope,
    private val fetch: suspend () -> ApiResult<T>,
) {
    private val _state = MutableStateFlow(LoadState<T>())
    val state: StateFlow<LoadState<T>> = _state.asStateFlow()

    private var running: Job? = null

    init {
        refresh()
    }

    /** Loads again, unless a load is already running, and returns the work that is (now) running. */
    fun refresh(): Job {
        running?.takeIf { it.isActive }?.let { return it }
        _state.update { it.copy(loading = true, error = null) }
        val job = scope.launch {
            when (val result = fetch()) {
                is ApiResult.Success -> _state.value = LoadState(result.value, loading = false)
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
        running = job
        return job
    }
}

/** What to tell the person when something could not be loaded: no technical details. */
fun ApiError.loadMessage(): String = if (this == ApiError.Network) Strings.SERVER_UNREACHABLE else Strings.SERVER_FAILED
