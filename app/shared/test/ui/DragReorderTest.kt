package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.wishlist.dragTargetIndex
import com.usuallycoding.opensecretsanta.ui.wishlist.edgeScrollSpeed
import com.usuallycoding.opensecretsanta.ui.wishlist.rowShift
import com.usuallycoding.opensecretsanta.ui.wishlist.settleOffset
import com.usuallycoding.opensecretsanta.ui.wishlist.shownNumber
import kotlin.test.Test
import kotlin.test.assertEquals

class DragReorderTest {
    // four rows with a gap of 10: tops 0, 110, 180, 310 and heights 100, 50, 120, 80 (middles 50, 135, 240, 350)
    private val tops = listOf(0f, 110f, 180f, 310f)
    private val heights = listOf(100f, 50f, 120f, 80f)

    @Test
    fun aRowThatHasNotPassedAMiddleStaysInItsSlot() {
        assertEquals(0, dragTargetIndex(tops, heights, from = 0, offset = 0f))
        assertEquals(0, dragTargetIndex(tops, heights, from = 0, offset = 80f)) // middle 130, row 1's middle is 135
        assertEquals(2, dragTargetIndex(tops, heights, from = 2, offset = -50f))
    }

    @Test
    fun aRowTakesTheSlotOfEveryNeighbourWhoseMiddleItPassed() {
        assertEquals(1, dragTargetIndex(tops, heights, from = 0, offset = 90f)) // middle 140 > 135
        assertEquals(2, dragTargetIndex(tops, heights, from = 0, offset = 200f)) // middle 250 > 240
        assertEquals(2, dragTargetIndex(tops, heights, from = 3, offset = -150f)) // middle 200 < 240, > 135
        assertEquals(1, dragTargetIndex(tops, heights, from = 3, offset = -220f)) // middle 130 < 135, > 50
        assertEquals(0, dragTargetIndex(tops, heights, from = 3, offset = -320f)) // middle 30 < 50
    }

    @Test
    fun theEndsOfTheListAreTheLimits() {
        assertEquals(3, dragTargetIndex(tops, heights, from = 1, offset = 5000f))
        assertEquals(0, dragTargetIndex(tops, heights, from = 2, offset = -5000f))
    }

    @Test
    fun aSingleRowStaysPut() {
        assertEquals(0, dragTargetIndex(listOf(0f), listOf(100f), from = 0, offset = 300f))
    }

    @Test
    fun theRowsBetweenMakeRoomTowardsTheDraggedRowsPlace() {
        // row 0 dragged over slot 2: rows 1 and 2 move up
        assertEquals(0f, rowShift(0, from = 0, target = 2, span = 110f))
        assertEquals(-110f, rowShift(1, from = 0, target = 2, span = 110f))
        assertEquals(-110f, rowShift(2, from = 0, target = 2, span = 110f))
        assertEquals(0f, rowShift(3, from = 0, target = 2, span = 110f))
        // row 3 dragged over slot 1: rows 1 and 2 move down
        assertEquals(0f, rowShift(0, from = 3, target = 1, span = 90f))
        assertEquals(90f, rowShift(1, from = 3, target = 1, span = 90f))
        assertEquals(90f, rowShift(2, from = 3, target = 1, span = 90f))
        assertEquals(0f, rowShift(3, from = 3, target = 1, span = 90f))
        // not over another slot: nobody moves
        assertEquals(0f, rowShift(1, from = 2, target = 2, span = 90f))
    }

    @Test
    fun theNumbersFollowTheRowsAsTheyChangePlaces() {
        // row 0 over slot 2: it shows 3, rows 1 and 2 show 1 and 2, row 3 keeps 4
        assertEquals(listOf(3, 1, 2, 4), (0..3).map { shownNumber(it, from = 0, target = 2) })
        // row 3 over slot 1: it shows 2, rows 1 and 2 show 3 and 4, row 0 keeps 1
        assertEquals(listOf(1, 3, 4, 2), (0..3).map { shownNumber(it, from = 3, target = 1) })
        // over its own slot nothing changes
        assertEquals(listOf(1, 2, 3, 4), (0..3).map { shownNumber(it, from = 2, target = 2) })
    }

    @Test
    fun theNumbersAreAlwaysAPermutationOfTheList() {
        for (from in 0..3) for (target in 0..3) {
            assertEquals(listOf(1, 2, 3, 4), (0..3).map { shownNumber(it, from, target) }.sorted())
        }
    }

    @Test
    fun aDroppedRowRestsWhereTheRowsBetweenLeftRoomForIt() {
        // row 0 over slot 2: rows 1 and 2 move up by 110, so row 2 ends at 190 and row 0 starts a gap below it
        assertEquals(200f, settleOffset(tops, heights, from = 0, target = 2))
        // row 3 over slot 1: it takes the top row 1 had
        assertEquals(-200f, settleOffset(tops, heights, from = 3, target = 1))
        assertEquals(0f, settleOffset(tops, heights, from = 2, target = 2))
    }

    @Test
    fun theListScrollsOnlyNearAnEdgeAndFasterTheCloserToIt() {
        // viewport 100..900, band 100, top speed 20
        assertEquals(0f, edgeScrollSpeed(500f, 100f, 900f, 100f, 20f))
        assertEquals(0f, edgeScrollSpeed(200f, 100f, 900f, 100f, 20f))
        assertEquals(-10f, edgeScrollSpeed(150f, 100f, 900f, 100f, 20f))
        assertEquals(-20f, edgeScrollSpeed(100f, 100f, 900f, 100f, 20f))
        assertEquals(10f, edgeScrollSpeed(850f, 100f, 900f, 100f, 20f))
        assertEquals(20f, edgeScrollSpeed(900f, 100f, 900f, 100f, 20f))
    }

    @Test
    fun beyondTheViewportTheSpeedStopsGrowing() {
        assertEquals(-20f, edgeScrollSpeed(-300f, 100f, 900f, 100f, 20f))
        assertEquals(20f, edgeScrollSpeed(2000f, 100f, 900f, 100f, 20f))
    }
}
