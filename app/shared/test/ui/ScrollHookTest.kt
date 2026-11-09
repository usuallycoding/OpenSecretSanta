package com.usuallycoding.opensecretsanta.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.usuallycoding.opensecretsanta.ui.form.scrollHook
import kotlin.test.Test
import kotlin.test.assertEquals

class ScrollHookTest {
    private val scrolled = Offset(0f, 40f)
    private val left = Offset(0f, 15f)

    /** How many times the hook reported a scroll for one vertical scroll of the content, [consumed] by it and [available] after it. */
    private fun reports(
        countConsumed: Boolean,
        fingerDown: Boolean,
        source: NestedScrollSource = NestedScrollSource.UserInput,
        consumed: Offset = scrolled,
        available: Offset = Offset.Zero,
    ): Int {
        var count = 0
        scrollHook(countConsumed, fingerDown = { fingerDown }) { count++ }.onPostScroll(consumed, available, source)
        return count
    }

    @Test
    fun aScrollWithNoFingerDownIsTheAppsOwn() {
        assertEquals(0, reports(countConsumed = true, fingerDown = false))
        assertEquals(0, reports(countConsumed = false, fingerDown = false, consumed = Offset.Zero, available = left))
    }

    @Test
    fun aScrollWithAFingerDownIsTheDrag() {
        assertEquals(1, reports(countConsumed = true, fingerDown = true))
        assertEquals(1, reports(countConsumed = false, fingerDown = true, consumed = Offset.Zero, available = left))
    }

    @Test
    fun whatTheContentScrolledCountsOnlyWhenAsked() {
        assertEquals(0, reports(countConsumed = false, fingerDown = true))
    }

    @Test
    fun aScrollThatIsNotUserInputIsNeverTheDrag() {
        assertEquals(0, reports(countConsumed = true, fingerDown = true, source = NestedScrollSource.SideEffect))
    }

    @Test
    fun aSidewaysScrollIsLeftToTheFieldsOwnText() {
        assertEquals(0, reports(countConsumed = true, fingerDown = true, consumed = Offset(30f, 0f)))
    }
}
