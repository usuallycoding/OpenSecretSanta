package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.config.ConfigurationException
import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ServerConfigTest {

    @Test
    fun defaultsAreApplied() {
        val config = ServerConfig.fromEnv(envWith())
        assertEquals(
            ServerConfig("s3cret", TEST_ENCRYPTION_KEY, "opensecretsanta.db", 8080, adminUsername = "admin", adminPassword = null),
            config,
        )
    }

    @Test
    fun overridesAreHonored() {
        val config = ServerConfig.fromEnv(
            envWith("PORT" to "9000", "DB_PATH" to "/data/santa.db", "ADMIN_USERNAME" to "Boss", "ADMIN_PASSWORD" to "first-pass"),
        )
        assertEquals(
            ServerConfig("s3cret", TEST_ENCRYPTION_KEY, "/data/santa.db", 9000, adminUsername = "boss", adminPassword = "first-pass"),
            config,
        )
    }

    @Test
    fun blankAdminPasswordCountsAsMissing() {
        assertNull(ServerConfig.fromEnv(envWith("ADMIN_PASSWORD" to "  ")).adminPassword)
    }

    @Test
    fun encryptionKeyDecodesToItsBytes() {
        assertContentEquals(ByteArray(32) { it.toByte() }, ServerConfig.fromEnv(envWith()).encryptionKeyBytes())
    }

    private fun failingVariable(env: Map<String, String>): String =
        assertFailsWith<ConfigurationException> { ServerConfig.fromEnv(env) }.variable

    @Test
    fun missingSecretNamesVariable() {
        assertEquals("JWT_SECRET", failingVariable(mapOf("DATA_ENCRYPTION_KEY" to TEST_ENCRYPTION_KEY)))
    }

    @Test
    fun blankSecretNamesVariable() {
        assertEquals("JWT_SECRET", failingVariable(envWith("JWT_SECRET" to "   ")))
    }

    @Test
    fun missingEncryptionKeyNamesVariable() {
        assertEquals("DATA_ENCRYPTION_KEY", failingVariable(mapOf("JWT_SECRET" to "s")))
    }

    @Test
    fun malformedEncryptionKeyNamesVariable() {
        assertEquals("DATA_ENCRYPTION_KEY", failingVariable(envWith("DATA_ENCRYPTION_KEY" to "not base64 !!")))
    }

    @Test
    fun wrongLengthEncryptionKeyNamesVariable() {
        for (size in listOf(16, 31, 33, 64)) {
            val key = Base64.getEncoder().encodeToString(ByteArray(size))
            assertEquals("DATA_ENCRYPTION_KEY", failingVariable(envWith("DATA_ENCRYPTION_KEY" to key)), "size $size")
        }
    }

    @Test
    fun invalidAdminUsernameNamesVariable() {
        assertEquals("ADMIN_USERNAME", failingVariable(envWith("ADMIN_USERNAME" to "a b")))
    }

    @Test
    fun nonNumericPortNamesVariable() {
        assertEquals("PORT", failingVariable(envWith("PORT" to "abc")))
    }

    @Test
    fun outOfRangePortIsRejected() {
        for (port in listOf("0", "65536", "-1")) {
            assertEquals("PORT", failingVariable(envWith("PORT" to port)), port)
        }
    }
}
