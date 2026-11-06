package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * [content] with a count on its top end corner, in the theme's tertiary colour, when [text] is not null. The badge says nothing to a
 * screen reader: what is badged carries the count in its own description.
 */
@Composable
internal fun CountBadged(text: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (text == null) {
        Box(modifier) { content() }
        return
    }
    BadgedBox(
        badge = {
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.clearAndSetSemantics { },
            ) { Text(text) }
        },
        modifier = modifier,
    ) { content() }
}

/** A small pill saying that a question has not been answered: text on the tertiary container, so that colour is not the only sign. */
@Composable
internal fun UnansweredPill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}
