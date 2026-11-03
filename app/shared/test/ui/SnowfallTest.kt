package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.snow.Flake
import com.usuallycoding.opensecretsanta.ui.snow.Snowfall
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnowfallTest {
    private val flake = Flake(x = 100f, y = 50f, radius = 3f, speed = 40f, sway = 10f, swaySpeed = 1f, phase = 0f, alpha = 0.5f)

    @Test
    fun aFlakeFallsAtItsSpeed() {
        val moved = Snowfall.step(listOf(flake), seconds = 0.5f, width = 400f, height = 800f).single()

        assertEquals(70f, moved.y, 0.001f)
        assertEquals(100f, moved.x)
    }

    @Test
    fun aFlakeSwaysSidewaysAsItFalls() {
        val xs = (0..20).map { i -> Snowfall.step(listOf(flake), seconds = i * 0.3f, width = 400f, height = 800f).single().drawnX(400f) }

        assertTrue(xs.max() - xs.min() > 10f)
        assertTrue(xs.all { it in 90f..110f })
    }

    @Test
    fun aFlakeBelowTheBottomStartsAgainAboveTheTop() {
        val low = flake.copy(y = 802f)

        val moved = Snowfall.step(listOf(low), seconds = 0.05f, width = 400f, height = 800f).single()

        assertTrue(moved.y < 0f && moved.y >= -low.radius, "y=${moved.y}")
    }

    @Test
    fun aVeryLongStepStillLeavesTheFlakeInsideTheFallAndNotLost() {
        val moved = Snowfall.step(listOf(flake), seconds = 10_000f, width = 400f, height = 800f).single()

        assertTrue(moved.y >= -flake.radius && moved.y <= 800f + flake.radius, "y=${moved.y}")
    }

    @Test
    fun flakesNeverLeaveTheWindowSidewaysEvenWhenItShrinks() {
        var flakes = Snowfall.start(120, 400f, 800f, Random(1))
        repeat(200) { i ->
            val width = if (i < 100) 400f else 150f
            flakes = Snowfall.step(flakes, 0.016f, width, 800f)
            assertTrue(flakes.all { it.drawnX(width) in 0f..width }, "step $i")
        }
    }

    @Test
    fun aPhoneAndAVeryLargeWindowAreAboutAsDenseUpToTheLimit() {
        val phone = Snowfall.flakeCount(400f, 850f)
        val desktop = Snowfall.flakeCount(1600f, 900f)
        val huge = Snowfall.flakeCount(8000f, 6000f)

        assertEquals(75, phone)
        assertTrue(desktop <= Snowfall.MAX_FLAKES)
        assertEquals(Snowfall.MAX_FLAKES, huge)
        assertTrue(desktop / (1600f * 900f) > 0.8f * phone / (400f * 850f))
    }

    @Test
    fun aTinyWindowStillHasSomeSnow() {
        assertEquals(Snowfall.MIN_FLAKES, Snowfall.flakeCount(50f, 50f))
    }

    @Test
    fun theSameSeedStartsTheSameSnow() {
        assertEquals(Snowfall.start(50, 400f, 800f, Random(7)), Snowfall.start(50, 400f, 800f, Random(7)))
    }

    @Test
    fun theStartIsSpreadOverTheWholeWindow() {
        val flakes = Snowfall.start(200, 400f, 800f, Random(3))

        assertTrue(flakes.all { it.y in 0f..800f && it.x in 0f..400f })
        assertTrue(flakes.any { it.y < 200f } && flakes.any { it.y > 600f })
    }

    @Test
    fun smallerFlakesAreSlowerAndFainterThanLargerOnes() {
        val flakes = Snowfall.start(300, 400f, 800f, Random(5)).sortedBy { it.radius }

        assertTrue(Snowfall.speedOf(flakes.first().radius) < Snowfall.speedOf(flakes.last().radius))
        assertTrue(Snowfall.alphaOf(flakes.first().radius) < Snowfall.alphaOf(flakes.last().radius))
        assertTrue(flakes.all { it.alpha in 0.3f..0.65f })
    }

    @Test
    fun resizingGivesTheWindowItsNumberOfFlakes() {
        val flakes = Snowfall.start(Snowfall.flakeCount(400f, 800f), 400f, 800f, Random(2))

        val bigger = Snowfall.resize(flakes, 1200f, 800f, Random(2))
        val smaller = Snowfall.resize(bigger, 200f, 400f, Random(2))

        assertEquals(Snowfall.flakeCount(1200f, 800f), bigger.size)
        assertEquals(Snowfall.flakeCount(200f, 400f), smaller.size)
        assertTrue(bigger.drop(flakes.size).all { it.y < 0f })
    }
}
