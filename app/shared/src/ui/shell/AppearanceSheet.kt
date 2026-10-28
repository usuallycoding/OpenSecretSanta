package com.usuallycoding.opensecretsanta.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.data.settings.AppearanceSettings
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SheetFrame
import com.usuallycoding.opensecretsanta.ui.theme.ThemeMode
import com.usuallycoding.opensecretsanta.ui.theme.dynamicColorSupported

/**
 * The sheet where the person chooses how the app looks: follow the device, light or dark and, where the platform has
 * it (Android 12 and later), the wallpaper's colours. A choice applies at once; there is nothing to save.
 */
@Composable
fun AppearanceSheet(settings: AppearanceSettings, onDismiss: () -> Unit) {
    val appearance by settings.appearance.collectAsState()
    SheetFrame(onDismiss) {
        Text(Strings.APPEARANCE, style = MaterialTheme.typography.titleLarge)
        Column(Modifier.fillMaxWidth()) {
            ModeRow(Strings.THEME_FOLLOW_DEVICE, appearance.mode == ThemeMode.System) { settings.setMode(ThemeMode.System) }
            ModeRow(Strings.THEME_LIGHT, appearance.mode == ThemeMode.Light) { settings.setMode(ThemeMode.Light) }
            ModeRow(Strings.THEME_DARK, appearance.mode == ThemeMode.Dark) { settings.setMode(ThemeMode.Dark) }
        }
        if (dynamicColorSupported) {
            Row(
                Modifier.fillMaxWidth().toggleable(appearance.dynamicColor, role = Role.Switch, onValueChange = settings::setDynamicColor).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Strings.DYNAMIC_COLORS, style = MaterialTheme.typography.bodyLarge)
                    Text(Strings.DYNAMIC_COLORS_NOTE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = appearance.dynamicColor, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onSelect).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
