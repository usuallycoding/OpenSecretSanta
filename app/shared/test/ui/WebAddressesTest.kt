package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.navigation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebAddressesTest {
    private val addresses = listOf(
        GroupsRoute to "#/groups",
        GroupRoute(2) to "#/groups/2",
        GifterRoute(2) to "#/groups/2/giver",
        ReceiverRoute(2) to "#/groups/2/receiver",
        GifterWishlistRoute(2) to "#/groups/2/giver/wishlist",
        GifterQuestionsRoute(2) to "#/groups/2/giver/questions",
        ReceiverWishlistRoute(2) to "#/groups/2/receiver/wishlist",
        ReceiverQuestionsRoute(2) to "#/groups/2/receiver/questions",
        DeliveryInfoRoute(2) to "#/groups/2/delivery-info",
        SendLetterRoute(2) to "#/groups/2/outbox",
        InboxRoute(2) to "#/groups/2/inbox",
        AnnouncementsRoute(2) to "#/groups/2/announcements",
        SettingsRoute to "#/settings",
        AdminGroupsRoute to "#/admin/groups",
        AdminGroupNewRoute to "#/admin/groups/new",
        AdminGroupRoute(2) to "#/admin/groups/2",
        AdminGroupEditRoute(2) to "#/admin/groups/2/edit",
        AdminGroupRulesRoute(2) to "#/admin/groups/2/rules",
        AdminAnnouncementsRoute(2) to "#/admin/groups/2/announcements",
        AdminUsersRoute to "#/admin/users",
        AdminUserRoute("alice.b_c-1") to "#/admin/users/alice.b_c-1",
    )

    @Test
    fun everyRouteHasExactlyTheDesignedAddressAndRoundTrips() {
        for ((route, address) in addresses) {
            assertEquals(address, WebAddresses.addressOf(route))
            assertEquals(route, WebAddresses.routeOf(address, isAdmin = true))
        }
    }

    @Test
    fun everyPaneHasItsAddressAndEveryPaneRouteIsAnAddress() {
        for (pane in GroupPane.entries) {
            val route = pane.route(9)
            assertEquals("#/groups/9/${pane.segment}", WebAddresses.addressOf(route), pane.name)
            assertEquals(route, WebAddresses.routeOf("#/groups/9/${pane.segment}", isAdmin = false), pane.name)
        }
        assertEquals(7, GroupPane.entries.size)
    }

    @Test
    fun theOldQuestionsAddressesAreThePagesNow() {
        assertEquals(GifterQuestionsRoute(2), WebAddresses.routeOf("#/groups/2/giver/questions", false))
        assertEquals(ReceiverQuestionsRoute(2), WebAddresses.routeOf("#/groups/2/receiver/questions", false))
        assertEquals(GifterRoute(2), WebAddresses.routeOf("#/groups/2/giver", false))
        assertEquals(ReceiverRoute(2), WebAddresses.routeOf("#/groups/2/receiver", false))
    }

    @Test
    fun aTrailingSlashAndAMissingHashAreTolerated() {
        assertEquals(GroupRoute(2), WebAddresses.routeOf("#/groups/2/", false))
        assertEquals(DeliveryInfoRoute(2), WebAddresses.routeOf("/groups/2/delivery-info/", false))
        assertEquals(AnnouncementsRoute(2), WebAddresses.routeOf("#/groups/2/announcements/", false))
        assertEquals(ReceiverQuestionsRoute(2), WebAddresses.routeOf("/groups/2/receiver/questions/", false))
        assertEquals(AdminAnnouncementsRoute(2), WebAddresses.routeOf("/admin/groups/2/announcements/", true))
        assertNull(WebAddresses.routeOf("#/profile", false)) // the Profile tab is gone, so its address names no screen
    }

    @Test
    fun unknownAndOldFormAddressesNameNothing() {
        for (a in listOf("", "#", "#/", "#/nope", "#/groups/x", "#/groups/2/other", "#/groups/2/wishlist", "#/groups/2/giver/questions/extra", "#/groups/2/receiver/questions/extra", "#/groups/2/questions", "#/groups/2/santa", "#/groups/2/outbox/extra", "#/groups/2/inbox/extra", "#/groups/2/announcements/extra", "#/admin/groups/2/announcements/extra", "#/groups/2/send-letter", "#/groups/2/gifter", "#/groups/2/gifter/questions", "#/groups/2/letters", "#/profile/x", "#/settings/extra", "#/admin", "#/admin/users/Al ice",
            "#com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute/2")) {
            assertNull(WebAddresses.routeOf(a, true), a)
        }
    }

    @Test
    fun adminAddressesNameNothingToANonAdministrator() {
        for ((route, address) in addresses) {
            if (address.startsWith("#/admin")) assertNull(WebAddresses.routeOf(address, false), address)
            else assertEquals(route, WebAddresses.routeOf(address, false))
        }
    }
    @Test
    fun theSettingsAddressIsForEveryoneAndToleratesASlash() {
        assertEquals(SettingsRoute, WebAddresses.routeOf("#/settings", isAdmin = false))
        assertEquals(SettingsRoute, WebAddresses.routeOf("#/settings", isAdmin = true))
        assertEquals(SettingsRoute, WebAddresses.routeOf("#/settings/", isAdmin = true))
    }

}
