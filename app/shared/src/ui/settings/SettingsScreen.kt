package com.usuallycoding.opensecretsanta.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.data.push.PushRegistration
import com.usuallycoding.opensecretsanta.data.settings.AppearanceSettings
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SheetBackHandler
import com.usuallycoding.opensecretsanta.ui.notifications.NotificationsSheet
import com.usuallycoding.opensecretsanta.ui.notifications.NotificationsViewModel
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordSheet
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordViewModel
import com.usuallycoding.opensecretsanta.ui.shell.AppearanceSheet
import com.usuallycoding.opensecretsanta.ui.theme.ThemeMode
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The settings page, shared by the participant and the admin view: appearance (showing the choice in force), notifications
 * (only where the platform has them), changing the password, and, set apart below them, signing out. The three first rows
 * open the sheets the top-bar menu used to open, over this page; closing a sheet leaves the page as it was.
 */
@Composable
fun SettingsScreen(onSignOut: () -> Unit) {
    val appearanceSettings = koinInject<AppearanceSettings>()
    val appearance by appearanceSettings.appearance.collectAsState()
    val push = koinInject<PushRegistration>()
    var choosingAppearance by remember { mutableStateOf(false) }
    var choosingNotifications by remember { mutableStateOf(false) }
    var changingPassword by remember { mutableStateOf(false) }
    // Back closes the sheet before it leaves the screen, on the platforms whose back is not already the sheet's own
    SheetBackHandler(enabled = choosingAppearance) { choosingAppearance = false }
    SheetBackHandler(enabled = choosingNotifications) { choosingNotifications = false }
    SheetBackHandler(enabled = changingPassword) { changingPassword = false }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsRow(Strings.APPEARANCE, supporting = appearance.mode.label()) { choosingAppearance = true }
        if (push.isOffered) SettingsRow(Strings.NOTIFICATIONS) { choosingNotifications = true }
        SettingsRow(Strings.CHANGE_PASSWORD) { changingPassword = true }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SettingsRow(Strings.SIGN_OUT, chevron = false, onClick = onSignOut)
    }

    if (choosingAppearance) AppearanceSheet(appearanceSettings, onDismiss = { choosingAppearance = false })
    if (choosingNotifications) {
        NotificationsSheet(koinViewModel<NotificationsViewModel>(key = "notifications-sheet"), onDismiss = { choosingNotifications = false })
    }
    if (changingPassword) {
        ChangePasswordSheet(koinViewModel<ChangePasswordViewModel>(key = "change-password-sheet"), onDismiss = { changingPassword = false })
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.System -> Strings.THEME_FOLLOW_DEVICE
    ThemeMode.Light -> Strings.THEME_LIGHT
    ThemeMode.Dark -> Strings.THEME_DARK
}

/** One full-width row that is pressed as a whole, at least 56dp high; [supporting] says what is in force, [chevron] that more opens. */
@Composable
private fun SettingsRow(title: String, supporting: String? = null, chevron: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            supporting?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
