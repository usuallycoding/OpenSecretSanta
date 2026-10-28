package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.form.LinkText
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.filled.Add
import com.usuallycoding.opensecretsanta.ui.form.AppFab

/** The person's own wishlist for one group, as a section of the Receiver page: their items in their order, and while the group allows it, ways to change them. */
@Composable
fun WishlistSection(viewModel: WishlistViewModel) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Toolbar(viewModel)
        ui.notice?.let { Notice(it, onDismiss = viewModel::dismissNotice) }
        LoadSection(state, onRetry = { viewModel.refresh() }) { data ->
            if (data.items.isEmpty()) {
                Text(Strings.WISHLIST_EMPTY, style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.items.forEachIndexed { index, item ->
                        key(item.id) {
                            ItemRow(
                                item = item,
                                editable = data.editable,
                                canMoveUp = index > 0 && !ui.moving,
                                canMoveDown = index < data.items.lastIndex && !ui.moving,
                                onEdit = { viewModel.openEdit(item.id) },
                                onUp = { viewModel.moveUp(item.id) },
                                onDown = { viewModel.moveDown(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
    ItemSheetHost(viewModel)
}

/** The floating "Add item" button of the wishlist: there only while the list can be changed and has room. */
@Composable
fun BoxScope.WishlistFab(viewModel: WishlistViewModel) {
    val state by viewModel.data.collectAsState()
    val data = state.value
    if (data != null && data.editable && viewModel.canAdd) {
        AppFab(Icons.Filled.Add, Strings.ADD_ITEM, onClick = viewModel::openAdd)
    }
}

@Composable
private fun Toolbar(viewModel: WishlistViewModel) {
    val state by viewModel.data.collectAsState()
    val data = state.value
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            when {
                data == null -> Unit
                !data.editable -> Text(Strings.WISHLIST_CLOSED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                !viewModel.canAdd -> Text(Strings.wishlistFull(WishlistLimits.MAX_ITEMS), style = MaterialTheme.typography.bodyMedium)
                else -> Unit
            }
        }
    }
}

@Composable
private fun Notice(notice: WishlistNotice, onDismiss: () -> Unit) {
    val text = when (notice) {
        WishlistNotice.ChangedElsewhere -> Strings.NOTICE_CHANGED_ELSEWHERE
        WishlistNotice.ItemGone -> Strings.NOTICE_ITEM_GONE
        WishlistNotice.Locked -> Strings.NOTICE_LOCKED
        WishlistNotice.ListFull -> Strings.NOTICE_LIST_FULL
        WishlistNotice.Unreachable -> Strings.SERVER_UNREACHABLE
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onDismiss) { Text(Strings.OK) }
    }
}

@Composable
private fun ItemRow(
    item: WishItem,
    editable: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemText(item, Modifier.weight(1f))
            if (editable) {
                Column {
                    IconButton(onClick = onEdit, modifier = Modifier.size(ITEM_BUTTON)) { Icon(Icons.Filled.Edit, contentDescription = Strings.EDIT_ITEM) }
                    IconButton(onClick = onUp, enabled = canMoveUp, modifier = Modifier.size(ITEM_BUTTON)) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = Strings.MOVE_UP) }
                    IconButton(onClick = onDown, enabled = canMoveDown, modifier = Modifier.size(ITEM_BUTTON)) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = Strings.MOVE_DOWN) }
                }
            }
        }
    }
}
/** The side of the edit and move buttons: three of them make a column that keeps the cards compact. */
private val ITEM_BUTTON = 40.dp


/** An item's title, note and link, as the person and their Santa both read it. A link opens a dialog first. */
@Composable
internal fun ItemText(title: String, note: String?, url: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ReadOnlyText(title, style = MaterialTheme.typography.titleMedium, label = Strings.FIELD_TITLE, maxLines = 1)
        note?.let { ReadOnlyText(it, style = MaterialTheme.typography.bodyMedium, label = Strings.NOTE_LABEL) }
        url?.let { LinkText(it) }
    }
}

@Composable
private fun ItemText(item: WishItem, modifier: Modifier = Modifier) = ItemText(item.title, item.note, item.url, modifier)
