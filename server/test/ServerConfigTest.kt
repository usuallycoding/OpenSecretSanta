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

    @Test
    fun corsOriginsAreEmptyWhenUnsetOrBlank() {
        assertEquals(emptyList(), ServerConfig.fromEnv(envWith()).corsAllowedOrigins)
        assertEquals(emptyList(), ServerConfig.fromEnv(envWith("CORS_ALLOWED_ORIGINS" to "  ,  ")).corsAllowedOrigins)
    }

    @Test
    fun corsOriginsAreSplitAndTrimmed() {
        val config = ServerConfig.fromEnv(envWith("CORS_ALLOWED_ORIGINS" to " https://app.example.org , http://localhost:8081,https://other.example.org:8443 "))

        assertEquals(
            listOf("https://app.example.org", "http://localhost:8081", "https://other.example.org:8443"),
            config.corsAllowedOrigins,
        )
    }

    @Test
    fun badCorsOriginsNameTheVariable() {
        for (bad in listOf("*", "app.example.org", "https://", "https://app.example.org/", "https://app.example.org/path", "ftp://app.example.org", "https://*.example.org", "https://app.example.org:abc", "https://ok.example.org,*")) {
            assertEquals("CORS_ALLOWED_ORIGINS", failingVariable(envWith("CORS_ALLOWED_ORIGINS" to bad)), bad)
        }
    }

    // --- email ---

    @Test
    fun emailIsOffByDefaultAndOnWithAKeyAndASender() {
        val off = ServerConfig.fromEnv(envWith())
        assertNull(off.resendApiKey)
        assertNull(off.emailFrom)
        assertEquals(false, off.emailEnabled)

        val on = ServerConfig.fromEnv(
            envWith("RESEND_API_KEY" to " re_abc123 ", "EMAIL_FROM" to "Santa <santa@mail.example.org>", "APP_URL" to "https://santa.example.org"),
        )
        assertEquals("re_abc123", on.resendApiKey)
        assertEquals("Santa <santa@mail.example.org>", on.emailFrom)
        assertEquals("https://santa.example.org", on.appUrl)
        assertEquals(true, on.emailEnabled)
    }

    @Test
    fun blankEmailSettingsCountAsUnset() {
        val config = ServerConfig.fromEnv(envWith("RESEND_API_KEY" to " ", "EMAIL_FROM" to "", "APP_URL" to " "))
        assertEquals(false, config.emailEnabled)
        assertNull(config.appUrl)
    }

    @Test
    fun aKeyWithoutASenderNamesEmailFromAndNeverPrintsTheKey() {
        val error = assertFailsWith<ConfigurationException> { ServerConfig.fromEnv(envWith("RESEND_API_KEY" to "re_topsecret")) }

        assertEquals("EMAIL_FROM", error.variable)
        assertEquals(false, "re_topsecret" in error.message.orEmpty())
    }

    @Test
    fun aSenderThatIsNotAnAddressNamesEmailFrom() {
        for (bad in listOf("santa", "santa@", "Santa <santa>", "two words@example.org")) {
            assertEquals("EMAIL_FROM", failingVariable(envWith("RESEND_API_KEY" to "re_x", "EMAIL_FROM" to bad)), bad)
        }
        assertEquals("EMAIL_FROM", failingVariable(envWith("EMAIL_FROM" to "nonsense")))
    }

    @Test
    fun anAppAddressThatIsNotHttpNamesAppUrl() {
        assertEquals("APP_URL", failingVariable(envWith("APP_URL" to "not a url")))
        assertEquals("APP_URL", failingVariable(envWith("APP_URL" to "ftp://example.org")))
    }

    @Test
    fun thePrintedConfigurationHidesEverySecret() {
        val config = ServerConfig.fromEnv(
            envWith("RESEND_API_KEY" to "re_topsecret", "EMAIL_FROM" to "santa@example.org", "ADMIN_PASSWORD" to "first-pass-secret"),
        )

        val printed = config.toString()

        for (secret in listOf("re_topsecret", "first-pass-secret", "s3cret", TEST_ENCRYPTION_KEY)) {
            assertEquals(false, secret in printed, secret)
        }
        assertEquals(true, "santa@example.org" in printed)
    }
    // --- push ---

    @Test
    fun pushIsOffByDefaultAndOnWithAServiceAccountKey() {
        val off = ServerConfig.fromEnv(envWith())
        assertNull(off.fcm)
        assertEquals(false, off.pushEnabled)

        val (file, _) = writeServiceAccountKey(projectId = "my-santa")
        val on = ServerConfig.fromEnv(envWith("FCM_SERVICE_ACCOUNT_FILE" to " $file "))
        assertEquals(true, on.pushEnabled)
        assertEquals("my-santa", on.fcm?.projectId)
        assertEquals("push@my-santa.iam.gserviceaccount.com", on.fcm?.clientEmail)
    }

    @Test
    fun aBlankKeyFileCountsAsUnset() {
        assertEquals(false, ServerConfig.fromEnv(envWith("FCM_SERVICE_ACCOUNT_FILE" to "  ")).pushEnabled)
    }

    @Test
    fun aKeyFileThatIsMissingOrWrongNamesTheVariable() {
        val dir = java.nio.file.Files.createTempDirectory("santa-fcm-bad")
        val notJson = dir.resolve("a.json").also { java.nio.file.Files.writeString(it, "hello") }
        val noKey = dir.resolve("b.json").also { java.nio.file.Files.writeString(it, """{"type":"service_account","project_id":"p"}""") }
        val wrongType = writeServiceAccountKey(type = "authorized_user").first
        val badKey = dir.resolve("c.json").also {
            java.nio.file.Files.writeString(it, """{"type":"service_account","project_id":"p","client_email":"e@p","token_uri":"https://x","private_key":"-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----"}""")
        }
        val plainHttp = writeServiceAccountKey(tokenUri = "http://evil.example.org/token").first

        for (bad in listOf(dir.resolve("missing.json"), notJson, noKey, wrongType, badKey, plainHttp)) {
            assertEquals("FCM_SERVICE_ACCOUNT_FILE", failingVariable(envWith("FCM_SERVICE_ACCOUNT_FILE" to bad.toString())), bad.toString())
        }
    }

    @Test
    fun theKeyIsNeverPrinted() {
        val (file, pair) = writeServiceAccountKey()
        val config = ServerConfig.fromEnv(envWith("FCM_SERVICE_ACCOUNT_FILE" to file.toString()))

        val everything = config.toString() + config.fcm.toString()

        assertEquals(false, Base64.getEncoder().encodeToString(pair.private.encoded).take(40) in everything)
        assertEquals(false, "PRIVATE KEY" in everything)
        assertEquals(true, "pushEnabled=true" in everything)
    }

}
