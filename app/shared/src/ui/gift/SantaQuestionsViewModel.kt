package com.usuallycoding.opensecretsanta.ui.gift

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.domain.gift.QuestionLimits
import com.usuallycoding.opensecretsanta.ui.Strings
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
 * The questions the person's Santa asked, and the person's answers. It is built only from
 * [Question], which has no field that could name the Santa, so the screen has nothing to leak.
 */
class SantaQuestionsViewModel(
    val groupId: Long,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { load() }
    private val _ui = MutableStateFlow(ReceiverUiState())

    val data: StateFlow<LoadState<SantaQuestionsData>> = loader.state
    val ui: StateFlow<ReceiverUiState> = _ui.asStateFlow()

    private val current: SantaQuestionsData? get() = loader.state.value.value

    val canAnswer: Boolean get() = current?.canAnswer == true

    fun refresh(): Job = loader.refresh()

    private suspend fun load(): ApiResult<SantaQuestionsData> {
        val questions = when (val result = api.santaQuestions(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        val group = when (val result = api.group(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        return ApiResult.Success(SantaQuestionsData(questions.map { it.toQuestion() }, groupPhase(group.state)))
    }

    // --- the bottom sheet -------------------------------------------------------------------

    /** Opens the sheet for question [number], filled with the person's current answer if there is one. */
    fun openAnswer(number: Int) {
        val question = current?.takeIf { it.canAnswer }?.questions?.firstOrNull { it.number == number } ?: return
        _ui.update { it.copy(editor = TextEditor(number = question.number, text = question.answer.orEmpty()), notice = null) }
    }

    fun dismissEditor() = _ui.update { it.copy(editor = null) }

    fun edit(text: String) = updateEditor { it.copy(text = text, error = null, general = null) }

    /** Checks what the server checks, then sends; returns the running work, or null when nothing is sent. */
    fun save(): Job? {
        val editor = _ui.value.editor ?: return null
        val number = editor.number ?: return null
        if (editor.saving) return null
        val text = checkedText(editor.text, QuestionLimits.MAX_ANSWER_LENGTH)
        if (text == null) {
            updateEditor { it.copy(error = Strings.textLength(QuestionLimits.MAX_ANSWER_LENGTH)) }
            return null
        }
        updateEditor { it.copy(saving = true, error = null, general = null) }
        return scope.launch {
            when (val result = api.answerQuestion(groupId, number, text)) {
                is ApiResult.Success -> {
                    val saved = result.value.toQuestion()
                    current?.let { data -> loader.replace(data.copy(questions = data.questions.map { if (it.number == saved.number) saved else it })) }
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
            error is ApiError.Rejected && error.status == 409 -> reloadWith(GiftNotice.Locked)
            error is ApiError.Rejected && error.status == 404 -> reloadWith(GiftNotice.QuestionGone)
            error == ApiError.Network -> updateEditor { it.copy(saving = false, general = Strings.SERVER_UNREACHABLE) }
            else -> updateEditor { it.copy(saving = false, general = Strings.SERVER_FAILED) }
        }
    }

    /** The sheet is over and the thread is read again; [notice] is said once the new thread is there. */
    private suspend fun reloadWith(notice: GiftNotice) {
        _ui.update { it.copy(editor = null) }
        loader.refresh().join()
        // a vanished group is shown as such by the screen, which needs no notice
        _ui.update { it.copy(notice = if (loader.state.value.error == null) notice else null) }
    }

    private fun updateEditor(change: (TextEditor) -> TextEditor) =
        _ui.update { state -> state.editor?.let { state.copy(editor = change(it)) } ?: state }
}
