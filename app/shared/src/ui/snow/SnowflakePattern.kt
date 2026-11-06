package com.usuallycoding.opensecretsanta.ui.snow

import kotlin.math.ceil
import kotlin.random.Random

/** One snowflake of a card's pattern. Sizes and distances are in dp from the card's top left; [shape] indexes the flake shapes. */
internal data class PatternFlake(
    val shape: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val angle: Float,
    val alpha: Float,
)

/** The scattered snowflakes on a card, apart from the card so that it can be tested: nothing here draws, and the same input gives the same flakes. */
internal object SnowflakePattern {
    const val SHAPES = 5
    const val MIN_SIZE = 14f
    const val MAX_SIZE = 64f

    /** About one flake for this many square dp. */
    const val CELL = 80f

    /** How far past the card's edge a flake's centre may be, so that some are cut off. */
    const val OVERHANG = 20f
    private const val MAX_ALPHA = 0.14f
    private const val MIN_ALPHA = 0.08f

    /**
     * The flakes of a card [width] by [height] dp: one at a random point of every cell of a grid laid over the card and a margin
     * around it, so that they are spread and not bunched, some on or past the edge. Small ones are the most common; the larger
     * a flake, the fainter.
     */
    fun flakes(seed: Int, width: Float, height: Float): List<PatternFlake> {
        if (width <= 0f || height <= 0f) return emptyList()
        val random = Random(seed)
        val spanX = width + 2 * OVERHANG
        val spanY = height + 2 * OVERHANG
        val columns = ceil(spanX / CELL).toInt().coerceAtLeast(1)
        val rows = ceil(spanY / CELL).toInt().coerceAtLeast(1)
        val cellWidth = spanX / columns
        val cellHeight = spanY / rows
        return buildList {
            for (row in 0 until rows) for (column in 0 until columns) {
                val grown = random.nextFloat()
                val size = MIN_SIZE + (MAX_SIZE - MIN_SIZE) * grown * grown
                add(
                    PatternFlake(
                        shape = random.nextInt(SHAPES),
                        x = -OVERHANG + (column + random.nextFloat()) * cellWidth,
                        y = -OVERHANG + (row + random.nextFloat()) * cellHeight,
                        size = size,
                        angle = random.nextFloat() * 360f,
                        alpha = MAX_ALPHA - (MAX_ALPHA - MIN_ALPHA) * (size - MIN_SIZE) / (MAX_SIZE - MIN_SIZE),
                    ),
                )
            }
        }
    }
}
