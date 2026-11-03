package com.usuallycoding.opensecretsanta.ui

import androidx.compose.ui.geometry.Offset
import com.usuallycoding.opensecretsanta.ui.form.Gesture
import com.usuallycoding.opensecretsanta.ui.form.GestureTracker
import kotlin.test.Test
import kotlin.test.assertEquals

class GestureTrackerTest {
    private val slop = 8f
    private val start = Offset(100f, 100f)

    private fun GestureTracker.down(at: Offset = start, consumed: Boolean = false, uptime: Long = 1L) = onEvent(at, true, consumed, uptime, slop)
    private fun GestureTracker.move(to: Offset, consumed: Boolean = false) = onEvent(to, true, consumed, 2L, slop)
    private fun GestureTracker.up(at: Offset = start, consumed: Boolean = false) = onEvent(at, false, consumed, 3L, slop)

    @Test
    fun aTouchThatNothingUsedIsATap() {
        val tracker = GestureTracker()
        assertEquals(Gesture.None, tracker.down())
        assertEquals(Gesture.Tap, tracker.up())
    }

    @Test
    fun aTouchThatAControlUsedIsNotATap() {
        val onDown = GestureTracker().apply { down(consumed = true) }
        assertEquals(Gesture.None, onDown.up())
        val onRelease = GestureTracker().apply { down() }
        assertEquals(Gesture.None, onRelease.up(consumed = true))
    }

    @Test
    fun aSmallWobbleIsStillATap() {
        val tracker = GestureTracker()
        tracker.down()
        assertEquals(Gesture.None, tracker.move(Offset(104f, 103f)))
        assertEquals(Gesture.Tap, tracker.up(Offset(104f, 103f)))
    }

    @Test
    fun aDragNothingUsedIsReportedOnceAndIsNoTap() {
        val tracker = GestureTracker()
        tracker.down()
        assertEquals(Gesture.Drag, tracker.move(Offset(100f, 130f)))
        assertEquals(Gesture.None, tracker.move(Offset(100f, 160f)))
        assertEquals(Gesture.None, tracker.up(Offset(100f, 160f)))
    }

    @Test
    fun aDragThatIsUsedIsNotReported() {
        val tracker = GestureTracker()
        tracker.down()
        assertEquals(Gesture.None, tracker.move(Offset(100f, 130f), consumed = true))
        assertEquals(Gesture.None, tracker.up(Offset(100f, 130f)))
    }

    @Test
    fun aDragThatStartsOnAControlIsStillReportedWhenTheMovesAreFree() {
        val tracker = GestureTracker()
        tracker.down(consumed = true)
        assertEquals(Gesture.Drag, tracker.move(Offset(160f, 100f)))
    }

    @Test
    fun theTouchIsRemembered() {
        val tracker = GestureTracker()
        assertEquals(-1L, tracker.downAt)
        tracker.down(uptime = 42L)
        assertEquals(42L, tracker.downAt)
        tracker.up()
        assertEquals(-1L, tracker.downAt)
    }

    @Test
    fun eachTouchStartsFresh() {
        val tracker = GestureTracker()
        tracker.down()
        tracker.move(Offset(100f, 160f))
        tracker.up()
        tracker.down()
        assertEquals(Gesture.Tap, tracker.up())
    }

    @Test
    fun movesWithNoTouchAreNothing() {
        val tracker = GestureTracker()
        assertEquals(Gesture.None, tracker.up())
    }
}
