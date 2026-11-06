package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.navigation.GifterRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.LinkTarget
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkTargetTest {
    @Test
    fun theLinkInAnEmailOpensTheGroup() {
        assertEquals(listOf(GroupRoute(7)), LinkTarget.routesOf("/groups/7"))
        assertEquals(listOf(GroupRoute(7)), LinkTarget.routesOf("#/groups/7"))
        assertEquals(listOf(GroupRoute(7)), LinkTarget.routesOf("/groups/7/"))
    }

    @Test
    fun aGifterOrReceiverPageOpensWithTheGroupBehindIt() {
        assertEquals(listOf(GroupRoute(7), GifterRoute(7)), LinkTarget.routesOf("/groups/7/giver"))
        assertEquals(listOf(GroupRoute(7), GifterRoute(7, questions = true)), LinkTarget.routesOf("/groups/7/giver/questions"))
        assertEquals(listOf(GroupRoute(7), ReceiverRoute(7)), LinkTarget.routesOf("/groups/7/receiver"))
        assertEquals(listOf(GroupRoute(7), ReceiverRoute(7, questions = true)), LinkTarget.routesOf("/groups/7/receiver/questions"))
    }

    @Test
    fun theFormerGifterAddressOpensNothing() {
        assertNull(LinkTarget.routesOf("/groups/7/gifter"))
        assertNull(LinkTarget.routesOf("/groups/7/gifter/questions"))
    }

    @Test
    fun theSameScreensAsANotificationOpens() {
        assertEquals(com.usuallycoding.opensecretsanta.ui.navigation.NotificationTarget.routesOf("questions", 7), LinkTarget.routesOf("/groups/7/receiver/questions"))
        assertEquals(com.usuallycoding.opensecretsanta.ui.navigation.NotificationTarget.routesOf("announcements", 7), LinkTarget.routesOf("/groups/7"))
    }

    @Test
    fun anythingButAGroupScreenOpensNothing() {
        assertNull(LinkTarget.routesOf("/admin/users/alice"))
        assertNull(LinkTarget.routesOf("/admin/groups/2"))
        assertNull(LinkTarget.routesOf("/groups")) // the list is the first screen anyway
        assertNull(LinkTarget.routesOf("/profile"))
        assertNull(LinkTarget.routesOf("/settings"))
        assertNull(LinkTarget.routesOf("/groups/abc"))
        assertNull(LinkTarget.routesOf("/groups/7/wishlist"))
    }

    @Test
    fun noAddressOpensNothing() {
        assertNull(LinkTarget.routesOf(null))
        assertNull(LinkTarget.routesOf(""))
        assertNull(LinkTarget.routesOf("   "))
    }
}
