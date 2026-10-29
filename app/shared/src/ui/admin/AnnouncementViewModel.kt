package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.AnnouncementRequest
import com.usuallycoding.opensecretsanta.contract.AnnouncementResponse
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the announcement sheet shows. A channel counts as chosen only when the server can use it. */
data class AnnouncementUi(
    val emailChosen: Boolean = true,
    val pushChosen: Boolean = true,
    /** What the server can send, from the account; both false until it has been read. */
    val emailAvailable: Boolean = false,
    val pushAvailable: Boolean = false,
    val ready: Boolean = false,
    val confirming: Boolean = false,
    val sending: Boolean = false,
    val result: AnnouncementResponse? = null,
    val error: String? = null,
) {
    val email: Boolean get() = emailChosen && emailAvailable
    val push: Boolean get() = pushChosen && pushAvailable

    companion object {
        const val MAX_TITLE = 100
        const val MAX_BODY = 2000
        const val PUSH_LIMIT = 300
    }
}

/**
 * Writing an announcement to a group's participants: the title, the message and the ways to send it. Which ways the
 * server has is read from the account. Nothing is kept once the sheet is closed.
 */
class AnnouncementViewModel(
    private val groupId: Long,
    val groupName: String,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val _ui = MutableStateFlow(AnnouncementUi())

    // Compose state, not flows: a text field that keeps its own copy of what is typed takes the text it is given back after
    // every keystroke, and a flow delivers it a frame late, so letters typed meanwhile were taken away again
    private var typedTitle by mutableStateOf("")
    private var typedBody by mutableStateOf("")

    val title: String get() = typedTitle
    val body: String get() = typedBody

    /** Something to send, a way to send it, and nothing in flight. */
    val canSend: Boolean
        get() = _ui.value.let { it.ready && !it.sending && (it.email || it.push) } && typedTitle.isNotBlank() && typedBody.isNotBlank()

    /** The push notification shows only the start of a long message. */
    val pushCutsMessage: Boolean get() = _ui.value.push && typedBody.trim().length > AnnouncementUi.PUSH_LIMIT
    private val loader = Loader(scope) { api.account().also { result -> if (result is ApiResult.Success) onLoaded(result.value) } }

    val ui: StateFlow<AnnouncementUi> = _ui.asStateFlow()
    val load: StateFlow<LoadState<AccountDto>> = loader.state

    fun refresh(): Job = loader.refresh()

    private fun onLoaded(account: AccountDto) =
        _ui.update { it.copy(emailAvailable = account.emailAvailable, pushAvailable = account.pushAvailable, ready = true) }

    /** The subject of the email and the title of the push notification, as they will read. */
    fun subject(): String = Strings.announceSubject(typedTitle, groupName)

    fun setTitle(text: String) {
        typedTitle = text.replace(Regex("[\\r\\n]+"), " ").take(AnnouncementUi.MAX_TITLE)
        _ui.update { it.copy(error = null) }
    }

    fun setBody(text: String) {
        typedBody = text.take(AnnouncementUi.MAX_BODY)
        _ui.update { it.copy(error = null) }
    }

    fun setEmail(on: Boolean) = _ui.update { if (it.emailAvailable) it.copy(emailChosen = on, error = null) else it }

    fun setPush(on: Boolean) = _ui.update { if (it.pushAvailable) it.copy(pushChosen = on, error = null) else it }

    /** Send was pressed: nothing goes out until it is confirmed. */
    fun ask() {
        if (canSend) _ui.update { it.copy(confirming = true) }
    }

    fun cancelConfirm() = _ui.update { it.copy(confirming = false) }

    fun send(): Job {
        val ui = _ui.value
        if (!canSend) return Job().apply { complete() }
        _ui.update { it.copy(confirming = false, sending = true, error = null) }
        return scope.launch {
            val request = AnnouncementRequest(typedTitle.trim(), typedBody.trim(), email = ui.email, push = ui.push)
            when (val result = api.announce(groupId, request)) {
                is ApiResult.Success -> _ui.update { it.copy(sending = false, result = result.value) }
                is ApiResult.Failure -> _ui.update { it.copy(sending = false, error = result.error.sendMessage()) }
            }
        }
    }

    private fun ApiError.sendMessage(): String = when (this) {
        is ApiError.Validation -> message
        is ApiError.Rejected -> message
        ApiError.Network -> Strings.SERVER_UNREACHABLE
        else -> Strings.SERVER_FAILED
    }
}
