package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.navigation.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TabScreensTest {
    @Test
    fun theScreensTheTabBarsOpenAreTabs() {
        for (route in listOf(GroupsRoute, SettingsRoute, AdminGroupsRoute, AdminUsersRoute)) {
            assertEquals(true, TabScreens.isTab(route), route.toString())
        }
    }

    @Test
    fun everyOtherScreenIsNot() {
        val others = listOf(
            GroupRoute(2), GifterRoute(2), ReceiverRoute(2), GifterWishlistRoute(2), GifterQuestionsRoute(2), ReceiverWishlistRoute(2), ReceiverQuestionsRoute(2), InboxRoute(2), SendLetterRoute(2), AnnouncementsRoute(2), DeliveryInfoRoute(2), AdminGroupNewRoute, AdminGroupRoute(2), AdminGroupEditRoute(2), AdminUserRoute("alice"),
        )
        for (route in others) assertEquals(false, TabScreens.isTab(route), route.toString())
    }

    @Test
    fun everyAddressOfATabScreenIsATab() {
        val tabAddresses = listOf("#/groups", "#/settings", "#/admin/groups", "#/admin/users")
        for (address in tabAddresses) assertEquals(true, TabScreens.isTab(WebAddresses.routeOf(address, isAdmin = true)!!), address)
    }
}
