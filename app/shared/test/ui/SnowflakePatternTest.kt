package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.snow.SnowflakePattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SnowflakePatternTest {
    @Test
    fun theSameInputGivesTheSameFlakes() {
        assertEquals(SnowflakePattern.flakes(7, 360f, 120f), SnowflakePattern.flakes(7, 360f, 120f))
    }

    @Test
    fun differentSeedsGiveDifferentFlakes() {
        assertNotEquals(SnowflakePattern.flakes(1, 360f, 120f), SnowflakePattern.flakes(2, 360f, 120f))
    }

    @Test
    fun aLargerCardHasMoreFlakesAtAboutTheSameDensity() {
        val small = SnowflakePattern.flakes(3, 360f, 100f)
        val large = SnowflakePattern.flakes(3, 360f, 700f)

        assertTrue(large.size > small.size)
        val smallDensity = small.size / (360f * 100f)
        val largeDensity = large.size / (360f * 700f)
        assertTrue(largeDensity in smallDensity / 2.5f..smallDensity * 2.5f, "$smallDensity $largeDensity")
    }

    @Test
    fun everyShapeIsUsedOnALargeCard() {
        val shapes = SnowflakePattern.flakes(5, 800f, 800f).map { it.shape }.toSet()

        assertEquals((0 until SnowflakePattern.SHAPES).toSet(), shapes)
    }

    @Test
    fun flakesVaryInSizeAndAngleWithinTheirLimits() {
        val flakes = SnowflakePattern.flakes(9, 800f, 800f)

        assertTrue(flakes.all { it.size in SnowflakePattern.MIN_SIZE..SnowflakePattern.MAX_SIZE })
        assertTrue(flakes.all { it.angle in 0f..360f })
        assertTrue(flakes.maxOf { it.size } - flakes.minOf { it.size } > 20f)
        assertTrue(flakes.all { it.alpha in 0.08f..0.14f })
    }

    @Test
    fun someFlakesAreCutOffByTheEdgeAndNoneIsFarOutside() {
        val flakes = SnowflakePattern.flakes(11, 360f, 300f)
        val overhang = SnowflakePattern.OVERHANG

        assertTrue(flakes.all { it.x in -overhang..360f + overhang && it.y in -overhang..300f + overhang })
        assertTrue(flakes.any { it.x - it.size / 2 < 0f || it.x + it.size / 2 > 360f || it.y - it.size / 2 < 0f || it.y + it.size / 2 > 300f })
    }

    @Test
    fun aCardWithNoSizeHasNoFlakes() {
        assertTrue(SnowflakePattern.flakes(1, 0f, 100f).isEmpty())
        assertTrue(SnowflakePattern.flakes(1, 100f, 0f).isEmpty())
    }
}
