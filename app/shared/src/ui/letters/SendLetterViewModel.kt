package com.usuallycoding.opensecretsanta.ui.letters

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.domain.gift.LetterLimits
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
 * The letters the person has sent their Santa, and the sheet for a new one. As with the questions, what the screen shows is what
 * the server answered: a letter appears once it is stored, and when the group changed behind the screen's back (archived, the
 * limit reached elsewhere) the list is reloaded and a notice says so. Nothing here says who the Santa is.
 */
class SendLetterViewModel(
    val groupId: Long,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { load() }
    private val _ui = MutableStateFlow(LetterUiState())

    val data: StateFlow<LoadState<SentLettersData>> = loader.state
    val ui: StateFlow<LetterUiState> = _ui.asStateFlow()

    private val current: SentLettersData? get() = loader.state.value.value

    /** Writing is available while the group is Active and the person has letters left. */
    val canSend: Boolean
        get() = current?.let { it.phase == GroupPhase.Active && it.letters.size < LetterLimits.MAX_LETTERS_PER_RECEIVER } == true

    fun refresh(): Job = loader.refresh()

    private suspend fun load(): ApiResult<SentLettersData> {
        val letters = when (val result = api.sentLetters(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        val group = when (val result = api.group(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        return ApiResult.Success(SentLettersData(letters.newestFirst(), groupPhase(group.state)))
    }

    // --- the bottom sheet -------------------------------------------------------------------

    fun openSheet() {
        if (!canSend) return
        _ui.update { it.copy(editor = LetterEditor(), notice = null) }
    }

    fun dismissEditor() = _ui.update { it.copy(editor = null) }

    fun editTitle(title: String) = updateEditor { it.copy(title = title, titleError = null, general = null) }

    fun editText(text: String) = updateEditor { it.copy(text = text, textError = null, general = null) }

    /** Checks what the server checks, both fields at once, then sends; returns the running work, or null when nothing is sent. */
    fun save(): Job? {
        val editor = _ui.value.editor ?: return null
        if (editor.saving) return null
        val title = editor.title.trim()
        val text = editor.text.trim()
        val titleError = fieldProblem("Title", title, LetterLimits.MAX_TITLE_LENGTH)
        val textError = fieldProblem("Description", text, LetterLimits.MAX_TEXT_LENGTH)
        if (titleError != null || textError != null) {
            updateEditor { it.copy(titleError = titleError, textError = textError) }
            return null
        }
        updateEditor { it.copy(saving = true, titleError = null, textError = null, general = null) }
        return scope.launch {
            when (val result = api.sendLetter(groupId, title, text)) {
                is ApiResult.Success -> {
                    current?.let { data -> loader.replace(data.copy(letters = listOf(result.value.toItem()) + data.letters)) }
                    _ui.update { it.copy(editor = null) }
                }
                is ApiResult.Failure -> failed(result.error)
            }
        }
    }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    /** What is wrong with one field, as the sheet says it, or null when it is fine. */
    private fun fieldProblem(label: String, value: String, max: Int): String? = when {
        value.isEmpty() -> Strings.fieldRequired(label)
        value.length > max -> Strings.textLength(max)
        else -> null
    }

    // --- when the server says no ----------------------------------------------------------------

    private suspend fun failed(error: ApiError) {
        when {
            error is ApiError.Validation && error.field == "title" -> updateEditor { it.copy(saving = false, titleError = error.message) }
            error is ApiError.Validation && error.field == "text" -> updateEditor { it.copy(saving = false, textError = error.message) }
            error is ApiError.Rejected && error.status == 409 -> {
                // the group is no longer Active, or the limit was reached elsewhere: the reloaded phase tells which
                reloadWith(null)
                if (loader.state.value.error == null) {
                    _ui.update { it.copy(notice = if (current?.phase == GroupPhase.Active) LetterNotice.LimitReached else LetterNotice.Locked) }
                }
            }
            error is ApiError.Rejected && error.status == 404 -> reloadWith(null)
            error == ApiError.Network -> updateEditor { it.copy(saving = false, general = Strings.SERVER_UNREACHABLE) }
            else -> updateEditor { it.copy(saving = false, general = Strings.SERVER_FAILED) }
        }
    }

    /** The sheet is over and the list is read again; [notice] is said once the new list is there. */
    private suspend fun reloadWith(notice: LetterNotice?) {
        _ui.update { it.copy(editor = null) }
        loader.refresh().join()
        _ui.update { it.copy(notice = notice) }
    }

    private fun updateEditor(change: (LetterEditor) -> LetterEditor) =
        _ui.update { state -> state.editor?.let { state.copy(editor = change(it)) } ?: state }
}
