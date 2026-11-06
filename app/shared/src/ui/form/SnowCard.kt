package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.usuallycoding.opensecretsanta.ui.snow.SnowflakePattern

/**
 * A [Card] with a faint pattern of snowflakes behind its content. The pattern is one draw pass behind the content: it adds no
 * layout, says nothing to a screen reader and takes no input, so the card is the size it would be without it, and the card's
 * shape cuts off the flakes that cross its edge. The same [seed] gives the same pattern, so give a card something that tells it
 * from the others (an id) and that does not change while it is shown.
 */
@Composable
internal fun SnowCard(
    seed: Any,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable ColumnScope.() -> Unit,
) {
    // the card's own column gives its children no minimum width, so this one is as wide as the card
    val inner = Modifier.fillMaxWidth().snowflakePattern(seed)
    if (onClick != null) Card(onClick = onClick, modifier = modifier, elevation = elevation) { Column(inner, content = content) }
    else Card(modifier, elevation = elevation) { Column(inner, content = content) }
}

@Composable
private fun Modifier.snowflakePattern(seed: Any): Modifier {
    val painters = SnowflakeShapes.map { rememberVectorPainter(it) }
    val color = MaterialTheme.colorScheme.secondary
    val seedHash = remember(seed) { seed.hashCode() }
    return clipToBounds().drawWithCache {
        val dp = density
        val flakes = SnowflakePattern.flakes(seedHash, size.width / dp, size.height / dp)
        val tint = ColorFilter.tint(color)
        onDrawBehind {
            for (flake in flakes) {
                val side = flake.size * dp
                val centre = Offset(flake.x * dp, flake.y * dp)
                translate(centre.x - side / 2, centre.y - side / 2) {
                    rotate(flake.angle, pivot = Offset(side / 2, side / 2)) {
                        with(painters[flake.shape]) { draw(Size(side, side), alpha = flake.alpha, colorFilter = tint) }
                    }
                }
            }
        }
    }
}
