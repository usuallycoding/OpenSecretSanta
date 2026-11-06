package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import com.usuallycoding.opensecretsanta.ui.layout.PageActionHost
import com.usuallycoding.opensecretsanta.ui.layout.LocalPageActions
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** The narrowest a column of a wide page gets; with less room than that for all of them the row scrolls sideways. */
private val MIN_COLUMN_WIDTH = 300.dp

/**
 * The sections of a Giving or Receiving page side by side, each under its own [titles] heading, which ends in the section's own button
 * when it has one, with its own scrolling and reloading in what [column] draws. They share the width evenly, and keep [MIN_COLUMN_WIDTH] by scrolling the row when there is not enough.
 */
@Composable
internal fun WideColumns(titles: List<String>, column: @Composable (index: Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fits = maxWidth >= MIN_COLUMN_WIDTH * titles.size
        val row = if (fits) Modifier.fillMaxSize() else Modifier.fillMaxSize().horizontalScroll(rememberScrollState())
        Row(row) {
            for ((index, title) in titles.withIndex()) {
                if (index > 0) VerticalDivider()
                // the column's floating button is a button at the end of its heading instead
                val actions = remember { PageActionHost() }
                Column((if (fits) Modifier.weight(1f) else Modifier.width(MIN_COLUMN_WIDTH)).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        for (action in actions.actions) {
                            TextButton(onClick = action.onClick) {
                                Icon(action.icon, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(action.description)
                            }
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        CompositionLocalProvider(LocalPageActions provides actions) { column(index) }
                    }
                }
            }
        }
    }
}
