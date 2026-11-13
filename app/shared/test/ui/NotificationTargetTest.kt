package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.NotificationData
import com.usuallycoding.opensecretsanta.ui.navigation.AnnouncementsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.NotificationTarget
import com.usuallycoding.opensecretsanta.ui.navigation.WebAddresses
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationTargetTest {
    @Test
    fun theDailyDigestOpensTheGroupListAndNothingElse() {
        assertEquals(emptyList(), NotificationTarget.routesOf("daily", null))
        assertEquals(emptyList(), NotificationTarget.routesOf(NotificationData.CATEGORY_DAILY, 7))
    }

    @Test
    fun aNotificationWithNoGroupOpensTheGroupList() {
        assertEquals(emptyList(), NotificationTarget.routesOf("announcements", null))
        assertEquals(emptyList(), NotificationTarget.routesOf(null, null))
    }

    @Test
    fun beingAddedToAGroupAndTheDrawOpenTheGroup() {
        assertEquals(listOf(GroupRoute(7)), NotificationTarget.routesOf("announcements", 7))
        assertEquals(listOf(GroupRoute(7)), NotificationTarget.routesOf("announcements", 7, target = null))
        assertEquals(listOf(GroupRoute(7)), NotificationTarget.routesOf("announcements", 7, target = "something-new"))
    }

    @Test
    fun anAnnouncementTheOwnerSentOpensTheAnnouncementsOverTheGroup() {
        assertEquals(
            listOf(GroupRoute(7), AnnouncementsRoute(7)),
            NotificationTarget.routesOf("announcements", 7, target = NotificationData.TARGET_MESSAGES),
        )
    }

    @Test
    fun aTargetOnAnotherKindChangesNothing() {
        assertEquals(listOf(GroupRoute(7)), NotificationTarget.routesOf("something-new", 7, target = NotificationData.TARGET_MESSAGES))
    }

    @Test
    fun theOldKindsOfNotificationNowOpenTheGroup() {
        for (kind in listOf("questions", "answers", "wishlist", "letters")) {
            assertEquals(listOf(GroupRoute(7)), NotificationTarget.routesOf(kind, 7), kind)
        }
    }

    @Test
    fun anUnknownOrMissingKindOpensTheGroup() {
        assertEquals(listOf(GroupRoute(7)), NotificationTarget.routesOf("something-new", 7))
        assertEquals(listOf(GroupRoute(7)), NotificationTarget.routesOf(null, 7))
    }

    // the web service worker has its own copy of this mapping; these are the addresses it builds
    @Test
    fun theWebAddressesTheServiceWorkerBuildsOpenTheSameScreens() {
        assertEquals(GroupRoute(7), WebAddresses.routeOf("#/groups/7", false))
        assertEquals(NotificationTarget.routesOf("announcements", 7, NotificationData.TARGET_MESSAGES).last(), WebAddresses.routeOf("#/groups/7/announcements", false))
    }
}
