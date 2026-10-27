package com.usuallycoding.opensecretsanta.ui.profile

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.ProfileUpdateRequest
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Profile
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

/** The five editable fields as text; a field the person has not filled in is empty. */
data class ProfileForm(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val deliveryInfo: String = "",
) {
    /** What a Santa needs to post a gift. */
    val hasNameAndAddress: Boolean get() = fullName.isNotBlank() && address.isNotBlank()
}

enum class ProfileField(val key: String, val label: String) {
    FullName("fullName", "Full name"),
    Email("email", "Email"),
    Phone("phone", "Phone"),
    Address("address", "Address"),
    DeliveryInfo("deliveryInfo", "Delivery information"),
}

/** Problems next to their fields, and one for problems that belong to none. */
data class ProfileErrors(val fields: Map<ProfileField, String> = emptyMap(), val general: String? = null) {
    val any: Boolean get() = fields.isNotEmpty() || general != null
}

data class ProfileUiState(
    val username: String = "",
    val form: ProfileForm = ProfileForm(),
    /** What the server has, or null until the profile has loaded. */
    val saved: ProfileForm? = null,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val errors: ProfileErrors = ProfileErrors(),
) {
    val dirty: Boolean get() = saved != null && form != saved
    val canSave: Boolean get() = dirty && !saving
}

private fun AccountDto.toForm() = ProfileForm(
    fullName = fullName.orEmpty(),
    email = email.orEmpty(),
    phone = phone.orEmpty(),
    address = address.orEmpty(),
    deliveryInfo = deliveryInfo.orEmpty(),
)

private fun fieldOf(key: String?): ProfileField? = ProfileField.entries.firstOrNull { it.key == key }

/** The server and `Profile.of` name the field in their messages; the person should see its label instead. */
private fun friendly(field: ProfileField, message: String): String =
    if ("at most" in message) "${field.label} must be at most ${Profile.MAX_LENGTH} characters long." else message

class ProfileViewModel(
    private val api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val scope = scope
    private val _ui = MutableStateFlow(ProfileUiState())
    private val loader = Loader(scope) { api.account().also { result -> if (result is ApiResult.Success) onLoaded(result.value) } }

    /** Whether the profile has loaded; the form below is shown once it has. */
    val load: StateFlow<LoadState<AccountDto>> = loader.state
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    /** Applied as part of the load itself, so it has happened by the time the load is reported as finished. */
    private fun onLoaded(account: AccountDto) {
        val fromServer = account.toForm()
        _ui.update { current ->
            val untouched = current.saved == null || current.form == current.saved
            // A reload brings the server's values in as the starting point, but never replaces what the person typed.
            current.copy(
                username = account.username,
                saved = fromServer,
                form = if (untouched) fromServer else current.form,
            )
        }
    }

    fun refresh(): Job = loader.refresh()

    fun edit(field: ProfileField, value: String) = _ui.update { current ->
        current.copy(
            form = when (field) {
                ProfileField.FullName -> current.form.copy(fullName = value)
                ProfileField.Email -> current.form.copy(email = value)
                ProfileField.Phone -> current.form.copy(phone = value)
                ProfileField.Address -> current.form.copy(address = value)
                ProfileField.DeliveryInfo -> current.form.copy(deliveryInfo = value)
            },
            justSaved = false,
            errors = current.errors.copy(fields = current.errors.fields - field, general = null),
        )
    }

    /** Checks what the server checks, then saves all five fields; returns the running work, or null when nothing is sent. */
    fun save(): Job? {
        val state = _ui.value
        if (!state.canSave) return null
        val profile = try {
            with(state.form) { Profile.of(fullName, email, phone, address, deliveryInfo) }
        } catch (e: DomainException.ValidationFailed) {
            val field = fieldOf(e.field)
            _ui.update {
                it.copy(errors = if (field != null) ProfileErrors(mapOf(field to friendly(field, e.message.orEmpty()))) else ProfileErrors(general = e.message))
            }
            return null
        }
        _ui.update { it.copy(saving = true, justSaved = false, errors = ProfileErrors()) }
        return scope.launch {
            val request = ProfileUpdateRequest(
                username = state.username,
                fullName = profile.fullName,
                email = profile.email,
                phone = profile.phone,
                address = profile.address,
                deliveryInfo = profile.deliveryInfo,
            )
            when (val result = api.updateProfile(request)) {
                is ApiResult.Success -> {
                    val saved = result.value.toForm()
                    _ui.update { it.copy(saving = false, justSaved = true, saved = saved, form = saved) }
                }
                is ApiResult.Failure -> _ui.update { it.copy(saving = false, errors = errorsFor(result.error)) }
            }
        }
    }

    private fun errorsFor(error: ApiError): ProfileErrors = when (error) {
        is ApiError.Validation -> {
            val field = fieldOf(error.field)
            if (field != null) ProfileErrors(mapOf(field to friendly(field, error.message))) else ProfileErrors(general = error.message)
        }
        ApiError.Network -> ProfileErrors(general = Strings.SERVER_UNREACHABLE)
        else -> ProfileErrors(general = Strings.SERVER_FAILED)
    }
}
