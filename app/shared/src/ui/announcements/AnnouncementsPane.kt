package com.usuallycoding.opensecretsanta.ui.announcements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings

/**
 * The announcements in a pane beside the group page on a wide window: a heading with a way to close the pane above the list, which
 * scrolls and reloads on its own.
 */
@Composable
fun AnnouncementsPane(viewModel: AnnouncementsViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Strings.GROUP_NOTIFICATIONS, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = Strings.GROUP_NOTIFICATIONS_CLOSE) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { AnnouncementsPage(viewModel, onBack = onClose) }
    }
}
