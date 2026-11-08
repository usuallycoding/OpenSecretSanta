package com.usuallycoding.opensecretsanta.ui.announcements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.PaneFrame

/**
 * The announcements in a pane beside the group page on a wide window: the pane's heading with a way to close it above the list, which
 * scrolls and reloads on its own.
 */
@Composable
fun AnnouncementsPane(viewModel: AnnouncementsViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    PaneFrame(Strings.ANNOUNCEMENTS, onClose, modifier) { AnnouncementsPage(viewModel, onBack = onClose) }
}
