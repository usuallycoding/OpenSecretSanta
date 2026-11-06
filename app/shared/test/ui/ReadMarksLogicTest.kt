package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.readmarks.Change
import com.usuallycoding.opensecretsanta.ui.readmarks.SeenBaseline
import com.usuallycoding.opensecretsanta.ui.readmarks.answerIsNew
import com.usuallycoding.opensecretsanta.ui.readmarks.itemChange
import com.usuallycoding.opensecretsanta.ui.readmarks.letterIsNew
import com.usuallycoding.opensecretsanta.ui.readmarks.newestOf
import com.usuallycoding.opensecretsanta.ui.readmarks.parseTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadMarksLogicTest {
    private val seen = SeenBaseline(known = true, seenUpTo = parseTime("2026-12-04T10:00:00Z"))
    private val never = SeenBaseline(known = true, seenUpTo = null)
    private val unknown = SeenBaseline(known = false, seenUpTo = null)

    private val before = "2026-12-04T09:00:00Z"
    private val after = "2026-12-04T11:00:00Z"

    @Test
    fun anItemCreatedAfterTheBaselineIsNewAndOneChangedAfterItIsEdited() {
        assertEquals(Change.New, itemChange(after, after, seen))
        assertEquals(Change.New, itemChange(after, "2026-12-04T12:00:00Z", seen))
        assertEquals(Change.Edited, itemChange(before, after, seen))
        assertNull(itemChange(before, before, seen))
    }

    @Test
    fun everythingIsNewForASantaWhoNeverLooked() {
        assertEquals(Change.New, itemChange(before, before, never))
        assertTrue(answerIsNew(before, never))
        assertTrue(letterIsNew(before, never))
    }

    @Test
    fun nothingIsMarkedWhenTheBaselineIsUnknownOrMissing() {
        assertNull(itemChange(after, after, unknown))
        assertNull(itemChange(after, after, null))
        assertFalse(answerIsNew(after, unknown))
        assertFalse(answerIsNew(after, null))
        assertFalse(letterIsNew(after, unknown))
        assertFalse(letterIsNew(after, null))
    }

    @Test
    fun anAnswerGivenOrChangedAfterTheBaselineIsNewAndAnUnansweredQuestionIsNot() {
        assertTrue(answerIsNew(after, seen))
        assertFalse(answerIsNew(before, seen))
        assertFalse(answerIsNew(null, seen))
        assertFalse(answerIsNew(null, never))
    }

    @Test
    fun aLetterSentAfterTheBaselineIsNew() {
        assertTrue(letterIsNew(after, seen))
        assertFalse(letterIsNew(before, seen))
    }

    @Test
    fun aTimeExactlyAtTheBaselineIsNotNew() {
        assertNull(itemChange("2026-12-04T10:00:00Z", "2026-12-04T10:00:00Z", seen))
        assertFalse(letterIsNew("2026-12-04T10:00:00Z", seen))
        assertFalse(answerIsNew("2026-12-04T10:00:00Z", seen))
    }

    @Test
    fun timesAreComparedAsInstantsNotAsText() {
        // as text "…10:00:00Z" sorts after "…10:00:00.500Z", although it is half a second earlier
        val half = SeenBaseline(known = true, seenUpTo = parseTime("2026-12-04T10:00:00.500Z"))

        assertFalse(letterIsNew("2026-12-04T10:00:00Z", half))
        assertTrue(letterIsNew("2026-12-04T10:00:01Z", half))
        assertEquals("2026-12-04T10:00:00.700Z", newestOf(listOf("2026-12-04T10:00:00Z", "2026-12-04T10:00:00.700Z", "2026-12-04T09:59:59.999Z")))
    }

    @Test
    fun theNewestTimeIsNullWhenThereIsNoneToRead() {
        assertNull(newestOf(emptyList()))
        assertNull(newestOf(listOf(null, "not a time")))
        assertEquals(after, newestOf(listOf(before, null, after, "junk")))
    }
}
