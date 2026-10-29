package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.MultiLineTextField
import com.usuallycoding.opensecretsanta.ui.form.SheetFrame
import com.usuallycoding.opensecretsanta.ui.form.SheetTextField
import com.usuallycoding.opensecretsanta.ui.load.loadMessage

/**
 * The sheet where the owner writes an announcement for a Pending or Active group, chooses how to send it, confirms and
 * reads how it went. A way the server has not been set up for is shown greyed out with the reason.
 */
@Composable
fun AnnouncementSheet(viewModel: AnnouncementViewModel, onDismiss: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    val load by viewModel.load.collectAsState()

    SheetFrame(onDismiss) {
        Text(Strings.ANNOUNCE_BUTTON, style = MaterialTheme.typography.titleLarge)
        val result = ui.result
        when {
            result != null -> {
                result.emails?.let { Text(Strings.emailSummary(it.sent, it.failed, it.noAddress), color = if (it.failed > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
                result.push?.let { Text(Strings.announcePushSummary(it.reached, it.notReached), color = MaterialTheme.colorScheme.primary) }
                Button(onClick = onDismiss) { Text(Strings.ANNOUNCE_CLOSE) }
            }
            !ui.ready && load.error == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !ui.ready -> {
                Text(load.error?.loadMessage().orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.refresh() }) { Text(Strings.TRY_AGAIN) }
            }
            else -> Form(ui, viewModel)
        }
    }
    if (ui.confirming) {
        AlertDialog(
            onDismissRequest = viewModel::cancelConfirm,
            title = { Text(Strings.ANNOUNCE_CONFIRM_TITLE) },
            text = { Text(Strings.announceConfirm(viewModel.groupName, ui.email, ui.push)) },
            confirmButton = { Button(onClick = viewModel::send) { Text(Strings.ANNOUNCE_SEND) } },
            dismissButton = { TextButton(onClick = viewModel::cancelConfirm) { Text(Strings.ANNOUNCE_CANCEL) } },
        )
    }
}

@Composable
private fun Form(ui: AnnouncementUi, viewModel: AnnouncementViewModel) {
    val enabled = !ui.sending
    SheetTextField(
        value = viewModel.title,
        onValueChange = viewModel::setTitle,
        label = Strings.ANNOUNCE_TITLE_LABEL,
        modifier = Modifier.fillMaxWidth(),
        hint = Strings.announceCount(viewModel.title.length, AnnouncementUi.MAX_TITLE),
    )
    if (viewModel.title.isNotBlank()) {
        Text(viewModel.subject(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    MultiLineTextField(
        value = viewModel.body,
        onValueChange = viewModel::setBody,
        label = Strings.ANNOUNCE_MESSAGE_LABEL,
        modifier = Modifier.fillMaxWidth(),
        hint = Strings.announceCount(viewModel.body.length, AnnouncementUi.MAX_BODY),
    )
    Text(Strings.ANNOUNCE_SEND_AS, style = MaterialTheme.typography.titleMedium)
    Channel(Strings.ANNOUNCE_EMAIL, ui.email, ui.emailAvailable && enabled, Strings.ANNOUNCE_EMAIL_OFF.takeIf { !ui.emailAvailable }, viewModel::setEmail)
    Channel(Strings.ANNOUNCE_PUSH, ui.push, ui.pushAvailable && enabled, Strings.ANNOUNCE_PUSH_OFF.takeIf { !ui.pushAvailable }, viewModel::setPush)
    if (viewModel.pushCutsMessage) {
        Text(Strings.ANNOUNCE_PUSH_CUT, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = viewModel::ask, enabled = viewModel.canSend) { Text(Strings.ANNOUNCE_SEND) }
        if (ui.sending) CircularProgressIndicator(Modifier.padding(start = 4.dp))
    }
}

/** One way of sending: a checkbox with its name, and under it the reason when the server cannot do it. */
@Composable
private fun Channel(name: String, checked: Boolean, enabled: Boolean, reason: String?, onChange: (Boolean) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
            Text(name, Modifier.padding(start = 8.dp), color = if (enabled || reason == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        reason?.let { Text(it, Modifier.padding(start = 40.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
