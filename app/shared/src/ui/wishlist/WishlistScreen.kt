package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadContent
import com.usuallycoding.opensecretsanta.ui.load.ScrollableMessage

/** The person's own wishlist for one group: their items in their order, and while the group allows it, ways to change them. */
@Composable
fun WishlistScreen(viewModel: WishlistViewModel, onBack: () -> Unit) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    if (state.isGone()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Strings.GROUP_GONE, style = MaterialTheme.typography.titleMedium)
                Button(onClick = onBack) { Text(Strings.BACK) }
            }
        }
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            Toolbar(viewModel)
            ui.notice?.let { Notice(it, onDismiss = viewModel::dismissNotice) }
            LoadContent(state, onRetry = { viewModel.refresh() }, onRefresh = { viewModel.refresh() }) { data ->
                if (data.items.isEmpty()) {
                    ScrollableMessage(Strings.WISHLIST_EMPTY)
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(data.items, key = { _, item -> item.id }) { index, item ->
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

@Composable
private fun Toolbar(viewModel: WishlistViewModel) {
    val state by viewModel.data.collectAsState()
    val data = state.value
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            when {
                data == null -> Unit
                !data.editable -> Text(Strings.WISHLIST_CLOSED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                !viewModel.canAdd -> Text(Strings.wishlistFull(WishlistLimits.MAX_ITEMS), style = MaterialTheme.typography.bodyMedium)
                else -> Button(onClick = viewModel::openAdd) { Text(Strings.ADD_ITEM) }
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
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
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
    val rowModifier = Modifier.fillMaxWidth()
    val content: @Composable () -> Unit = {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemText(item, Modifier.weight(1f))
            if (editable) {
                Column {
                    IconButton(onClick = onUp, enabled = canMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = Strings.MOVE_UP) }
                    IconButton(onClick = onDown, enabled = canMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = Strings.MOVE_DOWN) }
                }
            }
        }
    }
    if (editable) Card(onClick = onEdit, modifier = rowModifier) { content() } else Card(modifier = rowModifier) { content() }
}

/** An item's title, note and link, as the person and their Santa both read it. A link opens in the platform's browser. */
@Composable
internal fun ItemText(title: String, note: String?, url: String?, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        url?.let { link ->
            Text(
                link,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(link) } },
            )
        }
    }
}

@Composable
private fun ItemText(item: WishItem, modifier: Modifier = Modifier) = ItemText(item.title, item.note, item.url, modifier)
