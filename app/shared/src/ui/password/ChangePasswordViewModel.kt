package com.usuallycoding.opensecretsanta.ui.password

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.PasswordPolicy
import com.usuallycoding.opensecretsanta.ui.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What is wrong, next to the field it is about; [general] is for problems that belong to none. */
data class PasswordErrors(
    val current: String? = null,
    val new: String? = null,
    val repeat: String? = null,
    val general: String? = null,
) {
    val any: Boolean get() = current != null || new != null || repeat != null || general != null
}

data class ChangePasswordUiState(
    val current: String = "",
    val new: String = "",
    val repeat: String = "",
    val inProgress: Boolean = false,
    /** The password was changed; the sheet then shows its confirmation instead of the fields. */
    val done: Boolean = false,
    val errors: PasswordErrors = PasswordErrors(),
) {
    val canSubmit: Boolean get() = current.isNotEmpty() && new.isNotEmpty() && repeat.isNotEmpty() && !inProgress
}

class ChangePasswordViewModel(
    private val session: SessionManager,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentChange(value: String) = _state.update { it.copy(current = value) }

    fun onNewChange(value: String) = _state.update { it.copy(new = value) }

    fun onRepeatChange(value: String) = _state.update { it.copy(repeat = value) }

    /** Back to empty fields and no message, so nothing that was typed outlives the sheet it was typed in. */
    fun reset() = _state.update { ChangePasswordUiState() }

    /** Starts the change and returns the running work, or null when there is nothing to send. */
    fun submit(): Job? {
        val input = _state.value
        if (!input.canSubmit) return null
        val local = check(input)
        if (local.any) {
            _state.update { it.copy(errors = local) }
            return null
        }
        _state.update { it.copy(inProgress = true, errors = PasswordErrors()) }
        return scope.launch {
            val result = session.changePassword(input.current, input.new)
            _state.update { state ->
                when (result) {
                    // The session has moved on and the screen is about to be replaced.
                    is ApiResult.Success -> state.copy(inProgress = false, done = true)
                    is ApiResult.Failure -> state.copy(inProgress = false, errors = errorsFor(result.error))
                }
            }
        }
    }

    /** The same rules the server applies, so the person hears about them before a request is sent. */
    private fun check(input: ChangePasswordUiState): PasswordErrors {
        val policy = try {
            PasswordPolicy.validateNew(input.new, current = input.current)
            null
        } catch (e: DomainException.ValidationFailed) {
            e.message
        }
        return PasswordErrors(
            new = policy,
            repeat = if (input.new != input.repeat) Strings.PASSWORDS_DO_NOT_MATCH else null,
        )
    }

    private fun errorsFor(error: ApiError): PasswordErrors = when (error) {
        is ApiError.Validation -> when (error.field) {
            "newPassword" -> PasswordErrors(new = error.message)
            "currentPassword" -> PasswordErrors(current = error.message)
            else -> PasswordErrors(general = error.message)
        }
        // The server answers a wrong current password with 403 "forbidden" and no field.
        is ApiError.Rejected -> if (error.status == 403) PasswordErrors(current = error.message) else PasswordErrors(general = Strings.SERVER_FAILED)
        ApiError.Network -> PasswordErrors(general = Strings.SERVER_UNREACHABLE)
        else -> PasswordErrors(general = Strings.SERVER_FAILED)
    }
}
