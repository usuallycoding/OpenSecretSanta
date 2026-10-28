package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.GroupDetailsRequest
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
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

/** What the form starts from: the group being edited, or nothing for a new group. */
data class FormSource(val group: AdminGroup?)

/**
 * The form for a group's details: a new group when [groupId] is null, otherwise that group, which is
 * loaded and fills the form once. What is checked before sending is what the server checks, through
 * the same [GroupDetails.of], so a typo is caught on every platform with the same sentence.
 */
class AdminGroupFormViewModel(
    val groupId: Long?,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val _form = MutableStateFlow(GroupForm())
    private val _saved = MutableStateFlow<Long?>(null)
    private var filled = groupId == null
    private val loader = Loader(scope) { load() }

    val form: StateFlow<GroupForm> = _form.asStateFlow()

    companion object {
        /** Stands for "no group yet" where a plain id has to be passed. */
        const val NEW_GROUP = -1L
    }

    /** The group that was just saved: the screen leaves when this appears. */
    val saved: StateFlow<Long?> = _saved.asStateFlow()

    /** What loading the group to edit looks like; a new group has nothing to load. */
    val loaded: StateFlow<LoadState<FormSource>> = loader.state

    fun refresh(): Job = loader.refresh()

    private suspend fun load(): ApiResult<FormSource> {
        if (groupId == null) return ApiResult.Success(FormSource(null))
        return when (val result = api.adminGroup(groupId)) {
            is ApiResult.Success -> {
                val group = result.value.toGroup()
                if (!filled) {
                    filled = true
                    _form.update {
                        it.copy(
                            name = group.name,
                            description = group.description.orEmpty(),
                            location = group.location.orEmpty(),
                            date = group.date.orEmpty(),
                            time = group.time.orEmpty(),
                        )
                    }
                }
                ApiResult.Success(FormSource(group))
            }
            is ApiResult.Failure -> result
        }
    }

    fun edit(field: GroupField, value: String) = _form.update { form ->
        val changed = when (field) {
            GroupField.Name -> form.copy(name = value)
            GroupField.Description -> form.copy(description = value)
            GroupField.Location -> form.copy(location = value)
            GroupField.Date -> form.copy(date = value)
            GroupField.Time -> form.copy(time = value)
        }
        changed.copy(errors = form.errors - field, general = null)
    }

    /** Checks the fields, then sends; returns the running work, or null when nothing is sent. */
    fun save(): Job? {
        val form = _form.value
        if (form.saving) return null
        val details = try {
            GroupDetails.of(form.name, form.description, form.location, form.date, form.time)
        } catch (e: DomainException.ValidationFailed) {
            val field = GroupField.entries.firstOrNull { it.key == e.field }
            val message = e.message.orEmpty().replaceFirstChar { it.uppercase() }
            _form.update { it.copy(errors = if (field != null) mapOf(field to message) else emptyMap(), general = if (field == null) message else null) }
            return null
        }
        _form.update { it.copy(saving = true, errors = emptyMap(), general = null) }
        val date = details.date?.toString()
        val time = details.time?.toString()
        return scope.launch {
            val result = if (groupId == null) {
                api.createGroup(CreateGroupRequest(details.name, details.description, details.location, date, time))
            } else {
                api.updateGroup(groupId, GroupDetailsRequest(details.name, details.description, details.location, date, time))
            }
            when (result) {
                is ApiResult.Success -> {
                    _form.update { it.copy(saving = false) }
                    _saved.value = result.value.id
                }
                is ApiResult.Failure -> failed(result.error)
            }
        }
    }

    private fun failed(error: ApiError) = _form.update { form ->
        val field = (error as? ApiError.Validation)?.field?.let { key -> GroupField.entries.firstOrNull { it.key == key } }
        when {
            error is ApiError.Validation && field != null -> form.copy(saving = false, errors = mapOf(field to error.message))
            error is ApiError.Validation -> form.copy(saving = false, general = error.message)
            error is ApiError.Rejected && error.status == 409 -> form.copy(saving = false, general = Strings.GROUP_CLOSED_FOR_EDITS)
            error is ApiError.Rejected && error.status == 404 -> form.copy(saving = false, general = Strings.GROUP_GONE)
            error == ApiError.Network -> form.copy(saving = false, general = Strings.SERVER_UNREACHABLE)
            else -> form.copy(saving = false, general = Strings.SERVER_FAILED)
        }
    }
}
