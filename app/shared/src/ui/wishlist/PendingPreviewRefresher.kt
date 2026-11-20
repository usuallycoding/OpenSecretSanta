package com.usuallycoding.opensecretsanta.ui.wishlist

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Schedules a brief, bounded sequence of background refreshes while a wishlist has any `pending` link previews,
 * stopping immediately once none remain or all scheduled attempts finish.
 */
internal class PendingPreviewRefresher(
    private val scope: CoroutineScope,
    private val refresh: suspend () -> Boolean,
    private val delaysMillis: List<Long> = DEFAULT_DELAYS_MILLIS,
) {
    private var job: Job? = null

    fun sync(hasPending: Boolean) {
        if (!hasPending) {
            job?.cancel()
            job = null
            return
        }
        if (job?.isActive == true) return
        if (delaysMillis.isEmpty()) return
        job = scope.launch {
            for (waitMillis in delaysMillis) {
                delay(waitMillis)
                val stillPending = refresh()
                if (!stillPending) break
            }
        }
    }

    companion object {
        val DEFAULT_DELAYS_MILLIS = listOf(2_000L, 4_000L, 6_000L)
    }
}
