package com.usuallycoding.opensecretsanta

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformWasmJsTest {
    @Test
    fun `platform name is reported`() {
        assertTrue(getPlatform().name.isNotBlank())
    }

    @Test
    fun `the web has no pull to refresh because the browser refreshes the page`() {
        assertFalse(getPlatform().hasPullToRefresh)
    }
}
