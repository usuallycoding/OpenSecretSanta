package com.usuallycoding.opensecretsanta.ui

import androidx.compose.foundation.gestures.ScrollableState
import com.usuallycoding.opensecretsanta.ui.layout.OutsideScroll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class OutsideScrollTest {
    /** A scroller of [length] units that starts at the top and uses what it can of every delta, like a page does. */
    private class Page(val length: Float) {
        var position = 0f
        val state = ScrollableState { delta ->
            val next = (position + delta).coerceIn(0f, length)
            (next - position).also { position = next }
        }
    }

    @Test
    fun withNothingRegisteredNothingMoves() {
        val host = OutsideScroll()
        assertNull(host.target)
        assertEquals(0f, host.scrollBy(40f))
    }

    @Test
    fun theNewestPageIsTheTarget() {
        val host = OutsideScroll()
        val first = Page(500f)
        val second = Page(500f)
        host.add(first.state)
        host.add(second.state)

        assertSame(second.state, host.target)
        host.scrollBy(30f)

        assertEquals(0f, first.position)
        assertEquals(30f, second.position)
    }

    @Test
    fun aPageThatLeavesHandsTheTargetBack() {
        val host = OutsideScroll()
        val first = Page(500f)
        val second = Page(500f)
        host.add(first.state)
        host.add(second.state)

        host.remove(second.state)
        host.scrollBy(20f)

        assertSame(first.state, host.target)
        assertEquals(20f, first.position)
        assertEquals(0f, second.position)
    }

    @Test
    fun aPageThatLeavesBeforeANewerOneKeepsTheNewerOneAsTarget() {
        val host = OutsideScroll()
        val first = Page(500f)
        val second = Page(500f)
        host.add(first.state)
        host.add(second.state)

        host.remove(first.state)

        assertSame(second.state, host.target)
    }

    @Test
    fun theDeltaMovesThePageInItsOwnDirectionAndSaysHowMuchWasUsed() {
        val host = OutsideScroll()
        val page = Page(100f)
        host.add(page.state)

        assertEquals(60f, host.scrollBy(60f))
        assertEquals(40f, host.scrollBy(60f))
        assertEquals(100f, page.position)
        assertEquals(-100f, host.scrollBy(-150f))
        assertEquals(0f, page.position)
    }

    @Test
    fun aPageThatCannotScrollUsesNothing() {
        val host = OutsideScroll()
        host.add(Page(0f).state)

        assertEquals(0f, host.scrollBy(50f))
    }

    @Test
    fun removingSomethingNotRegisteredChangesNothing() {
        val host = OutsideScroll()
        val page = Page(100f)
        host.add(page.state)

        host.remove(Page(100f).state)

        assertSame(page.state, host.target)
    }
}
