package com.usuallycoding.opensecretsanta.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PageTitlesTest {
    @Test
    fun theGifterPageIsNamedAfterTheReceiver() {
        assertEquals("Giving to bob", Strings.givingTo("bob"))
        assertEquals("bob's Wishlist", Strings.receiverWishlistTab("bob"))
        assertEquals("Questions for bob", Strings.receiverQuestionsTab("bob"))
    }

    @Test
    fun theGifterPageFallsBackUntilTheReceiverIsKnown() {
        assertEquals("Giving to ...", Strings.givingTo(null))
        assertEquals("Wishlist", Strings.receiverWishlistTab(null))
        assertEquals("Questions", Strings.receiverQuestionsTab(null))
    }

    @Test
    fun aLongUsernameIsKeptWhole() {
        val name = "a".repeat(32)

        assertEquals("Giving to $name", Strings.givingTo(name))
        assertEquals("Questions for $name", Strings.receiverQuestionsTab(name))
    }

    @Test
    fun theReceiverPageNamesTheRoleNotAPerson() {
        assertEquals("Receiving from Secret Santa", Strings.TITLE_RECEIVING)
        assertEquals("Your Wishlist", Strings.TITLE_YOUR_WISHLIST)
        assertEquals("Your Wishlist", Strings.TAB_YOUR_WISHLIST)
        assertEquals("Respond to Secret Santa", Strings.TAB_RESPOND)
    }

    @Test
    fun thePairLinksKeepTheirShortLabels() {
        assertEquals("Gifter", Strings.TITLE_GIFTER)
        assertEquals("Receiver", Strings.TITLE_RECEIVER)
    }
}
