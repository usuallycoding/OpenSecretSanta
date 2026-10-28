package com.usuallycoding.opensecretsanta.ui.login

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why the last sign-in attempt did not work; the screen turns it into a sentence. */
sealed interface LoginProblem {
    data object WrongCredentials : LoginProblem
    data class Paused(val minutes: Int?) : LoginProblem
    data object Unreachable : LoginProblem
    data object ServerFailed : LoginProblem
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val inProgress: Boolean = false,
    /** Whether the web keeps the person signed in after the tab is closed; ticked until the person unticks it. */
    val staySignedIn: Boolean = true,
    val problem: LoginProblem? = null,
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotBlank() && !inProgress
}

class LoginViewModel(
    private val session: SessionManager,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value) }

    fun onStaySignedInChange(value: Boolean) = _state.update { it.copy(staySignedIn = value) }

    /** Starts the sign-in and returns the running work, or null when there is nothing to send. */
    fun submit(): Job? {
        val current = _state.value
        if (!current.canSubmit) return null
        _state.update { it.copy(inProgress = true, problem = null) }
        return scope.launch {
            val result = session.signIn(current.username.trim(), current.password, remember = current.staySignedIn)
            _state.update { state ->
                when (result) {
                    // The session has moved on and the screen is about to be replaced; nothing to show.
                    is ApiResult.Success -> state.copy(inProgress = false)
                    is ApiResult.Failure -> when (val error = result.error) {
                        ApiError.Unauthorized -> state.copy(inProgress = false, password = "", problem = LoginProblem.WrongCredentials)
                        is ApiError.RateLimited -> state.copy(inProgress = false, password = "", problem = LoginProblem.Paused(error.retryAfterSeconds?.let(::minutesFor)))
                        ApiError.Network -> state.copy(inProgress = false, problem = LoginProblem.Unreachable)
                        else -> state.copy(inProgress = false, problem = LoginProblem.ServerFailed)
                    }
                }
            }
        }
    }

    private fun minutesFor(seconds: Int): Int = maxOf(1, (seconds + 59) / 60)
}
