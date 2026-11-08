package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.gift.countBadgeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What the badges for the announcements a person has not seen say, to the eye and to a screen reader. */
class AnnouncementBadgesTest {
    @Test
    fun theBellsBadgeSaysTheNumberNineAndMoreAndNothingForNone() {
        assertNull(countBadgeText(0))
        assertEquals("3", countBadgeText(3))
        assertEquals("9", countBadgeText(9))
        assertEquals("9+", countBadgeText(12))
    }

    @Test
    fun theBellIsReadOutWithItsCount() {
        assertEquals("Announcements, 3 new", Strings.withNew(Strings.ANNOUNCEMENTS, 3))
    }

    @Test
    fun aGroupsEntryAndItsSidebarLineAreReadOutWithTheRealNumber() {
        assertEquals("1 new announcement", Strings.newAnnouncements(1))
        assertEquals("3 new announcements", Strings.newAnnouncements(3))
        assertEquals("12 new announcements", Strings.newAnnouncements(12))
    }
}
