package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistEntry
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
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
 * The person's own wishlist for one group. The server's answer is always what the screen shows: a
 * change is never displayed before the server has accepted it, and when something changed behind the
 * screen's back (another device, an archived group, a deleted item) the list is reloaded and a notice
 * says what happened.
 */
class WishlistViewModel(
    val groupId: Long,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { load() }
    private val _ui = MutableStateFlow(WishlistUiState())

    val data: StateFlow<LoadState<WishlistData>> = loader.state
    val ui: StateFlow<WishlistUiState> = _ui.asStateFlow()

    private val current: WishlistData? get() = loader.state.value.value

    val editable: Boolean get() = current?.editable == true

    /** "Add" is available while the list can be changed and has room. */
    val canAdd: Boolean get() = current?.let { it.editable && it.items.size < WishlistLimits.MAX_ITEMS } == true

    fun refresh(): Job = loader.refresh()

    /** The list and the group are read together, so the screen knows from one place what may be done. */
    private suspend fun load(): ApiResult<WishlistData> {
        val list = when (val result = api.wishlist(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        val group = when (val result = api.group(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        return ApiResult.Success(WishlistData(list.map { it.toItem() }, groupPhase(group.state)))
    }

    // --- the bottom sheet -------------------------------------------------------------------

    fun openAdd() {
        if (!canAdd) return
        _ui.update { it.copy(editor = ItemEditor(itemId = null), notice = null) }
    }

    fun openEdit(itemId: Long) {
        val item = current?.takeIf { it.editable }?.items?.firstOrNull { it.id == itemId } ?: return
        _ui.update { it.copy(editor = ItemEditor(item.id, item.title, item.note.orEmpty(), item.url.orEmpty()), notice = null) }
    }

    fun dismissEditor() = _ui.update { it.copy(editor = null) }

    fun edit(field: WishField, value: String) = updateEditor { editor ->
        val changed = when (field) {
            WishField.Title -> editor.copy(title = value)
            WishField.Note -> editor.copy(note = value)
            WishField.Url -> editor.copy(url = value)
        }
        changed.copy(errors = editor.errors - field, general = null)
    }

    /** Checks what the server checks, then sends; returns the running work, or null when nothing is sent. */
    fun save(): Job? {
        val editor = _ui.value.editor ?: return null
        if (editor.saving) return null
        val entry = try {
            WishlistEntry.of(editor.title, editor.note, editor.url)
        } catch (e: DomainException.ValidationFailed) {
            val field = fieldOf(e.field)
            updateEditor { it.copy(errors = if (field != null) mapOf(field to e.message.orEmpty()) else emptyMap(), general = if (field == null) e.message else null) }
            return null
        }
        updateEditor { it.copy(saving = true, errors = emptyMap(), general = null) }
        val request = WishlistItemRequest(entry.title, entry.note, entry.url)
        return scope.launch {
            val result = if (editor.itemId == null) {
                api.addWishlistItem(groupId, request)
            } else {
                api.updateWishlistItem(groupId, editor.itemId, request)
            }
            when (result) {
                is ApiResult.Success -> {
                    val saved = result.value.toItem()
                    current?.let { data ->
                        val items = if (editor.itemId == null) data.items + saved else data.items.map { if (it.id == saved.id) saved else it }
                        loader.replace(data.copy(items = items))
                    }
                    _ui.update { it.copy(editor = null) }
                }
                is ApiResult.Failure -> failed(result.error, itemRelated = editor.itemId != null, fromEditor = true)
            }
        }
    }

    // --- deleting -----------------------------------------------------------------------------

    /** Asks for a confirmation to delete the item that the sheet is editing. */
    fun requestDelete() {
        val id = _ui.value.editor?.itemId ?: return
        val item = current?.takeIf { it.editable }?.items?.firstOrNull { it.id == id } ?: return
        _ui.update { it.copy(deleting = item) }
    }

    fun cancelDelete() = _ui.update { it.copy(deleting = null) }

    fun confirmDelete(): Job? {
        val item = _ui.value.deleting ?: return null
        _ui.update { it.copy(deleting = null) }
        return scope.launch {
            when (val result = api.deleteWishlistItem(groupId, item.id)) {
                is ApiResult.Success -> {
                    current?.let { data -> loader.replace(data.copy(items = data.items.filterNot { it.id == item.id })) }
                    _ui.update { it.copy(editor = null) }
                }
                is ApiResult.Failure -> failed(result.error, itemRelated = true, fromEditor = false)
            }
        }
    }

    // --- reordering ---------------------------------------------------------------------------

    fun moveUp(itemId: Long): Job? = move(itemId, -1)

    fun moveDown(itemId: Long): Job? = move(itemId, +1)

    /** Sends every id in the new order; the order the server answers with is the one shown. */
    private fun move(itemId: Long, by: Int): Job? {
        val data = current?.takeIf { it.editable } ?: return null
        if (_ui.value.moving) return null
        val from = data.items.indexOfFirst { it.id == itemId }
        val to = from + by
        if (from < 0 || to !in data.items.indices) return null
        val ids = data.items.map { it.id }.toMutableList()
        ids.add(to, ids.removeAt(from))
        _ui.update { it.copy(moving = true, notice = null) }
        return scope.launch {
            when (val result = api.reorderWishlist(groupId, ids)) {
                is ApiResult.Success -> {
                    loader.replace(data.copy(items = result.value.map { it.toItem() }))
                    _ui.update { it.copy(moving = false) }
                }
                is ApiResult.Failure -> {
                    _ui.update { it.copy(moving = false) }
                    failed(result.error, itemRelated = false, fromEditor = false)
                }
            }
        }
    }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    // --- when the server says no ----------------------------------------------------------------

    private suspend fun failed(error: ApiError, itemRelated: Boolean, fromEditor: Boolean) {
        when {
            error is ApiError.Validation && fieldOf(error.field) != null && fromEditor ->
                updateEditor { it.copy(saving = false, errors = mapOf(fieldOf(error.field)!! to error.message)) }
            error is ApiError.Validation && error.field == "itemIds" -> reloadWith(WishlistNotice.ChangedElsewhere)
            error is ApiError.Rejected && error.status == 409 -> {
                // archived, or the list filled up elsewhere: the reloaded phase tells which
                reloadWith(null)
                if (loader.state.value.error == null) {
                    _ui.update { it.copy(notice = if (editable) WishlistNotice.ListFull else WishlistNotice.Locked) }
                }
            }
            error is ApiError.Rejected && error.status == 404 -> reloadWith(if (itemRelated) WishlistNotice.ItemGone else null)
            error == ApiError.Network && fromEditor ->
                updateEditor { it.copy(saving = false, general = Strings.SERVER_UNREACHABLE) }
            error == ApiError.Network -> _ui.update { it.copy(notice = WishlistNotice.Unreachable) }
            fromEditor -> updateEditor { it.copy(saving = false, general = Strings.SERVER_FAILED) }
            else -> _ui.update { it.copy(notice = WishlistNotice.Unreachable) }
        }
    }

    /** The sheet and the confirmation are over, the list is read again, and [notice] is said once the new list is there. */
    private suspend fun reloadWith(notice: WishlistNotice?) {
        _ui.update { it.copy(editor = null, deleting = null, moving = false) }
        loader.refresh().join()
        _ui.update { it.copy(notice = notice) }
    }

    private fun updateEditor(change: (ItemEditor) -> ItemEditor) =
        _ui.update { state -> state.editor?.let { state.copy(editor = change(it)) } ?: state }

    private fun fieldOf(key: String?): WishField? = WishField.entries.firstOrNull { it.key == key }
}
