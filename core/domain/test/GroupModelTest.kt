package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupStateTest {

    @Test
    fun statesFollowTheDocumentedOrder() {
        assertEquals(listOf("draft", "pending", "active", "archived"), GroupState.entries.map { it.storedName })
    }

    @Test
    fun eachStateHasExactlyOneForwardStep() {
        assertEquals(GroupState.PENDING, GroupState.DRAFT.next())
        assertEquals(GroupState.ACTIVE, GroupState.PENDING.next())
        assertEquals(GroupState.ARCHIVED, GroupState.ACTIVE.next())
        assertNull(GroupState.ARCHIVED.next())
    }

    @Test
    fun onlyTheNextStateIsReachable() {
        for (from in GroupState.entries) {
            for (to in GroupState.entries) {
                assertEquals(from.next() == to, from.canMoveTo(to), "$from -> $to")
            }
        }
    }

    @Test
    fun noStateCanMoveBackwardsSidewaysOrSkip() {
        assertFalse(GroupState.PENDING.canMoveTo(GroupState.DRAFT))
        assertFalse(GroupState.DRAFT.canMoveTo(GroupState.ACTIVE))
        assertFalse(GroupState.ACTIVE.canMoveTo(GroupState.PENDING))
        assertFalse(GroupState.ACTIVE.canMoveTo(GroupState.ACTIVE))
    }

    @Test
    fun participantsChangeOnlyBeforeActivationAndDetailsUntilArchived() {
        assertEquals(listOf(true, true, false, false), GroupState.entries.map { it.allowsParticipantChanges })
        assertEquals(listOf(true, true, true, false), GroupState.entries.map { it.allowsDetailChanges })
    }

    @Test
    fun storedNamesRoundTrip() {
        for (state in GroupState.entries) assertEquals(state, GroupState.fromStored(state.storedName))
        assertFailsWith<IllegalStateException> { GroupState.fromStored("bogus") }
    }
}

class GroupDetailsTest {

    private fun details(
        name: String? = "Office party",
        description: String? = null,
        location: String? = null,
        date: String? = null,
        time: String? = null,
    ) = GroupDetails.of(name, description, location, date, time)

    private fun fieldOf(block: () -> Unit) = assertFailsWith<DomainException.ValidationFailed> { block() }.field

    @Test
    fun onlyTheNameIsRequired() {
        assertEquals(GroupDetails("Office party"), details())
    }

    @Test
    fun fullDetailsAreParsed() {
        val result = details(description = "Bring snacks", location = "HQ", date = "2026-12-24", time = "18:30")
        assertEquals(LocalDate(2026, 12, 24), result.date)
        assertEquals(LocalTime(18, 30), result.time)
        assertEquals("Bring snacks", result.description)
    }

    @Test
    fun textIsTrimmedAndBlanksBecomeNull() {
        val result = details(name = "  Party  ", description = "   ", location = "", date = " ", time = "")
        assertEquals(GroupDetails("Party"), result)
    }

    @Test
    fun blankOrMissingNameIsRejected() {
        assertEquals("name", fieldOf { details(name = "   ") })
        assertEquals("name", fieldOf { details(name = null) })
    }

    @Test
    fun nameLengthBoundsAreEnforced() {
        assertEquals(100, details(name = "n".repeat(100)).name.length)
        assertEquals("name", fieldOf { details(name = "n".repeat(101)) })
    }

    @Test
    fun otherLengthLimitsAreEnforcedAndNamed() {
        assertEquals("description", fieldOf { details(description = "d".repeat(2001)) })
        assertEquals("location", fieldOf { details(location = "l".repeat(201)) })
        assertEquals(2000, details(description = "d".repeat(2000)).description!!.length)
    }

    @Test
    fun impossibleDatesAreRejected() {
        for (bad in listOf("2026-02-30", "2026-13-01", "24-12-2026", "tomorrow", "2026-12-24T10:00")) {
            assertEquals("date", fieldOf { details(date = bad) }, bad)
        }
    }

    @Test
    fun leapDayIsAccepted() {
        assertEquals(LocalDate(2028, 2, 29), details(date = "2028-02-29").date)
    }

    @Test
    fun invalidTimesOfDayAreRejected() {
        for (bad in listOf("24:00", "12:60", "6pm", "18-30", "noon")) {
            assertEquals("time", fieldOf { details(time = bad) }, bad)
        }
    }

    @Test
    fun validTimesAreAccepted() {
        assertEquals(LocalTime(0, 0), details(time = "00:00").time)
        assertEquals(LocalTime(23, 59), details(time = "23:59").time)
        assertTrue(details(time = "18:30:15").time != null)
    }
}
