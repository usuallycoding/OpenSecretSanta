package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.groups.formatInstant
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every time is shown in Bucharest time: UTC+2 in winter, UTC+3 in summer, with the EU's clock changes. */
class FormatInstantTest {
    @Test
    fun winterTimeIsTwoHoursAheadOfUtc() {
        assertEquals("1 Dec 2026, 12:00", formatInstant("2026-12-01T10:00:00Z"))
    }

    @Test
    fun summerTimeIsThreeHoursAheadOfUtc() {
        assertEquals("1 Jul 2026, 13:00", formatInstant("2026-07-01T10:00:00Z"))
    }

    @Test
    fun theClocksGoForwardOnTheLastSundayOfMarchAtOneUtc() {
        assertEquals("29 Mar 2026, 02:59", formatInstant("2026-03-29T00:59:00Z"))
        assertEquals("29 Mar 2026, 04:00", formatInstant("2026-03-29T01:00:00Z"))
        assertEquals("28 Mar 2026, 23:30", formatInstant("2026-03-28T21:30:00Z"))
    }

    @Test
    fun theClocksGoBackOnTheLastSundayOfOctoberAtOneUtc() {
        assertEquals("25 Oct 2026, 03:59", formatInstant("2026-10-25T00:59:00Z"))
        assertEquals("25 Oct 2026, 03:00", formatInstant("2026-10-25T01:00:00Z"))
        assertEquals("26 Oct 2026, 02:00", formatInstant("2026-10-26T00:00:00Z"))
    }

    @Test
    fun theNextDayIsReachedAcrossMidnightAndNewYear() {
        assertEquals("1 Jan 2027, 00:30", formatInstant("2026-12-31T22:30:00Z"))
        assertEquals("2 Jul 2026, 00:15", formatInstant("2026-07-01T21:15:00Z"))
    }

    @Test
    fun secondsAndMillisecondsAreAccepted() {
        assertEquals("24 Dec 2026, 20:30", formatInstant("2026-12-24T18:30:15.123Z"))
        assertEquals("24 Dec 2026, 20:30", formatInstant("2026-12-24T18:30:00Z"))
    }

    @Test
    fun thereIsNoZoneSuffixAndSingleDigitDaysAreNotPadded() {
        assertEquals("5 Jan 2026, 09:05", formatInstant("2026-01-05T07:05:00Z"))
    }

    @Test
    fun anythingElseIsShownAsItCame() {
        assertEquals("yesterday", formatInstant("yesterday"))
        assertEquals("", formatInstant(""))
        assertEquals("2026-13-45T99:99:00Z", formatInstant("2026-13-45T99:99:00Z"))
    }
}
