package com.usuallycoding.opensecretsanta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ServerAddressTest {
    @Test
    fun aReleaseBuildAlwaysUsesTheRealServer() {
        assertEquals("https://santa-api.plm360.org", serverAddress(debuggable = false))
    }

    @Test
    fun aDebuggableBuildKeepsTheDevelopmentServer() {
        assertEquals(DEVELOPMENT_SERVER, serverAddress(debuggable = true))
        assertNotEquals(PRODUCTION_SERVER, serverAddress(debuggable = true))
    }

    @Test
    fun theRealServerIsReachedOverHttpsOnly() {
        assertTrue(PRODUCTION_SERVER.startsWith("https://"))
    }
}
