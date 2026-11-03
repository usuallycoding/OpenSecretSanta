package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.navigation.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TabScreensTest {
    @Test
    fun theScreensTheTabBarsOpenAreTabs() {
        for (route in listOf(GroupsRoute, ProfileRoute, SettingsRoute, AdminGroupsRoute, AdminUsersRoute)) {
            assertEquals(true, TabScreens.isTab(route), route.toString())
        }
    }

    @Test
    fun everyOtherScreenIsNot() {
        val others = listOf(
            GroupRoute(2), GifterRoute(2), ReceiverRoute(2, questions = true), AdminGroupNewRoute, AdminGroupRoute(2), AdminGroupEditRoute(2), AdminUserRoute("alice"),
        )
        for (route in others) assertEquals(false, TabScreens.isTab(route), route.toString())
    }

    @Test
    fun everyAddressOfATabScreenIsATab() {
        val tabAddresses = listOf("#/groups", "#/profile", "#/settings", "#/admin/groups", "#/admin/users")
        for (address in tabAddresses) assertEquals(true, TabScreens.isTab(WebAddresses.routeOf(address, isAdmin = true)!!), address)
    }
}
