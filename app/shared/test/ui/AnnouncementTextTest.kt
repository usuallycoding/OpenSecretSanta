package com.usuallycoding.opensecretsanta.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AnnouncementTextTest {
    @Test
    fun theConfirmationNamesTheGroupAndTheWays() {
        assertEquals("The participants of \"Party\" will be sent this as email. It cannot be taken back.", Strings.announceConfirm("Party", email = true, push = false))
        assertEquals("The participants of \"Party\" will be sent this as a push notification. It cannot be taken back.", Strings.announceConfirm("Party", email = false, push = true))
        assertEquals("The participants of \"Party\" will be sent this as email and a push notification. It cannot be taken back.", Strings.announceConfirm("Party", email = true, push = true))
    }

    @Test
    fun thePushSummaryCountsWhoWasReached() {
        assertEquals("Push notification sent to all 4 participants.", Strings.announcePushSummary(4, 0))
        assertEquals("Push notification sent to 3 of 5 participants. 2 have notifications off or no device.", Strings.announcePushSummary(3, 2))
        assertEquals("Push notification sent to 4 of 5 participants. 1 has notifications off or no device.", Strings.announcePushSummary(4, 1))
    }

    @Test
    fun theSubjectIsTheTitleThenTheGroup() {
        assertEquals("Schedule change: Office Party", Strings.announceSubject(" Schedule change ", "Office Party"))
    }
}
