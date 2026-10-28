package com.usuallycoding.opensecretsanta.ui.form

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class CopyStatus { Idle, Copied, Failed }

/**
 * What a copy icon shows. A copy that works reads as [CopyStatus.Copied] for [holdMillis] and then goes back to
 * [CopyStatus.Idle]; one the platform refuses (the web only has a clipboard on a secure page) stays
 * [CopyStatus.Failed] until the next try.
 */
class CopyAction(private val write: suspend (String) -> Unit, private val holdMillis: Long = 1500) {
    private val _status = MutableStateFlow(CopyStatus.Idle)
    val status: StateFlow<CopyStatus> = _status

    suspend fun copy(text: String) {
        try {
            write(text)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            _status.value = CopyStatus.Failed
            return
        }
        _status.value = CopyStatus.Copied
        delay(holdMillis)
        _status.value = CopyStatus.Idle
    }
}
