package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import com.usuallycoding.opensecretsanta.ui.form.LocalFieldsOverSnow
import com.usuallycoding.opensecretsanta.ui.form.PasswordField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadContent
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText

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
        Column(Modifier.fillMaxSize()) {
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
                when (notice) {
                    UserNotice.Unreachable -> Strings.SERVER_UNREACHABLE
                    UserNotice.HandOverRefused -> Strings.NOTICE_HANDOVER_REFUSED
                    UserNotice.Failed -> Strings.NOTICE_RESET_FAILED
                },
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
    if (ui.handingOver) HandOverDialog(viewModel, ui)
    ui.created?.let { PasswordsDialog(Strings.RESET_DONE_TITLE, Strings.RESET_DONE_BODY, listOf(it), viewModel::dismissCreated) }
}

/** The confirmation of a handover: what moves, and the administrator's own password, kept here and sent only when confirmed. */
@Composable
private fun HandOverDialog(viewModel: AdminUserViewModel, ui: AdminUserUi) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = viewModel::cancelHandOver,
        title = { Text(Strings.HANDOVER_TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Strings.handOverBody(viewModel.username))
                // a dialog is over the snow and opaque: its field keeps its usual look
                CompositionLocalProvider(LocalFieldsOverSnow provides false) {
                    PasswordField(Strings.HANDOVER_PASSWORD, password, { password = it }, ui.handOverError, ImeAction.Done) {
                        viewModel.confirmHandOver(password)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.confirmHandOver(password) }, enabled = password.isNotEmpty() && !ui.busy) {
                Text(Strings.HANDOVER_CONFIRM, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = viewModel::cancelHandOver) { Text(Strings.CANCEL) } },
    )
}

/** A user's username, their temporary password while it is temporary, and the reset: the Users tab and the group sheet show the same thing. */
@Composable
internal fun UserDetail(user: ManagedUser, viewModel: AdminUserViewModel, busy: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReadOnlyText(user.username, style = MaterialTheme.typography.headlineSmall, label = Strings.USERNAME)
        user.temporaryPassword?.let { password ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Strings.USER_TEMPORARY_PASSWORD, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ReadOnlyText(password, style = MaterialTheme.typography.titleMedium, label = Strings.USER_TEMPORARY_PASSWORD)
                Text(Strings.USER_MUST_CHANGE_NOTE, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (viewModel.canReset) {
            OutlinedButton(onClick = viewModel::askReset, enabled = !busy) { Text(Strings.RESET_PASSWORD) }
        }
        if (viewModel.canHandOver) {
            OutlinedButton(onClick = viewModel::askHandOver, enabled = !busy && !viewModel.handOverBlocked) { Text(Strings.HANDOVER_BUTTON) }
            if (viewModel.handOverBlocked) Text(Strings.handOverBlocked(user.username), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
