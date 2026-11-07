package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.form.CountBadged
import com.usuallycoding.opensecretsanta.ui.gift.countBadgeText

/**
 * An action of a group card: an outlined icon button with no text. Its [name] is the tooltip (on hover, and on a long press on a
 * touch screen) and, as [spoken], what a screen reader says, with a count when there is one; the icon and the badge say nothing of
 * their own. [count] is the text of a badge on the button's corner, or null for none, and [error] draws the button in the error
 * colour, outline and icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardIconButton(
    icon: ImageVector,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    count: String? = null,
    spoken: String = name,
    error: Boolean = false,
) {
    val danger = MaterialTheme.colorScheme.error
    CountBadged(count, modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(name) } },
            state = rememberTooltipState(),
        ) {
            OutlinedIconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = spoken },
                border = if (error) BorderStroke(1.dp, danger.copy(alpha = if (enabled) 1f else 0.12f)) else IconButtonDefaults.outlinedIconButtonBorder(enabled),
                colors = if (error) IconButtonDefaults.outlinedIconButtonColors(contentColor = danger) else IconButtonDefaults.outlinedIconButtonColors(),
            ) {
                Icon(icon, contentDescription = null)
            }
        }
    }
}

/** A [CardIconButton] for something that can be new to the Santa: a badge with [count] ("9+" above nine, none for none), and the count in its spoken name. */
@Composable
internal fun UnreadIconButton(
    icon: ImageVector,
    name: String,
    count: Int,
    spoken: (String, Int) -> String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    CardIconButton(icon, name, onClick, enabled = enabled, count = countBadgeText(count), spoken = if (count > 0) spoken(name, count) else name)
}
