package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.groups.paneTitle
import com.usuallycoding.opensecretsanta.ui.navigation.GroupPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupPaneTitleTest {
    @Test
    fun theReceiversPagesNameTheReceiver() {
        assertEquals("bob's Wishlist", paneTitle(GroupPane.GifterWishlist, "bob"))
        assertEquals("Questions for bob", paneTitle(GroupPane.GifterQuestions, "bob"))
        assertEquals("Letters from bob", paneTitle(GroupPane.Inbox, "bob"))
    }

    @Test
    fun theyReadWithoutTheNameUntilItIsKnown() {
        assertEquals("Wishlist", paneTitle(GroupPane.GifterWishlist, null))
        assertEquals("Questions", paneTitle(GroupPane.GifterQuestions, null))
        assertEquals("Letters from your receiver", paneTitle(GroupPane.Inbox, null))
    }

    @Test
    fun thePersonsOwnPagesAndTheAnnouncementsHaveTheirOwnTitles() {
        assertEquals("Your Wishlist", paneTitle(GroupPane.ReceiverWishlist, "bob"))
        assertEquals("Respond to Secret Santa", paneTitle(GroupPane.ReceiverQuestions, "bob"))
        assertEquals("Letters to your Santa", paneTitle(GroupPane.Outbox, "bob"))
        assertEquals("Announcements", paneTitle(GroupPane.Announcements, null))
    }

    @Test
    fun everyPaneHasATitleThatIsNeverBlank() {
        for (pane in GroupPane.entries) {
            assertTrue(paneTitle(pane, null).isNotBlank(), pane.name)
            assertTrue(paneTitle(pane, "bob").isNotBlank(), pane.name)
        }
    }
}
