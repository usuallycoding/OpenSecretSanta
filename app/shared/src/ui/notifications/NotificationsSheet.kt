package com.usuallycoding.opensecretsanta.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SheetFrame
import com.usuallycoding.opensecretsanta.ui.load.loadMessage

/**
 * The sheet where the person chooses what they are notified about: a switch for this device (which asks the system's
 * permission and registers the device) and one for each of the four kinds. It loads again whenever the app comes back to
 * the front, so that a permission given or taken away in the system's settings shows at once.
 */
@Composable
fun NotificationsSheet(viewModel: NotificationsViewModel, onDismiss: () -> Unit) {
    val load by viewModel.load.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    SheetFrame(onDismiss) {
        Text(Strings.NOTIFICATIONS, style = MaterialTheme.typography.titleLarge)
        when {
            load.value == null && load.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            load.value == null -> {
                Text(load.error?.loadMessage() ?: Strings.SERVER_FAILED, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.refresh() }) { Text(Strings.TRY_AGAIN) }
            }
            else -> {
                reason(ui)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                SwitchRow(
                    title = Strings.NOTIFICATIONS_THIS_DEVICE,
                    note = Strings.NOTIFICATIONS_THIS_DEVICE_NOTE,
                    checked = ui.deviceOn,
                    enabled = ui.deviceSwitchEnabled,
                    onChange = { viewModel.setDevice(it) },
                )
                HorizontalDivider()
                SwitchRow(Strings.NOTIFICATIONS_ANNOUNCEMENTS, Strings.NOTIFICATIONS_ANNOUNCEMENTS_NOTE, ui.switches.announcements, ui.togglesEnabled) {
                    viewModel.setSwitch(NotificationCategory.ANNOUNCEMENTS, it)
                }
                SwitchRow(Strings.NOTIFICATIONS_WISHLIST, Strings.NOTIFICATIONS_WISHLIST_NOTE, ui.switches.wishlist, ui.togglesEnabled) {
                    viewModel.setSwitch(NotificationCategory.WISHLIST, it)
                }
                SwitchRow(Strings.NOTIFICATIONS_QUESTIONS, Strings.NOTIFICATIONS_QUESTIONS_NOTE, ui.switches.questions, ui.togglesEnabled) {
                    viewModel.setSwitch(NotificationCategory.QUESTIONS, it)
                }
                SwitchRow(Strings.NOTIFICATIONS_ANSWERS, Strings.NOTIFICATIONS_ANSWERS_NOTE, ui.switches.answers, ui.togglesEnabled) {
                    viewModel.setSwitch(NotificationCategory.ANSWERS, it)
                }
                ui.message?.let {
                    Text(it.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Why the device switch cannot be used, when it cannot. */
private fun reason(ui: NotificationsUiState): String? = when {
    !ui.serverAvailable -> Strings.NOTIFICATIONS_SERVER_OFF
    !ui.deviceSupported -> Strings.NOTIFICATIONS_UNSUPPORTED
    else -> null
}

private fun NotificationsMessage.text(): String = when (this) {
    NotificationsMessage.PermissionDenied -> Strings.NOTIFICATIONS_DENIED
    NotificationsMessage.DeviceFailed -> Strings.NOTIFICATIONS_DEVICE_FAILED
    NotificationsMessage.SaveFailed -> Strings.NOTIFICATIONS_SAVE_FAILED
}

@Composable
private fun SwitchRow(title: String, note: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val textColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
