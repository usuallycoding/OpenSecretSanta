package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.admin.AdminPair
import com.usuallycoding.opensecretsanta.ui.admin.AdminPhase
import com.usuallycoding.opensecretsanta.ui.admin.DrawState
import com.usuallycoding.opensecretsanta.ui.navigation.AdminAnnouncementsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupPairRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupRulesRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminPane
import com.usuallycoding.opensecretsanta.ui.navigation.WebAddresses
import com.usuallycoding.opensecretsanta.ui.navigation.adminGroupIdOfPane
import com.usuallycoding.opensecretsanta.ui.navigation.adminPaneOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AdminPaneTest {
    private val panes = listOf(AdminPane.Rules, AdminPane.Pair("alice", "bob"), AdminPane.Announcements)
    private val draw = DrawState.Pairs(listOf(AdminPair("alice", "bob", santaIsMe = false, receiverIsMe = false)))

    @Test
    fun everyPaneHasARouteThatNamesItAndItsGroupAndAnAddress() {
        for (pane in panes) {
            val route = pane.route(7)
            assertEquals(pane, adminPaneOf(route), pane.toString())
            assertEquals(7L, adminGroupIdOfPane(route), pane.toString())
            assertNotNull(WebAddresses.addressOf(route), pane.toString())
        }
    }

    @Test
    fun theRoutesAreTheOnesTheAddressesNameAndAnythingElseIsNoPane() {
        assertEquals(AdminGroupRulesRoute(7), AdminPane.Rules.route(7))
        assertEquals(AdminGroupPairRoute(7, "alice", "bob"), AdminPane.Pair("alice", "bob").route(7))
        assertEquals(AdminAnnouncementsRoute(7), AdminPane.Announcements.route(7))
        assertNull(adminPaneOf(AdminGroupRoute(7)))
        assertNull(adminGroupIdOfPane("not a route"))
    }

    @Test
    fun theRulesAndTheAnnouncementsAreThereFromPendingOn() {
        for (pane in listOf(AdminPane.Rules, AdminPane.Announcements)) {
            assertEquals(false, pane.within(AdminPhase.Draft, DrawState.None), "$pane draft")
            assertEquals(false, pane.within(AdminPhase.Other, DrawState.None), "$pane other")
            for (phase in listOf(AdminPhase.Pending, AdminPhase.Active, AdminPhase.Archived)) {
                assertEquals(true, pane.within(phase, DrawState.None), "$pane $phase")
            }
        }
    }

    @Test
    fun nothingIsKnownBeforeThePhase() {
        for (pane in panes) assertNull(pane.within(null, DrawState.None), pane.toString())
    }

    @Test
    fun aPairIsThereOnlyInADrawnGroupAndOnlyIfItIsOneOfTheDrawsPairs() {
        val pair = AdminPane.Pair("alice", "bob")
        assertEquals(false, pair.within(AdminPhase.Draft, DrawState.None))
        assertEquals(false, pair.within(AdminPhase.Pending, DrawState.None))
        for (phase in listOf(AdminPhase.Active, AdminPhase.Archived)) {
            assertEquals(true, pair.within(phase, draw), "$phase")
            assertEquals(false, AdminPane.Pair("alice", "carol").within(phase, draw), "$phase other receiver")
            assertEquals(false, AdminPane.Pair("bob", "alice").within(phase, draw), "$phase reversed")
        }
    }

    @Test
    fun aPairWaitsForTheDrawAndStaysWhenTheDrawCouldNotBeRead() {
        val pair = AdminPane.Pair("alice", "bob")
        assertNull(pair.within(AdminPhase.Active, DrawState.None))
        assertEquals(true, pair.within(AdminPhase.Active, DrawState.Failed))
    }
}
