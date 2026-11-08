package com.usuallycoding.opensecretsanta.ui

import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.layout.isWide
import com.usuallycoding.opensecretsanta.ui.navigation.GifterRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverRoute
import com.usuallycoding.opensecretsanta.ui.readmarks.UnreadCounts
import com.usuallycoding.opensecretsanta.ui.shell.groupItems
import com.usuallycoding.opensecretsanta.ui.shell.holds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WideLayoutTest {
    @Test
    fun aWindowIsWideFrom840dp() {
        assertFalse(isWide(839.dp))
        assertTrue(isWide(840.dp))
        assertFalse(isWide(412.dp))
        assertTrue(isWide(1280.dp))
    }

    @Test
    fun theOverviewCarriesTheAnnouncementsThePersonHasNotSeenAndTheOtherItemsAreUnchanged() {
        val items = groupItems(7, GroupPhase.Active, null, 0, announcements = 2)

        val overview = items.first()
        assertEquals(2, overview.count)
        assertEquals("Overview, 2 new announcements", overview.spoken)
        assertEquals(listOf(0, 0), items.drop(1).map { it.count })
    }

    @Test
    fun theOverviewHasNoBadgeForNoNewAnnouncements() {
        val overview = groupItems(7, GroupPhase.Active, null, 0).first()

        assertEquals(0, overview.count)
        assertNull(overview.spoken)
    }

    @Test
    fun aPendingGroupsOverviewCarriesTheCountToo() {
        assertEquals(1, groupItems(7, GroupPhase.Pending, null, 0, announcements = 1).first().count)
        assertEquals("Overview, 1 new announcement", groupItems(7, GroupPhase.Pending, null, 0, announcements = 1).first().spoken)
    }

    @Test
    fun aDrawnGroupHoldsOverviewGivingAndReceiving() {
        val items = groupItems(7, GroupPhase.Active, null, 0)

        assertEquals(listOf("Overview", "Giving", "Receiving"), items.map { it.label })
        assertEquals(listOf(GroupRoute(7), GifterRoute(7), ReceiverRoute(7)), items.map { it.route })
    }

    @Test
    fun anArchivedGroupHoldsTheSameThree() {
        assertEquals(listOf("Overview", "Giving", "Receiving"), groupItems(7, GroupPhase.Archived, null, 0).map { it.label })
    }

    @Test
    fun aPendingGroupHoldsOverviewAndReceivingOnly() {
        assertEquals(listOf("Overview", "Receiving"), groupItems(7, GroupPhase.Pending, null, 0).map { it.label })
    }

    @Test
    fun aGroupInAnyOtherPhaseHoldsOnlyItsOverview() {
        assertEquals(listOf("Overview"), groupItems(7, GroupPhase.Other, null, 0).map { it.label })
    }

    @Test
    fun givingCountsWhatIsNewAndReceivingWhatWaitsForAnAnswer() {
        val items = groupItems(7, GroupPhase.Active, UnreadCounts(wishlist = 2, answers = 1, letters = 3), unanswered = 4)

        assertEquals(listOf(0, 6, 4), items.map { it.count })
        assertEquals("Giving, 6 new", items[1].spoken)
        assertEquals("Receiving, 4 unanswered", items[2].spoken)
        assertNull(items[0].spoken)
    }

    @Test
    fun nothingIsCountedWhileTheCountsAreUnknown() {
        assertEquals(listOf(0, 0, 0), groupItems(7, GroupPhase.Active, null, 0).map { it.count })
    }

    @Test
    fun theThreeColumnPagesBelongToGivingAndReceivingAndTheGroupToItsOverview() {
        val (overview, giving, receiving) = groupItems(7, GroupPhase.Active, null, 0)

        assertTrue(giving.holds(GifterRoute(7)))
        assertTrue(receiving.holds(ReceiverRoute(7)))
        assertTrue(overview.holds(GroupRoute(7)))
        assertFalse(giving.holds(ReceiverRoute(7)))
        assertFalse(receiving.holds(GifterRoute(7)))
        assertFalse(overview.holds(GroupsRoute))
    }

    @Test
    fun aPanesRouteIsNotHeldByGivingOrReceivingBecauseTheSidebarMapsItToTheOverview() {
        val (_, giving, receiving) = groupItems(7, GroupPhase.Active, null, 0)

        for (pane in com.usuallycoding.opensecretsanta.ui.navigation.GroupPane.entries) {
            assertFalse(giving.holds(pane.route(7)), pane.name)
            assertFalse(receiving.holds(pane.route(7)), pane.name)
        }
    }
}
