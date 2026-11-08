package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.NotificationData
import com.usuallycoding.opensecretsanta.ui.navigation.AnnouncementsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GifterRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.InboxRoute
import com.usuallycoding.opensecretsanta.ui.navigation.NotificationTarget
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverRoute
import com.usuallycoding.opensecretsanta.ui.navigation.WebAddresses
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationTargetTest {
    @Test
    fun aQuestionFromTheSantaOpensTheReceiversQuestions() {
        assertEquals(listOf(GroupRoute(7), ReceiverRoute(7, questions = true)), NotificationTarget.routesOf("questions", 7))
    }

    @Test
    fun anAnswerFromTheReceiverOpensTheGiftersQuestions() {
        assertEquals(listOf(GroupRoute(7), GifterRoute(7, questions = true)), NotificationTarget.routesOf("answers", 7))
    }

    @Test
    fun aWishlistChangeOpensTheGiftersWishlist() {
        assertEquals(listOf(GroupRoute(7), GifterRoute(7)), NotificationTarget.routesOf("wishlist", 7))
    }

    @Test
    fun aLetterFromTheReceiverOpensTheInboxOverTheGroup() {
        assertEquals(listOf(GroupRoute(7), InboxRoute(7)), NotificationTarget.routesOf("letters", 7))
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
        assertEquals(listOf(GroupRoute(7), InboxRoute(7)), NotificationTarget.routesOf("letters", 7, target = NotificationData.TARGET_MESSAGES))
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
        assertEquals(NotificationTarget.routesOf("questions", 7).last(), WebAddresses.routeOf("#/groups/7/receiver/questions", false))
        assertEquals(NotificationTarget.routesOf("answers", 7).last(), WebAddresses.routeOf("#/groups/7/giver/questions", false))
        assertEquals(NotificationTarget.routesOf("wishlist", 7).last(), WebAddresses.routeOf("#/groups/7/giver", false))
        assertEquals(NotificationTarget.routesOf("letters", 7).last(), WebAddresses.routeOf("#/groups/7/inbox", false))
        assertEquals(NotificationTarget.routesOf("announcements", 7, NotificationData.TARGET_MESSAGES).last(), WebAddresses.routeOf("#/groups/7/notifications", false))
    }
}
