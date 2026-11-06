package com.usuallycoding.opensecretsanta

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationAnswerTest {
    private val android12 = 32
    private val android13 = 33

    @Test
    fun aGrantedPermissionIsGrantedWhateverElseIsTrue() {
        assertEquals(PermissionAnswer.Granted, notificationAnswer(android13, granted = true, canAskAgain = false))
        assertEquals(PermissionAnswer.Granted, notificationAnswer(android12, granted = true, canAskAgain = false))
    }

    @Test
    fun aRefusalTheSystemWillAskAboutAgainIsOnlyDenied() {
        assertEquals(PermissionAnswer.Denied, notificationAnswer(android13, granted = false, canAskAgain = true))
    }

    @Test
    fun aRefusalTheSystemWillNotAskAboutAgainIsBlocked() {
        assertEquals(PermissionAnswer.Blocked, notificationAnswer(android13, granted = false, canAskAgain = false))
        assertEquals(PermissionAnswer.Blocked, notificationAnswer(34, granted = false, canAskAgain = false))
    }

    @Test
    fun beforeAndroid13NothingIsAskedSoAnUngrantedPermissionIsBlocked() {
        assertEquals(PermissionAnswer.Blocked, notificationAnswer(android12, granted = false, canAskAgain = true))
        assertEquals(PermissionAnswer.Blocked, notificationAnswer(android12, granted = false, canAskAgain = false))
    }
}
