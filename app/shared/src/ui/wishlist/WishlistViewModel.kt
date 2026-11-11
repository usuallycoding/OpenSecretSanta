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
        _ui.update { it.copy(editor = ItemEditor(item.id, item.title, item.note.orEmpty(), item.urls.ifEmpty { listOf("") }), notice = null) }
    }

    fun dismissEditor() = _ui.update { it.copy(editor = null) }

    fun edit(field: WishField, value: String) = updateEditor { editor ->
        val changed = when (field) {
            WishField.Title -> editor.copy(title = value)
            WishField.Note -> editor.copy(note = value)
        }
        changed.copy(errors = editor.errors - field, general = null)
    }

    fun editLink(index: Int, value: String) = updateEditor { editor ->
        if (index !in editor.urls.indices) return@updateEditor editor
        editor.copy(
            urls = editor.urls.toMutableList().also { it[index] = value },
            linkErrors = editor.linkErrors - index,
            linksError = null,
            general = null,
        )
    }

    /** Another empty link field after the last, up to [WishlistLimits.MAX_LINKS]. */
    fun addLink() = updateEditor { editor ->
        if (!editor.canAddLink) editor else editor.copy(urls = editor.urls + "", linksError = null)
    }

    /** Takes a link field away; the last one left is emptied instead, so the sheet always shows one. */
    fun removeLink(index: Int) = updateEditor { editor ->
        if (index !in editor.urls.indices) return@updateEditor editor
        val urls = editor.urls.filterIndexed { i, _ -> i != index }.ifEmpty { listOf("") }
        // errors belong to the fields they were shown next to, which move up after the one taken away
        val errors = editor.linkErrors.filterKeys { it != index }.mapKeys { (i, _) -> if (i > index) i - 1 else i }
        editor.copy(urls = urls, linkErrors = errors, linksError = null, general = null)
    }

    /** Checks what the server checks, then sends; returns the running work, or null when nothing is sent. */
    fun save(): Job? {
        val editor = _ui.value.editor ?: return null
        if (editor.saving) return null
        val kept = keptLinks(editor.urls)
        val entry = try {
            WishlistEntry.of(editor.title, editor.note, editor.urls)
        } catch (e: DomainException.ValidationFailed) {
            if (linkPlace(e.field) != null) showInvalidLinks(editor.urls, kept) else showInvalid(e.field, e.message.orEmpty(), kept)
            return null
        }
        updateEditor { it.copy(saving = true, errors = emptyMap(), linkErrors = emptyMap(), linksError = null, general = null) }
        val request = WishlistItemRequest(entry.title, entry.note, urls = entry.urls)
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
                is ApiResult.Failure -> failed(result.error, itemRelated = editor.itemId != null, fromEditor = true, kept = kept)
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
                is ApiResult.Failure -> failed(result.error, itemRelated = true, fromEditor = false, kept = emptyList())
            }
        }
    }

    // --- reordering ---------------------------------------------------------------------------

    fun moveUp(itemId: Long): Job? = moveBy(itemId, -1)

    fun moveDown(itemId: Long): Job? = moveBy(itemId, +1)

    private fun moveBy(itemId: Long, by: Int): Job? {
        val from = current?.items?.indexOfFirst { it.id == itemId } ?: return null
        return moveTo(itemId, from + by)
    }

    /** Moves the item to [index] and sends every id in the new order; the order the server answers with is the one shown. */
    fun moveTo(itemId: Long, index: Int): Job? {
        val data = current?.takeIf { it.editable } ?: return null
        if (_ui.value.moving) return null
        val from = data.items.indexOfFirst { it.id == itemId }
        if (from < 0 || index !in data.items.indices || index == from) return null
        val ids = data.items.map { it.id }.toMutableList()
        ids.add(index, ids.removeAt(from))
        _ui.update { it.copy(moving = true, notice = null) }
        return scope.launch {
            when (val result = api.reorderWishlist(groupId, ids)) {
                is ApiResult.Success -> {
                    loader.replace(data.copy(items = result.value.map { it.toItem() }))
                    _ui.update { it.copy(moving = false) }
                }
                is ApiResult.Failure -> {
                    _ui.update { it.copy(moving = false) }
                    failed(result.error, itemRelated = false, fromEditor = false, kept = emptyList())
                }
            }
        }
    }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    // --- when the server says no ----------------------------------------------------------------

    private suspend fun failed(error: ApiError, itemRelated: Boolean, fromEditor: Boolean, kept: List<Int>) {
        when {
            error is ApiError.Validation && fromEditor && isFormField(error.field) -> {
                updateEditor { it.copy(saving = false) }
                showInvalid(error.field, error.message, kept)
            }
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

    /** `title`, `note`, `urls` or `urls[i]`: what the form can show next to a field. */
    private fun isFormField(key: String?): Boolean = fieldOf(key) != null || key == LINKS_FIELD || linkPlace(key) != null

    private fun linkPlace(key: String?): Int? =
        key?.removePrefix("$LINKS_FIELD[")?.removeSuffix("]")?.takeIf { key.startsWith("$LINKS_FIELD[") && key.endsWith("]") }?.toIntOrNull()

    /** Shows a problem next to its field: a link by the place it has among the [kept] fields, those with something in them. */
    private fun showInvalid(key: String?, message: String, kept: List<Int>) = updateEditor { editor ->
        val field = fieldOf(key)
        val place = linkPlace(key)?.let { kept.getOrNull(it) }
        when {
            field != null -> editor.copy(errors = mapOf(field to message), linkErrors = emptyMap(), linksError = null)
            place != null -> editor.copy(errors = emptyMap(), linkErrors = mapOf(place to message), linksError = null)
            key == LINKS_FIELD -> editor.copy(errors = emptyMap(), linkErrors = emptyMap(), linksError = message)
            else -> editor.copy(errors = emptyMap(), linkErrors = emptyMap(), linksError = null, general = message)
        }
    }

    /** Every link that is not acceptable gets its message next to its own field, not just the first. */
    private fun showInvalidLinks(urls: List<String>, kept: List<Int>) {
        val problems = kept.mapNotNull { place ->
            try {
                WishlistEntry.of("-", null, listOf(urls[place]))
                null
            } catch (e: DomainException.ValidationFailed) {
                place to e.message.orEmpty()
            }
        }.toMap()
        updateEditor { it.copy(errors = emptyMap(), linkErrors = problems, linksError = null) }
    }

    private companion object {
        const val LINKS_FIELD = "urls"
    }
}

/** The places, among the link fields of the sheet, of the links that are sent: not blank and not a repeat, in order. */
internal fun keptLinks(urls: List<String>): List<Int> {
    val seen = mutableSetOf<String>()
    return urls.indices.filter { index -> urls[index].trim().let { it.isNotEmpty() && seen.add(it) } }
}
