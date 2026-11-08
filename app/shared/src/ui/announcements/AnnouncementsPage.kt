package com.usuallycoding.opensecretsanta.ui.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.form.TimesLine
import com.usuallycoding.opensecretsanta.ui.groups.GroupGone
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LazyPullPage
import com.usuallycoding.opensecretsanta.ui.load.loadItems

/**
 * The announcements the group's owner has sent, last one sent first, as a page of their own or in a pane beside the group's page.
 * There is nothing here that edits or deletes one; the owner reads the same list.
 */
@Composable
fun AnnouncementsPage(viewModel: AnnouncementsViewModel, onBack: () -> Unit) {
    val state by viewModel.data.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    if (state.isGone()) {
        GroupGone(onBack)
        return
    }
    LazyPullPage(loading = state.loading, onRefresh = { viewModel.refresh() }) {
        loadItems(state, onRetry = { viewModel.refresh() }) { announcements ->
            if (announcements.isEmpty()) item(key = "none") { Text(Strings.NO_GROUP_NOTIFICATIONS, style = MaterialTheme.typography.bodyLarge) }
            items(announcements, key = { it.number }) { announcement -> AnnouncementCard(announcement) }
        }
    }
}

/** An announcement in a card: its title, its message, which is cut when long and opens in full, and when it was sent. */
@Composable
internal fun AnnouncementCard(announcement: AnnouncementItem, modifier: Modifier = Modifier) {
    SnowCard("announcement-${announcement.number}", modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ReadOnlyText(announcement.title, style = MaterialTheme.typography.titleMedium, label = Strings.GROUP_NOTIFICATION_TITLE_LABEL, maxLines = 2, copy = false)
            ReadOnlyText(announcement.text, style = MaterialTheme.typography.bodyMedium, label = Strings.GROUP_NOTIFICATION_TEXT_LABEL)
            TimesLine(Strings.groupNotificationSentAt(announcement.sent))
        }
    }
}
