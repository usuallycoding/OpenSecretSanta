package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.navigation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebAddressesTest {
    private val addresses = listOf(
        GroupsRoute to "#/groups",
        GroupRoute(2) to "#/groups/2",
        GifterRoute(2) to "#/groups/2/gifter",
        ReceiverRoute(2) to "#/groups/2/receiver",
        ProfileRoute to "#/profile",
        AdminGroupsRoute to "#/admin/groups",
        AdminGroupNewRoute to "#/admin/groups/new",
        AdminGroupRoute(2) to "#/admin/groups/2",
        AdminGroupEditRoute(2) to "#/admin/groups/2/edit",
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
    fun aTrailingSlashAndAMissingHashAreTolerated() {
        assertEquals(GroupRoute(2), WebAddresses.routeOf("#/groups/2/", false))
        assertEquals(ProfileRoute, WebAddresses.routeOf("/profile", false))
    }

    @Test
    fun unknownAndOldFormAddressesNameNothing() {
        for (a in listOf("", "#", "#/", "#/nope", "#/groups/x", "#/groups/2/other", "#/groups/2/wishlist", "#/groups/2/receiver/wishlist", "#/groups/2/questions", "#/groups/2/santa", "#/profile/x", "#/admin", "#/admin/users/Al ice",
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
}
