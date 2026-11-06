package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** One arm of a snowflake, drawn pointing up from the centre (12, 12) of the 24 by 24 viewport. */
private typealias Arm = PathBuilder.() -> Unit

/** A snowflake of six identical arms, every 60 degrees: perfect 6-fold symmetry. [dots] are drawn as round dots at those arm points. */
private fun snowflake(name: String, dots: List<Pair<Float, Float>> = emptyList(), arm: Arm): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 48.dp, defaultHeight = 48.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        repeat(6) { i ->
            group(rotate = i * 60f, pivotX = 12f, pivotY = 12f) {
                // black, because the flake is tinted where it is drawn
                path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = arm)
                if (dots.isNotEmpty()) {
                    // a round cap on a line of no length is a dot
                    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.8f, strokeLineCap = StrokeCap.Round) {
                        for ((x, y) in dots) { moveTo(x, y); lineTo(x, y + 0.01f) }
                    }
                }
            }
        }
    }.build()

/** A main arm with an inner and an outer V. */
private val Dendrite = snowflake("SnowflakeDendrite") {
    moveTo(12f, 12f); lineTo(12f, 1.8f)
    moveTo(12f, 8.8f); lineTo(9.6f, 6.4f)
    moveTo(12f, 8.8f); lineTo(14.4f, 6.4f)
    moveTo(12f, 5.4f); lineTo(10.4f, 3.8f)
    moveTo(12f, 5.4f); lineTo(13.6f, 3.8f)
}

/** A long main arm with a single wide V. */
private val Star = snowflake("SnowflakeStar") {
    moveTo(12f, 12f); lineTo(12f, 1.2f)
    moveTo(12f, 6.4f); lineTo(9f, 3.4f)
    moveTo(12f, 6.4f); lineTo(15f, 3.4f)
}

/** A hexagon with a spoke to each corner and a small V beyond it. */
private val Plate = snowflake("SnowflakePlate") {
    moveTo(12f, 12f); lineTo(12f, 2.6f)
    // the hexagon: one side from this arm's corner to the next arm's
    moveTo(12f, 6f); lineTo(17.2f, 9f)
    moveTo(12f, 4.4f); lineTo(10.6f, 3.2f)
    moveTo(12f, 4.4f); lineTo(13.4f, 3.2f)
}

/** A main arm with crossbars that shorten towards the tip, like a fern. */
private val Fern = snowflake("SnowflakeFern") {
    moveTo(12f, 12f); lineTo(12f, 1.6f)
    moveTo(9.4f, 9.2f); lineTo(14.6f, 9.2f)
    moveTo(10.2f, 6.4f); lineTo(13.8f, 6.4f)
    moveTo(11f, 3.8f); lineTo(13f, 3.8f)
}

/** A main arm with a V, and a dot at the tip and at the end of each branch. */
private val Dotted = snowflake("SnowflakeDotted", dots = listOf(12f to 2.6f, 9.2f to 6.2f, 14.8f to 6.2f)) {
    moveTo(12f, 12f); lineTo(12f, 4f)
    moveTo(12f, 8.6f); lineTo(9.8f, 6.4f)
    moveTo(12f, 8.6f); lineTo(14.2f, 6.4f)
}

/** The shapes of the snowflakes that make up the pattern on the cards. */
internal val SnowflakeShapes: List<ImageVector> = listOf(Dendrite, Star, Plate, Fern, Dotted)
