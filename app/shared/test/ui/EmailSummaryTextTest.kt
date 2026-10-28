package com.usuallycoding.opensecretsanta.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailSummaryTextTest {
    @Test
    fun everyoneEmailedSaysAll() {
        assertEquals("Emails sent to all 4 participants.", Strings.emailSummary(sent = 4, failed = 0, noAddress = 0))
    }

    @Test
    fun failuresAndMissingAddressesAreListed() {
        assertEquals(
            "Emails sent to 8 of 11 participants. 1 failed, 2 have no email address.",
            Strings.emailSummary(sent = 8, failed = 1, noAddress = 2),
        )
    }

    @Test
    fun oneMissingAddressIsSingularAndOnlyWhatHappenedIsListed() {
        assertEquals("Emails sent to 3 of 4 participants. 1 has no email address.", Strings.emailSummary(sent = 3, failed = 0, noAddress = 1))
        assertEquals("Emails sent to 3 of 4 participants. 1 failed.", Strings.emailSummary(sent = 3, failed = 1, noAddress = 0))
    }

    @Test
    fun nothingSentIsSaidPlainly() {
        assertEquals("Emails sent to 0 of 4 participants. 4 failed.", Strings.emailSummary(sent = 0, failed = 4, noAddress = 0))
    }

    @Test
    fun theProfileNoteSaysWhatTheEmailIsFor() {
        assertTrue("email" in Strings.PROFILE_WHY && "draw" in Strings.PROFILE_WHY)
        assertTrue(Strings.PROFILE_WHY.startsWith("A Santa uses your name and address"))
    }
}
