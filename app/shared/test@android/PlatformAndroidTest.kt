package com.usuallycoding.opensecretsanta

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformAndroidTest {
    @Test
    fun `platform name is reported`() {
        assertTrue(getPlatform().name.isNotBlank())
    }

    @Test
    fun `android refreshes a screen by pulling it down`() {
        assertTrue(getPlatform().hasPullToRefresh)
    }
}
