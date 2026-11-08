package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.navigation.GroupPane
import com.usuallycoding.opensecretsanta.ui.navigation.givingWithin
import com.usuallycoding.opensecretsanta.ui.navigation.groupIdOfPane
import com.usuallycoding.opensecretsanta.ui.navigation.paneOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupPaneTest {
    @Test
    fun everyPaneHasARouteThatNamesItAndItsGroup() {
        for (pane in GroupPane.entries) {
            val route = pane.route(7)
            assertEquals(pane, paneOf(route), pane.name)
            assertEquals(7L, groupIdOfPane(route), pane.name)
        }
    }

    @Test
    fun anythingButAPaneRouteIsNoPane() {
        assertNull(paneOf("not a route"))
        assertNull(paneOf(com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute(7)))
        assertNull(groupIdOfPane(com.usuallycoding.opensecretsanta.ui.navigation.GifterRoute(7)))
    }

    @Test
    fun aDrawnGroupKeepsEveryPane() {
        for (phase in listOf(GroupPhase.Active, GroupPhase.Archived)) {
            for (pane in GroupPane.entries) assertEquals(pane, pane.within(phase), "$phase $pane")
        }
    }

    @Test
    fun aPendingGroupHasItsOwnWishlistAndTheAnnouncementsOnly() {
        assertEquals(GroupPane.Announcements, GroupPane.Announcements.within(GroupPhase.Pending))
        assertEquals(GroupPane.ReceiverWishlist, GroupPane.ReceiverWishlist.within(GroupPhase.Pending))
        assertEquals(GroupPane.ReceiverWishlist, GroupPane.ReceiverQuestions.within(GroupPhase.Pending))
        for (pane in listOf(GroupPane.GifterWishlist, GroupPane.GifterQuestions, GroupPane.Inbox, GroupPane.Outbox)) {
            assertNull(pane.within(GroupPhase.Pending), pane.name)
        }
    }

    @Test
    fun aGroupInAnyOtherPhaseHasNoPane() {
        for (pane in GroupPane.entries) assertNull(pane.within(GroupPhase.Other), pane.name)
    }

    @Test
    fun whatAPaneBecomesIsNeverChangedAgainSoARedirectCannotLoop() {
        for (phase in GroupPhase.entries) {
            for (pane in GroupPane.entries) {
                val first = pane.within(phase) ?: continue
                assertEquals(first, first.within(phase), "$phase $pane")
            }
        }
    }

    @Test
    fun theGivingPageHasSomethingOnlyOnceTheNamesAreDrawn() {
        assertTrue(givingWithin(GroupPhase.Active))
        assertTrue(givingWithin(GroupPhase.Archived))
        assertFalse(givingWithin(GroupPhase.Pending))
        assertFalse(givingWithin(GroupPhase.Other))
    }
}
