package com.usuallycoding.opensecretsanta.ui.delivery

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.DeliveryInfoDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfo
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
data class DeliveryForm(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val deliveryInfo: String = "",
) {
    /** What a Santa needs to post a gift. */
    val hasNameAndAddress: Boolean get() = fullName.isNotBlank() && address.isNotBlank()
}

enum class DeliveryField(val key: String, val label: String) {
    FullName("fullName", "Full name"),
    Email("email", "Email"),
    Phone("phone", "Phone"),
    Address("address", "Address"),
    DeliveryInfo("deliveryInfo", "Delivery information"),
}

/** Problems next to their fields, and one for problems that belong to none. */
data class DeliveryErrors(val fields: Map<DeliveryField, String> = emptyMap(), val general: String? = null) {
    val any: Boolean get() = fields.isNotEmpty() || general != null
}

data class DeliveryUiState(
    val form: DeliveryForm = DeliveryForm(),
    /** What the server has, or null until the delivery info has loaded. */
    val saved: DeliveryForm? = null,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val errors: DeliveryErrors = DeliveryErrors(),
) {
    val dirty: Boolean get() = saved != null && form != saved
    val canSave: Boolean get() = dirty && !saving
}

private fun DeliveryInfoDto.toForm() = DeliveryForm(
    fullName = fullName.orEmpty(),
    email = email.orEmpty(),
    phone = phone.orEmpty(),
    address = address.orEmpty(),
    deliveryInfo = deliveryInfo.orEmpty(),
)

private fun fieldOf(key: String?): DeliveryField? = DeliveryField.entries.firstOrNull { it.key == key }

/** The server and `DeliveryInfo.of` name the field in their messages; the person should see its label instead. */
private fun friendly(field: DeliveryField, message: String): String =
    if ("at most" in message) "${field.label} must be at most ${DeliveryInfo.MAX_LENGTH} characters long." else message

/** The page for one group's delivery info: what the person tells that group about where and how a gift can reach them. */
class DeliveryInfoViewModel(
    val groupId: Long,
    private val api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val scope = scope
    private val _ui = MutableStateFlow(DeliveryUiState())
    private val loader = Loader(scope) { api.deliveryInfo(groupId).also { result -> if (result is ApiResult.Success) onLoaded(result.value) } }

    /** Whether the delivery info has loaded; the form below is shown once it has. */
    val load: StateFlow<LoadState<DeliveryInfoDto>> = loader.state
    val ui: StateFlow<DeliveryUiState> = _ui.asStateFlow()

    /** Applied as part of the load itself, so it has happened by the time the load is reported as finished. */
    private fun onLoaded(info: DeliveryInfoDto) {
        val fromServer = info.toForm()
        _ui.update { current ->
            val untouched = current.saved == null || current.form == current.saved
            // A reload brings the server's values in as the starting point, but never replaces what the person typed.
            current.copy(
                saved = fromServer,
                form = if (untouched) fromServer else current.form,
            )
        }
    }

    fun refresh(): Job = loader.refresh()

    fun edit(field: DeliveryField, value: String) = _ui.update { current ->
        current.copy(
            form = when (field) {
                DeliveryField.FullName -> current.form.copy(fullName = value)
                DeliveryField.Email -> current.form.copy(email = value)
                DeliveryField.Phone -> current.form.copy(phone = value)
                DeliveryField.Address -> current.form.copy(address = value)
                DeliveryField.DeliveryInfo -> current.form.copy(deliveryInfo = value)
            },
            justSaved = false,
            errors = current.errors.copy(fields = current.errors.fields - field, general = null),
        )
    }

    /** Checks what the server checks, then saves all five fields; returns the running work, or null when nothing is sent. */
    fun save(): Job? {
        val state = _ui.value
        if (!state.canSave) return null
        val info = try {
            with(state.form) { DeliveryInfo.of(fullName, email, phone, address, deliveryInfo) }
        } catch (e: DomainException.ValidationFailed) {
            val field = fieldOf(e.field)
            _ui.update {
                it.copy(errors = if (field != null) DeliveryErrors(mapOf(field to friendly(field, e.message.orEmpty()))) else DeliveryErrors(general = e.message))
            }
            return null
        }
        _ui.update { it.copy(saving = true, justSaved = false, errors = DeliveryErrors()) }
        return scope.launch {
            val request = DeliveryInfoDto(
                fullName = info.fullName,
                email = info.email,
                phone = info.phone,
                address = info.address,
                deliveryInfo = info.deliveryInfo,
            )
            when (val result = api.updateDeliveryInfo(groupId, request)) {
                is ApiResult.Success -> {
                    val saved = result.value.toForm()
                    _ui.update { it.copy(saving = false, justSaved = true, saved = saved, form = saved) }
                }
                is ApiResult.Failure -> _ui.update { it.copy(saving = false, errors = errorsFor(result.error)) }
            }
        }
    }

    private fun errorsFor(error: ApiError): DeliveryErrors = when (error) {
        is ApiError.Validation -> {
            val field = fieldOf(error.field)
            if (field != null) DeliveryErrors(mapOf(field to friendly(field, error.message))) else DeliveryErrors(general = error.message)
        }
        is ApiError.Rejected -> when (error.status) {
            409 -> DeliveryErrors(general = Strings.GROUP_CLOSED_FOR_EDITS)
            404 -> DeliveryErrors(general = Strings.GROUP_GONE)
            else -> DeliveryErrors(general = Strings.SERVER_FAILED)
        }
        ApiError.Network -> DeliveryErrors(general = Strings.SERVER_UNREACHABLE)
        else -> DeliveryErrors(general = Strings.SERVER_FAILED)
    }
}
