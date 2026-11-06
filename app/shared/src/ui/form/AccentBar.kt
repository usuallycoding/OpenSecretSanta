package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val AccentBarWidth = 4.dp

/**
 * When [visible], a bar in the theme's tertiary colour along the start edge of what it is put on (the right edge when the layout
 * runs right to left), drawn behind the content. The card's shape rounds it and it takes no room, so nothing moves when it comes
 * or goes. It is only colour: what it marks always says so in words as well.
 */
@Composable
internal fun Modifier.startAccentBar(visible: Boolean): Modifier {
    if (!visible) return this
    val color = MaterialTheme.colorScheme.tertiary
    return drawBehind {
        val width = AccentBarWidth.toPx()
        val left = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - width
        drawRect(color, topLeft = Offset(left, 0f), size = Size(width, size.height))
    }
}
