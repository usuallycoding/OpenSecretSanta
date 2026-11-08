package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.layout.LocalPageActions
import com.usuallycoding.opensecretsanta.ui.layout.PageAction
import com.usuallycoding.opensecretsanta.ui.layout.PageActionHost

/**
 * The heading of a column of a wide page or of a pane: its [title], then the buttons the page asked for in the place of its floating
 * button (as text buttons) and, when there is one, a [trailing] control such as a close icon.
 */
@Composable
internal fun ColumnHeading(title: String, actions: List<PageAction>, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        for (action in actions) {
            TextButton(onClick = action.onClick) {
                Icon(action.icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(action.description)
            }
        }
        trailing?.invoke()
    }
}

/**
 * A pane beside the group page: a heading with the page's [title], its own button, as the columns of a wide page have, and a close icon,
 * above the page, which scrolls and reloads on its own. The pane gives the page its own place for its action, so that the add, ask and
 * send buttons become buttons of the heading and no floating button is drawn.
 */
@Composable
internal fun PaneFrame(title: String, onClose: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val actions = remember { PageActionHost() }
    Column(modifier) {
        ColumnHeading(title, actions.actions) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = Strings.closePane(title)) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalPageActions provides actions) { content() }
        }
    }
}
