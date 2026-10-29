package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.usuallycoding.opensecretsanta.ui.form.CopyIcon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.GroupSearchBar
import com.usuallycoding.opensecretsanta.ui.groups.NoMatchingGroups
import com.usuallycoding.opensecretsanta.ui.groups.shown
import com.usuallycoding.opensecretsanta.ui.load.LoadContent
import com.usuallycoding.opensecretsanta.ui.load.ScrollableMessage
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.usuallycoding.opensecretsanta.ui.form.AppFab
import com.usuallycoding.opensecretsanta.ui.form.FAB_CLEARANCE

/** The administrator's own groups, with a way to create one. */
@Composable
fun AdminGroupsScreen(viewModel: AdminGroupsViewModel, onOpen: (Long) -> Unit, onNew: () -> Unit, onCreated: (Long) -> Unit) {
    val state by viewModel.state.collectAsState()
    val duplicate by viewModel.duplicate.collectAsState()
    val created by viewModel.created.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    // a copy was made: open it, once
    LaunchedEffect(created) {
        created?.let {
            viewModel.createdHandled()
            onCreated(it)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize()) {
            LoadContent(state, onRetry = { viewModel.refresh() }, onRefresh = { viewModel.refresh() }) { groups ->
                if (groups.isEmpty()) {
                    ScrollableMessage(Strings.ADMIN_NO_GROUPS)
                } else {
                    val shown = viewModel.filter.shown(groups)
                    // the search and the chips stay where they are while the list under them scrolls
                    Column(Modifier.fillMaxSize()) {
                        GroupSearchBar(viewModel.filter)
                        if (shown.isEmpty()) {
                            NoMatchingGroups(onClear = viewModel.filter::clear)
                        } else {
                            LazyColumn(
                                Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp + FAB_CLEARANCE),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(shown, key = { it.id }) { group -> GroupCard(group, onDuplicate = { viewModel.askDuplicate(group) }) { onOpen(group.id) } }
                            }
                        }
                    }
                }
            }
        }
        AppFab(Icons.Filled.Add, Strings.NEW_GROUP, onClick = onNew)
    }
    duplicate?.let { DuplicateDialog(it, viewModel) }
}

@Composable
private fun DuplicateDialog(ui: DuplicateUi, viewModel: AdminGroupsViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cancelDuplicate,
        title = { Text(Strings.DUPLICATE_TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Strings.duplicateBody(ui.copyName))
                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.confirmDuplicate() }, enabled = !ui.busy) {
                Text(if (ui.busy) Strings.DUPLICATING else Strings.DUPLICATE)
            }
        },
        dismissButton = { TextButton(onClick = viewModel::cancelDuplicate, enabled = !ui.busy) { Text(Strings.CANCEL) } },
    )
}

@Composable
private fun GroupCard(group: AdminGroup, onDuplicate: () -> Unit, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(group.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(group.state.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            group.whenText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            group.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Strings.participantCount(group.participants.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                    Icon(CopyIcon, contentDescription = Strings.DUPLICATE, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
