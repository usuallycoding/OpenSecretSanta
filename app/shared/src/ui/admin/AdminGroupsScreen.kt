package com.usuallycoding.opensecretsanta.ui.admin

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
import com.usuallycoding.opensecretsanta.ui.load.LoadContent

/** The administrator's own groups, with a way to create one. */
@Composable
fun AdminGroupsScreen(viewModel: AdminGroupsViewModel, onOpen: (Long) -> Unit, onNew: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onNew) { Text(Strings.NEW_GROUP) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { viewModel.refresh() }) { Text(Strings.REFRESH) }
                }
            }
            LoadContent(state, onRetry = { viewModel.refresh() }) { groups ->
                if (groups.isEmpty()) {
                    Text(Strings.ADMIN_NO_GROUPS, modifier = Modifier.fillMaxWidth().padding(24.dp), style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(groups, key = { it.id }) { group -> GroupCard(group) { onOpen(group.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: AdminGroup, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(group.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(group.state.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            group.whenText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            group.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                Strings.participantCount(group.participants.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
