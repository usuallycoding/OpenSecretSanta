package com.usuallycoding.opensecretsanta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformIosTest {
    @Test
    fun `platform name is reported`() {
        assertTrue(getPlatform().name.isNotBlank())
    }

    @Test
    fun `the ios app does not offer the android app for download`() {
        assertFalse(getPlatform().offersAndroidDownload)
    }

    @Test
    fun theUpdateWayOfThePlatform() {
        assertEquals(UpdateWay.None, getPlatform().updateWay)
    }
}
