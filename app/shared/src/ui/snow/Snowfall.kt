package com.usuallycoding.opensecretsanta.ui.snow

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * One snowflake. Sizes and distances are in dp. A flake falls at [speed] and sways sideways around its column [x] by up to
 * [sway], at [swaySpeed] radians a second from [phase]. Larger flakes are closer, so they are faster and less faint.
 */
internal data class Flake(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val sway: Float,
    val swaySpeed: Float,
    val phase: Float,
    val alpha: Float,
) {
    /** Where the flake is drawn across: its column swayed sideways, never outside the window. */
    fun drawnX(width: Float): Float = (x + sway * sin(phase)).coerceIn(0f, width)
}

/** The movement of the snow, apart from the screen so that it can be tested: nothing here draws or reads a clock. */
internal object Snowfall {
    const val MIN_FLAKES = 30
    const val MAX_FLAKES = 300
    private const val AREA_PER_FLAKE = 4500f
    private const val MIN_RADIUS = 1.5f
    private const val MAX_RADIUS = 4.5f
    private const val TWO_PI = (2 * PI).toFloat()

    /** How many flakes a window of this size (in dp) calls for: about the same density everywhere, within a fixed limit. */
    fun flakeCount(width: Float, height: Float): Int = (width * height / AREA_PER_FLAKE).toInt().coerceIn(MIN_FLAKES, MAX_FLAKES)

    /** How far a flake of this [radius] falls in a second: the larger it is, the faster. */
    fun speedOf(radius: Float): Float = 18f + radius * 10f

    /** How strongly a flake of this [radius] is drawn: the smaller it is, the fainter, but always fairly faint. */
    fun alphaOf(radius: Float): Float = 0.3f + (radius - MIN_RADIUS) / (MAX_RADIUS - MIN_RADIUS) * 0.35f

    /** A flake at height [y] in a random column. */
    fun flake(random: Random, width: Float, y: Float): Flake {
        val radius = MIN_RADIUS + random.nextFloat() * (MAX_RADIUS - MIN_RADIUS)
        return Flake(
            x = random.nextFloat() * width,
            y = y,
            radius = radius,
            speed = speedOf(radius) * (0.9f + random.nextFloat() * 0.2f),
            sway = 6f + random.nextFloat() * 14f,
            swaySpeed = 0.6f + random.nextFloat(),
            phase = random.nextFloat() * TWO_PI,
            alpha = alphaOf(radius),
        )
    }

    /** The snow when it starts: flakes spread over the whole window, the same for the same [random]. */
    fun start(count: Int, width: Float, height: Float, random: Random): List<Flake> =
        List(count) { flake(random, width, random.nextFloat() * height) }

    /** Gives a window that was resized the number of flakes it calls for: new ones begin above the top, extra ones are dropped. */
    fun resize(flakes: List<Flake>, width: Float, height: Float, random: Random): List<Flake> {
        val count = flakeCount(width, height)
        return when {
            flakes.size > count -> flakes.take(count)
            flakes.size < count -> flakes + List(count - flakes.size) { flake(random, width, -MAX_RADIUS) }
            else -> flakes
        }
    }

    /**
     * The flakes after [seconds]: each has fallen, swayed on, and started again from above the top once it was below the bottom
     * (however long the step), and a flake whose column the window left behind is moved inside it.
     */
    fun step(flakes: List<Flake>, seconds: Float, width: Float, height: Float): List<Flake> = flakes.map { flake ->
        val span = height + 2 * flake.radius
        flake.copy(
            x = flake.x.coerceIn(0f, width),
            y = ((flake.y + flake.speed * seconds + flake.radius) % span + span) % span - flake.radius,
            phase = (flake.phase + flake.swaySpeed * seconds) % TWO_PI,
        )
    }
}
