package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * When something was made or changed, in one small muted line: the parts that are there, joined with " · "
 * ("Added 29 Oct 2026, 14:05 · Changed 30 Oct 2026, 09:12"). It only shows; it is not a button and takes no taps.
 */
@Composable
fun TimesLine(vararg parts: String?, modifier: Modifier = Modifier) {
    val shown = parts.filterNotNull()
    if (shown.isEmpty()) return
    Text(
        shown.joinToString(" · "),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
