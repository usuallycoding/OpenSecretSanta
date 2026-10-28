package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadContent

/** One managed user: their profile, their temporary password while it is still temporary, and a way to reset it. */
@Composable
fun AdminUserScreen(viewModel: AdminUserViewModel, onBack: () -> Unit) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    if (state.isGone()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Strings.USER_GONE, style = MaterialTheme.typography.titleMedium)
                Button(onClick = onBack) { Text(Strings.BACK) }
            }
        }
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            UserNoticeRow(ui, viewModel)
            LoadContent(state, onRetry = { viewModel.refresh() }, onRefresh = { viewModel.refresh() }) { user ->
                // the detail does not scroll by itself: the sheet that shows it scrolls as a whole
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { UserDetail(user, viewModel, ui.busy) }
            }
        }
    }
    UserDialogs(viewModel, ui)
}

/** The failure a reset can leave behind, said once with a way to dismiss it. */
@Composable
internal fun UserNoticeRow(ui: AdminUserUi, viewModel: AdminUserViewModel) {
    ui.notice?.let { notice ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (notice == UserNotice.Unreachable) Strings.SERVER_UNREACHABLE else Strings.NOTICE_RESET_FAILED,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = viewModel::dismissNotice) { Text(Strings.OK) }
        }
    }
}

/** The confirmation before a reset and the dialog that shows the new temporary password once. */
@Composable
internal fun UserDialogs(viewModel: AdminUserViewModel, ui: AdminUserUi) {
    if (ui.confirming) {
        AlertDialog(
            onDismissRequest = viewModel::cancelReset,
            title = { Text(Strings.RESET_TITLE) },
            text = { Text(Strings.resetBody(viewModel.username)) },
            confirmButton = { TextButton(onClick = { viewModel.confirmReset() }) { Text(Strings.RESET_PASSWORD) } },
            dismissButton = { TextButton(onClick = viewModel::cancelReset) { Text(Strings.CANCEL) } },
        )
    }
    ui.created?.let { PasswordsDialog(Strings.RESET_DONE_TITLE, Strings.RESET_DONE_BODY, listOf(it), viewModel::dismissCreated) }
}

/** A user's profile, their temporary password while it is temporary, and the reset: the Users tab and the group sheet show the same thing. */
@Composable
internal fun UserDetail(user: ManagedUser, viewModel: AdminUserViewModel, busy: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(user.username, style = MaterialTheme.typography.headlineSmall)
        if (listOf(user.fullName, user.email, user.phone, user.address, user.deliveryInfo).any { it != null }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(Strings.USER_FIELD_NAME, user.fullName)
                    Field(Strings.USER_FIELD_EMAIL, user.email)
                    Field(Strings.RECEIVER_PHONE, user.phone)
                    Field(Strings.RECEIVER_ADDRESS, user.address)
                    Field(Strings.RECEIVER_DELIVERY, user.deliveryInfo)
                }
            }
        } else {
            Text(Strings.USER_NO_PROFILE, style = MaterialTheme.typography.bodyMedium)
        }
        user.temporaryPassword?.let { password ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Strings.USER_TEMPORARY_PASSWORD, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer { Text(password, style = MaterialTheme.typography.titleMedium) }
                Text(Strings.USER_MUST_CHANGE_NOTE, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (viewModel.canReset) {
            OutlinedButton(onClick = viewModel::askReset, enabled = !busy) { Text(Strings.RESET_PASSWORD) }
        }
    }
}

@Composable
private fun Field(label: String, value: String?) {
    if (value == null) return
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
