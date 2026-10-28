package com.usuallycoding.opensecretsanta.ui.load

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.getPlatform

/** The least height the [HeaderWithFooter] footer keeps, so that a long header never squeezes it away. */
private val MIN_FOOTER_HEIGHT = 160.dp

/**
 * A page with [header] on top (details, buttons) and a [footer] that fills all the height that is left, for a list of
 * people that scrolls inside itself. The header scrolls when it needs more than the height minus the footer's minimum. The
 * whole page can be pulled down (on the platforms with the gesture) and reloads through [onRefresh] while [loading].
 */
@Composable
fun HeaderWithFooter(
    loading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable (Boolean) -> Unit = { pulled ->
        CompositionLocalProvider(LocalPullActive provides pulled) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val headerMax = (maxHeight - MIN_FOOTER_HEIGHT - PAGE_PADDING * 2).coerceAtLeast(0.dp)
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = PAGE_PADDING)) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = headerMax).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = header,
                    )
                    Column(Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = footer)
                }
            }
        }
    }
    if (getPlatform().hasPullToRefresh) PullRefresh(loading, onRefresh, modifier.fillMaxSize(), body) else body(false)
}

private val PAGE_PADDING = 8.dp

/** A list that scrolls inside itself and takes all the height the rest of the footer leaves, with its content at the top. */
@Composable
fun ColumnScope.ScrollingBox(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().weight(1f)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}
