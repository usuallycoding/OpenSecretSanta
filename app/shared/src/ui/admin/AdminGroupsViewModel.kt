package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.ui.groups.GroupListFilter
import com.usuallycoding.opensecretsanta.ui.groups.adminStateChips
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import com.usuallycoding.opensecretsanta.ui.load.loadMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The dialog that offers a copy of [group], whether it is being made ([busy]) and what went wrong, if anything. */
data class DuplicateUi(val group: AdminGroup, val busy: Boolean = false, val error: String? = null) {
    val copyName: String get() = copyName(group.name)
}

private const val COPY_PREFIX = "Copy of "

/** "Copy of <name>", cut with an ellipsis when that would be longer than a group name may be. */
fun copyName(name: String): String {
    val full = COPY_PREFIX + name
    val limit = GroupDetails.MAX_NAME_LENGTH
    return if (full.length <= limit) full else full.take(limit - 1) + "…"
}

/**
 * The administrator's own groups, in the order the server returns them (newest first), the search and chips that narrow
 * them, and the copying of one of them.
 */
class AdminGroupsViewModel(
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.adminGroups().map { groups -> groups.map { it.toGroup() } } }
    private val _duplicate = MutableStateFlow<DuplicateUi?>(null)
    private val _created = MutableStateFlow<Long?>(null)

    val state: StateFlow<LoadState<List<AdminGroup>>> = loader.state

    /** What the administrator searched for and which states they chose; the screen applies it to the loaded groups. */
    val filter = GroupListFilter(adminStateChips)

    /** The copy being offered; null when there is none. */
    val duplicate: StateFlow<DuplicateUi?> = _duplicate.asStateFlow()

    /** The group that was just created by a copy: the screen opens it and then calls [createdHandled]. */
    val created: StateFlow<Long?> = _created.asStateFlow()

    fun refresh(): Job = loader.refresh()

    fun askDuplicate(group: AdminGroup) {
        _duplicate.value = DuplicateUi(group)
    }

    fun cancelDuplicate() {
        // a copy that is being made is let finish; the dialog stays until it has
        if (_duplicate.value?.busy != true) _duplicate.value = null
    }

    fun createdHandled() {
        _created.value = null
    }

    /** Creates the Draft: the details and participants of the original, under the name of the copy. */
    fun confirmDuplicate(): Job? {
        val ui = _duplicate.value ?: return null
        if (ui.busy) return null
        _duplicate.value = ui.copy(busy = true, error = null)
        val group = ui.group
        return scope.launch {
            val request = CreateGroupRequest(
                name = ui.copyName,
                description = group.description,
                location = group.location,
                date = group.date,
                time = group.time,
                participants = group.participants.map { it.username },
            )
            when (val result = api.createGroup(request)) {
                is ApiResult.Success -> {
                    _duplicate.value = null
                    _created.value = result.value.id
                }
                is ApiResult.Failure -> _duplicate.update { it?.copy(busy = false, error = (result.error as? ApiError.Validation)?.message ?: result.error.loadMessage()) }
            }
        }
    }
}
