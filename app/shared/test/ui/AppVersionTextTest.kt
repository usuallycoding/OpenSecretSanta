package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.APP_VERSION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppVersionTextTest {
    @Test
    fun `the label is Version and the number`() {
        assertEquals("Version 1.0.0", Strings.version("1.0.0"))
    }

    @Test
    fun `the app has a version`() {
        assertTrue(APP_VERSION.isNotBlank())
    }
}
