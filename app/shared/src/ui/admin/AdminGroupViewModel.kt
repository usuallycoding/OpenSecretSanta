package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.user.Username
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

/**
 * One of the administrator's groups and everything that can be done to it. The server's answer is
 * always what is shown; when the group changed behind the screen's back it is reloaded and a notice
 * says so. Temporary passwords live only in [ui] while their dialog is open: they are never written
 * into the loaded group.
 */
class AdminGroupViewModel(
    val groupId: Long,
    val me: String,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val _draw = MutableStateFlow<DrawState>(DrawState.None)
    private val _ui = MutableStateFlow(AdminGroupUi())
    private val loader = Loader(scope) {
        api.adminGroup(groupId).map { it.toGroup() }.also { result ->
            if (result is ApiResult.Success) loadDraw(result.value.phase, result.value.participants)
        }
    }

    val data: StateFlow<LoadState<AdminGroup>> = loader.state
    val ui: StateFlow<AdminGroupUi> = _ui.asStateFlow()

    /** Who gives to whom, once the names are drawn; a failure to read it leaves the rest of the page as it is. */
    val draw: StateFlow<DrawState> = _draw.asStateFlow()

    private val current: AdminGroup? get() = loader.state.value.value

    fun refresh(): Job = loader.refresh()

    // --- oversight (read-only) ------------------------------------------------------------------------

    /** The draw and the question threads exist once the names are drawn. */
    val canReadDraw: Boolean
        get() = current?.phase.let { it == AdminPhase.Active || it == AdminPhase.Archived }

    /** The wishlist link of a Pending group's participants; once the names are drawn the pair sheet shows the receiver's list. */
    val canReadPendingWishlists: Boolean get() = current?.phase == AdminPhase.Pending

    /** A person's details can be opened from the pair list of a drawn group and from the participants of a Pending one. */
    fun openUserSheet(username: String) {
        if (canReadDraw || canReadPendingWishlists) _ui.update { it.copy(sheet = GroupSheet.User(username)) }
    }

    fun openPairSheet(santa: String, receiver: String) {
        if (canReadDraw) _ui.update { it.copy(sheet = GroupSheet.Pair(santa, receiver)) }
    }

    fun openWishlistSheet(username: String) {
        if (canReadPendingWishlists) _ui.update { it.copy(sheet = GroupSheet.Wishlist(username)) }
    }

    fun dismissSheet() = _ui.update { it.copy(sheet = null) }

    /** Reads the draw for a group that has one; a group that has none is not asked. */
    private suspend fun loadDraw(phase: AdminPhase, participants: List<AdminParticipant>) {
        if (phase != AdminPhase.Active && phase != AdminPhase.Archived) {
            _draw.value = DrawState.None
            return
        }
        _draw.value = when (val result = api.adminAssignments(groupId)) {
            is ApiResult.Success -> DrawState.Pairs(result.value.map { it.toPair(me, participants) })
            is ApiResult.Failure -> DrawState.Failed
        }
    }

    // --- what can be done, by phase -----------------------------------------------------------------

    val canEditDetails: Boolean
        get() = current?.phase.let { it == AdminPhase.Draft || it == AdminPhase.Pending || it == AdminPhase.Active }

    val canEditParticipants: Boolean
        get() = current?.phase.let { it == AdminPhase.Draft || it == AdminPhase.Pending }

    /** Any group can be deleted; one with data of its own asks for a second confirmation first. */
    val canDelete: Boolean get() = current != null

    /** The next step for the state, or null when there is none (an Archived group). */
    val nextStep: AdminStep?
        get() = when (current?.phase) {
            AdminPhase.Draft -> AdminStep.MakePending
            AdminPhase.Pending -> AdminStep.DrawNames
            AdminPhase.Active -> AdminStep.Archive
            else -> null
        }

    /** Drawing the names needs enough people; with fewer the action says so instead of sending a request that fails. */
    val drawNeedsMorePeople: Boolean
        get() = current?.let { it.phase == AdminPhase.Pending && it.participants.size < ActivateGroup.MIN_PARTICIPANTS } == true

    // --- the participants sheet ------------------------------------------------------------------------

    fun openParticipants() {
        val group = current?.takeIf { canEditParticipants } ?: return
        _ui.update { it.copy(participants = ParticipantsEditor(group.participants.map { p -> p.username }), notice = null) }
    }

    fun dismissParticipants() = _ui.update { it.copy(participants = null) }

    fun typeUsername(text: String) = updateEditor { it.copy(input = text, error = null, general = null) }

    /** Adds the typed username after checking it as the server does; a name already listed is not added twice. */
    fun addUsername() = updateEditor { editor ->
        val name = try {
            Username.parse(editor.input.trim()).value
        } catch (e: DomainException.ValidationFailed) {
            return@updateEditor editor.copy(error = e.message)
        }
        if (name in editor.names) {
            editor.copy(error = Strings.ADMIN_ALREADY_LISTED)
        } else {
            editor.copy(names = editor.names + name, input = "", error = null)
        }
    }

    fun removeUsername(name: String) = updateEditor { it.copy(names = it.names - name, general = null) }

    /** Replaces the whole list; returns the running work, or null when nothing is sent. */
    fun saveParticipants(): Job? {
        val editor = _ui.value.participants ?: return null
        if (editor.saving) return null
        updateEditor { it.copy(saving = true, error = null, general = null) }
        return scope.launch {
            when (val result = api.setParticipants(groupId, editor.names)) {
                is ApiResult.Success -> {
                    applyChange(result.value)
                    _ui.update { it.copy(participants = null) }
                }
                is ApiResult.Failure -> participantsFailed(result.error)
            }
        }
    }

    // --- the steps -------------------------------------------------------------------------------------

    /** Asks for a confirmation of [step], if the group allows it now. */
    fun ask(step: AdminStep) {
        val allowed = when (step) {
            AdminStep.Delete -> canDelete
            AdminStep.DrawNames -> nextStep == step && !drawNeedsMorePeople
            else -> nextStep == step
        }
        if (allowed && !_ui.value.busy) _ui.update { it.copy(confirming = step, notice = null) }
    }

    fun cancelStep() = _ui.update { it.copy(confirming = null) }

    fun cancelDelete() = _ui.update { it.copy(confirmingDelete = null) }

    /** The second confirmation of a deletion: sends it again with `confirm=true`. */
    fun confirmDelete(): Job? {
        if (_ui.value.confirmingDelete == null || _ui.value.busy) return null
        _ui.update { it.copy(confirmingDelete = null, busy = true) }
        return scope.launch {
            when (val result = api.deleteGroup(groupId, confirm = true)) {
                is ApiResult.Success -> _ui.update { it.copy(busy = false, deleted = true) }
                is ApiResult.Failure -> stepFailed(result.error)
            }
        }
    }

    fun confirmStep(): Job? {
        val step = _ui.value.confirming ?: return null
        if (_ui.value.busy) return null
        _ui.update { it.copy(confirming = null, busy = true) }
        return scope.launch {
            if (step == AdminStep.Delete) {
                when (val result = api.deleteGroup(groupId)) {
                    is ApiResult.Success -> _ui.update { it.copy(busy = false, deleted = true) }
                    is ApiResult.Failure -> {
                        val error = result.error
                        // the group has data that would go with it: say what, and delete only when that is confirmed too
                        if (error is ApiError.ConfirmationRequired) _ui.update { it.copy(busy = false, confirmingDelete = error.impact) }
                        else stepFailed(error)
                    }
                }
                return@launch
            }
            val result = when (step) {
                AdminStep.MakePending -> api.makePending(groupId)
                AdminStep.DrawNames -> api.activate(groupId)
                else -> api.archive(groupId)
            }
            when (result) {
                is ApiResult.Success -> {
                    applyChange(result.value)
                    _ui.update { it.copy(busy = false) }
                }
                is ApiResult.Failure -> stepFailed(result.error)
            }
        }
    }

    // --- the passwords and notices ----------------------------------------------------------------------

    fun dismissPasswords() = _ui.update { it.copy(passwords = null) }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    // --- internals ----------------------------------------------------------------------------------------

    /** The server's answer replaces the group; accounts it created are shown once and kept nowhere else. */
    private suspend fun applyChange(change: GroupChangeResponse) {
        val group = change.group.toGroup()
        loader.replace(group)
        loadDraw(group.phase, group.participants)
        val created = change.createdAccounts.map { CreatedAccount(it.username, it.temporaryPassword) }
        if (created.isNotEmpty()) _ui.update { it.copy(passwords = created) }
    }

    private suspend fun participantsFailed(error: ApiError) {
        when {
            error is ApiError.Validation && error.field == "participants" -> updateEditor { it.copy(saving = false, error = error.message) }
            error is ApiError.Rejected && error.status == 409 -> reloadWith(AdminNotice.RosterFrozen)
            error is ApiError.Rejected && error.status == 404 -> reloadWith(null)
            error == ApiError.Network -> updateEditor { it.copy(saving = false, general = Strings.SERVER_UNREACHABLE) }
            else -> updateEditor { it.copy(saving = false, general = Strings.SERVER_FAILED) }
        }
    }

    private suspend fun stepFailed(error: ApiError) {
        when {
            error is ApiError.Rejected && error.status == 404 -> reloadWith(null)
            error is ApiError.Rejected && error.status == 409 -> reloadWith(AdminNotice.ChangedElsewhere)
            error is ApiError.Validation -> reloadWith(AdminNotice.ChangedElsewhere)
            else -> _ui.update { it.copy(busy = false, notice = AdminNotice.Unreachable) }
        }
    }

    /** The sheet and the confirmation are over, the group is read again, and [notice] is said once the new group is there. */
    private suspend fun reloadWith(notice: AdminNotice?) {
        _ui.update { it.copy(participants = null, confirming = null, confirmingDelete = null, busy = false) }
        loader.refresh().join()
        _ui.update { it.copy(notice = if (loader.state.value.error == null) notice else null) }
    }

    private fun updateEditor(change: (ParticipantsEditor) -> ParticipantsEditor) =
        _ui.update { state -> state.participants?.let { state.copy(participants = change(it)) } ?: state }
}
