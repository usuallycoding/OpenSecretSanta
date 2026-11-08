package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.gift.countBadgeText

/**
 * A small bell followed by how many announcements the person has not seen, in the theme's tertiary colour: the number, "9+" above nine,
 * and nothing at all for none. A screen reader hears the real number, "3 new announcements", and nothing of the icon.
 */
@Composable
internal fun UnreadAnnouncementsCount(count: Int, modifier: Modifier = Modifier) {
    val text = countBadgeText(count) ?: return
    val colour = MaterialTheme.colorScheme.tertiary
    Row(
        modifier.clearAndSetSemantics { contentDescription = Strings.newAnnouncements(count) },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = colour)
        Text(text, style = MaterialTheme.typography.labelLarge, color = colour)
    }
}
