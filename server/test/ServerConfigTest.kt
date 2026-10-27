package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.config.ConfigurationException
import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerConfigTest {

    @Test
    fun defaultsAreApplied() {
        val config = ServerConfig.fromEnv(mapOf("JWT_SECRET" to "s3cret"))
        assertEquals(ServerConfig("s3cret", "opensecretsanta.db", 8080), config)
    }

    @Test
    fun overridesAreHonored() {
        val config = ServerConfig.fromEnv(
            mapOf("JWT_SECRET" to "s3cret", "PORT" to "9000", "DB_PATH" to "/data/santa.db"),
        )
        assertEquals(ServerConfig("s3cret", "/data/santa.db", 9000), config)
    }

    @Test
    fun missingSecretNamesVariable() {
        val error = assertFailsWith<ConfigurationException> { ServerConfig.fromEnv(emptyMap()) }
        assertEquals("JWT_SECRET", error.variable)
    }

    @Test
    fun blankSecretNamesVariable() {
        val error = assertFailsWith<ConfigurationException> {
            ServerConfig.fromEnv(mapOf("JWT_SECRET" to "   "))
        }
        assertEquals("JWT_SECRET", error.variable)
    }

    @Test
    fun nonNumericPortNamesVariable() {
        val error = assertFailsWith<ConfigurationException> {
            ServerConfig.fromEnv(mapOf("JWT_SECRET" to "s", "PORT" to "abc"))
        }
        assertEquals("PORT", error.variable)
    }

    @Test
    fun outOfRangePortIsRejected() {
        for (port in listOf("0", "65536", "-1")) {
            val error = assertFailsWith<ConfigurationException> {
                ServerConfig.fromEnv(mapOf("JWT_SECRET" to "s", "PORT" to port))
            }
            assertEquals("PORT", error.variable)
        }
    }
}
