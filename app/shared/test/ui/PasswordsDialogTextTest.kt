package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.admin.CreatedAccount
import com.usuallycoding.opensecretsanta.ui.admin.allPasswordsText
import com.usuallycoding.opensecretsanta.ui.admin.offersCopyAll
import com.usuallycoding.opensecretsanta.ui.admin.passwordLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordsDialogTextTest {
    private val bob = CreatedAccount("bob", "Xk3-9fTq")
    private val carol = CreatedAccount("carol", "Pm2-7aZd")

    @Test
    fun aRowIsCopiedAsUsernameAndPassword() {
        assertEquals("bob: Xk3-9fTq", passwordLine(bob))
    }

    @Test
    fun copyAllIsOneLinePerPersonInTheOrderShown() {
        assertEquals("bob: Xk3-9fTq\ncarol: Pm2-7aZd", allPasswordsText(listOf(bob, carol)))
        assertEquals("carol: Pm2-7aZd\nbob: Xk3-9fTq", allPasswordsText(listOf(carol, bob)))
    }

    @Test
    fun oneOrNoPeopleGiveOneLineOrNothing() {
        assertEquals("bob: Xk3-9fTq", allPasswordsText(listOf(bob)))
        assertEquals("", allPasswordsText(emptyList()))
    }

    @Test
    fun copyAllIsOfferedOnlyForTwoOrMore() {
        assertFalse(offersCopyAll(emptyList()))
        assertFalse(offersCopyAll(listOf(bob)))
        assertTrue(offersCopyAll(listOf(bob, carol)))
    }
}
