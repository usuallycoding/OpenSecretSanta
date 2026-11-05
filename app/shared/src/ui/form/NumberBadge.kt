package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** The side of a [NumberBadge], and the space between it and the text that follows it. */
internal val BadgeSize = 24.dp
internal val BadgeGap = 10.dp

/**
 * A number in a small filled circle, at the start of a question or a wishlist item. The number is part of what the text
 * is labelled with for screen readers, so the circle announces nothing of its own.
 */
@Composable
internal fun NumberBadge(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(BadgeSize).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
