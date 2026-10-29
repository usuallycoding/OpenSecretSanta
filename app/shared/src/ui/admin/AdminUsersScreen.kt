package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.load.LoadContent

/** The users the administrator manages: everyone in their non-Draft groups, and themselves. */
@Composable
fun AdminUsersScreen(viewModel: AdminUsersViewModel, onOpen: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize()) {
            ui.notice?.let { notice ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (notice) {
                            UsersNotice.Unreachable -> Strings.SERVER_UNREACHABLE
                            UsersNotice.DeleteFailed -> Strings.NOTICE_DELETE_FAILED
                            UsersNotice.DeleteRefused -> Strings.NOTICE_DELETE_REFUSED
                        },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = viewModel::dismissNotice) { Text(Strings.OK) }
                }
            }
            LoadContent(state, onRetry = { viewModel.refresh() }, onRefresh = { viewModel.refresh() }) { users ->
                val shown = filterUsers(users, viewModel.search)
                Column(Modifier.fillMaxSize()) {
                    PeopleSearchField(viewModel.search, viewModel::setSearch, viewModel::clearSearch, Strings.ADMIN_SEARCH_USERS)
                    if (shown.isEmpty()) {
                        // it scrolls, so that pulling down still reloads
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Strings.USERS_NO_MATCH, style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(shown, key = { it.username }) { user ->
                                UserCard(user, onClick = { onOpen(user.username) }, onDelete = if (user.canDelete) ({ viewModel.askDelete(user.username) }) else null)
                            }
                        }
                    }
                }
            }
        }
    }
    ui.confirmingDelete?.let { username ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(Strings.USER_DELETE_TITLE) },
            text = { Text(Strings.deleteUserBody(username)) },
            confirmButton = { TextButton(onClick = { viewModel.confirmDelete() }) { Text(Strings.USER_DELETE, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(Strings.CANCEL) } },
        )
    }
}

/** One user; a user the server says can be deleted has a delete icon at the end of the row. */
@Composable
private fun UserCard(user: ManagedUser, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = if (onDelete != null) 4.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (user.mustChangePassword) {
                        Text(Strings.USER_MUST_CHANGE, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                user.fullName?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (user.canDelete) {
                    Text(Strings.USER_NOT_IN_GROUP, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = Strings.USER_DELETE, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
