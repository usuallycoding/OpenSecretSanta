package com.usuallycoding.opensecretsanta.ui.gift

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.domain.gift.QuestionLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.groupPhase
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The person's receiver and the questions the person asked them. As with the wishlist, what the screen
 * shows is what the server answered: a question appears once it is stored, and when the group changed
 * behind the screen's back (archived, the limit reached elsewhere) it is reloaded and a notice says so.
 */
class ReceiverViewModel(
    val groupId: Long,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { load() }
    private val _ui = MutableStateFlow(ReceiverUiState())

    val data: StateFlow<LoadState<ReceiverData>> = loader.state
    val ui: StateFlow<ReceiverUiState> = _ui.asStateFlow()

    private val current: ReceiverData? get() = loader.state.value.value

    /** Asking is available while the group is Active and the person has questions left. */
    val canAsk: Boolean
        get() = current?.let { it.phase == GroupPhase.Active && it.questions.size < QuestionLimits.MAX_QUESTIONS_PER_SANTA } == true

    fun refresh(): Job = loader.refresh()

    private suspend fun load(): ApiResult<ReceiverData> {
        val receiver = when (val result = api.receiver(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        val questions = when (val result = api.receiverQuestions(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        val group = when (val result = api.group(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        return ApiResult.Success(ReceiverData(receiver.toInfo(), questions.map { it.toQuestion() }, groupPhase(group.state)))
    }

    // --- the bottom sheet -------------------------------------------------------------------

    fun openAsk() {
        if (!canAsk) return
        _ui.update { it.copy(editor = TextEditor(), notice = null) }
    }

    fun dismissEditor() = _ui.update { it.copy(editor = null) }

    fun edit(text: String) = updateEditor { it.copy(text = text, error = null, general = null) }

    /** Checks what the server checks, then sends; returns the running work, or null when nothing is sent. */
    fun save(): Job? {
        val editor = _ui.value.editor ?: return null
        if (editor.saving) return null
        val text = checkedText(editor.text, QuestionLimits.MAX_QUESTION_LENGTH)
        if (text == null) {
            updateEditor { it.copy(error = Strings.textLength(QuestionLimits.MAX_QUESTION_LENGTH)) }
            return null
        }
        updateEditor { it.copy(saving = true, error = null, general = null) }
        return scope.launch {
            when (val result = api.askQuestion(groupId, text)) {
                is ApiResult.Success -> {
                    current?.let { data -> loader.replace(data.copy(questions = data.questions + result.value.toQuestion())) }
                    _ui.update { it.copy(editor = null) }
                }
                is ApiResult.Failure -> failed(result.error)
            }
        }
    }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    // --- when the server says no ----------------------------------------------------------------

    private suspend fun failed(error: ApiError) {
        when {
            error is ApiError.Validation && error.field == "text" -> updateEditor { it.copy(saving = false, error = error.message) }
            error is ApiError.Rejected && error.status == 409 -> {
                // the group is no longer Active, or the limit was reached elsewhere: the reloaded phase tells which
                reloadWith(null)
                if (loader.state.value.error == null) {
                    _ui.update { it.copy(notice = if (current?.phase == GroupPhase.Active) GiftNotice.LimitReached else GiftNotice.Locked) }
                }
            }
            error is ApiError.Rejected && error.status == 404 -> reloadWith(null)
            error == ApiError.Network -> updateEditor { it.copy(saving = false, general = Strings.SERVER_UNREACHABLE) }
            else -> updateEditor { it.copy(saving = false, general = Strings.SERVER_FAILED) }
        }
    }

    /** The sheet is over and the thread is read again; [notice] is said once the new thread is there. */
    private suspend fun reloadWith(notice: GiftNotice?) {
        _ui.update { it.copy(editor = null) }
        loader.refresh().join()
        _ui.update { it.copy(notice = notice) }
    }

    private fun updateEditor(change: (TextEditor) -> TextEditor) =
        _ui.update { state -> state.editor?.let { state.copy(editor = change(it)) } ?: state }
}
