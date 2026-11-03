package com.usuallycoding.opensecretsanta.ui.admin

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupPickerLogicTest {
    @Test
    fun anEmptyDateStartsOnThisYearsChristmas() {
        assertEquals(LocalDate(2026, 12, 25), defaultGroupDate(LocalDate(2026, 11, 3)))
        assertEquals(LocalDate(2026, 12, 25), defaultGroupDate(LocalDate(2026, 1, 1)))
    }

    @Test
    fun onChristmasDayItIsStillThisYear() {
        assertEquals(LocalDate(2026, 12, 25), defaultGroupDate(LocalDate(2026, 12, 25)))
    }

    @Test
    fun afterChristmasItIsNextYear() {
        assertEquals(LocalDate(2027, 12, 25), defaultGroupDate(LocalDate(2026, 12, 26)))
        assertEquals(LocalDate(2027, 12, 25), defaultGroupDate(LocalDate(2026, 12, 31)))
    }

    @Test
    fun anEmptyTimeStartsAtMidnight() {
        assertEquals("00:00", DEFAULT_GROUP_TIME.toString())
    }

    @Test
    fun theDatePickerCountsUtcMidnight() {
        assertEquals(1798070400000L, LocalDate(2026, 12, 24).toPickerMillis())
    }

    @Test
    fun aDateSurvivesThePickerWhateverTheZone() {
        for (date in listOf(LocalDate(2026, 12, 24), LocalDate(2026, 1, 1), LocalDate(2027, 12, 31), LocalDate(2028, 2, 29))) {
            assertEquals(date, dateFromPickerMillis(date.toPickerMillis()))
        }
    }

    @Test
    fun theDatePickersMillisAreReadInUtcNotInTheDevicesZone() {
        // Midnight UTC is still the evening before in a zone behind UTC; the date must stay the 24th
        assertEquals(LocalDate(2026, 12, 24), dateFromPickerMillis(1798070400000L))
    }

    @Test
    fun theFormFieldsReadBackAsDatesAndTimes() {
        assertEquals(LocalDate(2026, 12, 24), parseGroupDate("2026-12-24"))
        assertEquals(LocalTime(18, 30), parseGroupTime("18:30"))
        assertEquals("18:30", parseGroupTime("18:30")?.toString())
    }

    @Test
    fun anEmptyOrBrokenFieldReadsAsNothing() {
        assertNull(parseGroupDate(""))
        assertNull(parseGroupDate("2026-13-45"))
        assertNull(parseGroupTime(""))
        assertNull(parseGroupTime("25:99"))
    }
}
