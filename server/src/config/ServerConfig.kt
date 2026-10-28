package com.usuallycoding.opensecretsanta.server.config

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.security.BcryptPasswordHasher
import java.util.Base64

class ConfigurationException(val variable: String, reason: String) :
    IllegalStateException("Invalid configuration for $variable: $reason")

data class ServerConfig(
    val jwtSecret: String,
    /** Base64 of the 32-byte key used to encrypt retrievable secrets such as temporary passwords. */
    val dataEncryptionKey: String,
    val dbPath: String,
    val port: Int,
    val adminUsername: String = DEFAULT_ADMIN_USERNAME,
    /** Only needed (and only used) when the database has no users yet. */
    val adminPassword: String? = null,
    /** Web origins (`scheme://host[:port]`) allowed to call the API from a browser; empty means no CORS at all. */
    val corsAllowedOrigins: List<String> = emptyList(),
    /** bcrypt work factor; not read from the environment, lowered only by tests. */
    val passwordHashCost: Int = BcryptPasswordHasher.DEFAULT_COST,
    /** The Resend API key; with [emailFrom] it turns on the emails of a draw, and it is never printed. */
    val resendApiKey: String? = null,
    /** The sender of those emails: `name@domain` or `Name <name@domain>`, on a domain verified at Resend. */
    val emailFrom: String? = null,
    /** Where the web app is, for the link in the emails; optional. */
    val appUrl: String? = null,
    /** Where Resend is; only changed to point the server at a stand-in during tests. */
    val resendApiUrl: String? = null,
) {
    fun encryptionKeyBytes(): ByteArray = Base64.getDecoder().decode(dataEncryptionKey.trim())

    /** Email is on only with both an API key and a sender. */
    val emailEnabled: Boolean get() = resendApiKey != null && emailFrom != null

    /** The secrets are never printed, so that the configuration can be logged without leaking them. */
    override fun toString(): String =
        "ServerConfig(dbPath=$dbPath, port=$port, adminUsername=$adminUsername, corsAllowedOrigins=$corsAllowedOrigins, " +
            "emailEnabled=$emailEnabled, emailFrom=$emailFrom, appUrl=$appUrl, secrets=hidden)"

    companion object {
        const val DEFAULT_DB_PATH = "opensecretsanta.db"
        const val DEFAULT_PORT = 8080
        const val DEFAULT_ADMIN_USERNAME = "admin"
        const val ENCRYPTION_KEY_BYTES = 32

        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig = ServerConfig(
            jwtSecret = env["JWT_SECRET"]?.takeIf { it.isNotBlank() }
                ?: throw ConfigurationException("JWT_SECRET", "is required"),
            dataEncryptionKey = parseEncryptionKey(env["DATA_ENCRYPTION_KEY"]),
            dbPath = env["DB_PATH"]?.takeIf { it.isNotBlank() } ?: DEFAULT_DB_PATH,
            port = parsePort(env["PORT"]),
            adminUsername = parseAdminUsername(env["ADMIN_USERNAME"]),
            adminPassword = env["ADMIN_PASSWORD"]?.takeIf { it.isNotBlank() },
            corsAllowedOrigins = parseCorsOrigins(env["CORS_ALLOWED_ORIGINS"]),
            resendApiKey = env["RESEND_API_KEY"]?.takeIf { it.isNotBlank() }?.trim(),
            emailFrom = parseEmailFrom(env["EMAIL_FROM"], keySet = !env["RESEND_API_KEY"].isNullOrBlank()),
            appUrl = parseAppUrl(env["APP_URL"]),
            resendApiUrl = env["RESEND_API_URL"]?.takeIf { it.isNotBlank() }?.trim(),
        )

        private val ORIGIN = Regex("(https?)://([A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?)(:[0-9]{1,5})?")

        private fun parseCorsOrigins(raw: String?): List<String> =
            raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { origin ->
                if (!ORIGIN.matches(origin)) {
                    throw ConfigurationException("CORS_ALLOWED_ORIGINS", "'$origin' is not an origin like https://app.example.org (scheme, host and optional port, no path or wildcard)")
                }
                origin
            }

        private fun parseEncryptionKey(raw: String?): String {
            if (raw.isNullOrBlank()) throw ConfigurationException("DATA_ENCRYPTION_KEY", "is required")
            val bytes = try {
                Base64.getDecoder().decode(raw.trim())
            } catch (_: IllegalArgumentException) {
                throw ConfigurationException("DATA_ENCRYPTION_KEY", "is not valid base64")
            }
            if (bytes.size != ENCRYPTION_KEY_BYTES) {
                throw ConfigurationException("DATA_ENCRYPTION_KEY", "must decode to $ENCRYPTION_KEY_BYTES bytes but decodes to ${bytes.size}")
            }
            return raw.trim()
        }

        private val ADDRESS = "[^@\\s<>]+@[^@\\s<>]+\\.[^@\\s<>]+"
        private val EMAIL_FROM = Regex("($ADDRESS|[^<>\\r\\n]+<$ADDRESS>)")
        private val HTTP_URL = Regex("https?://[^\\s]+")

        /** A sender is needed when there is a key, and must be an address, with or without a display name. */
        private fun parseEmailFrom(raw: String?, keySet: Boolean): String? {
            val from = raw?.takeIf { it.isNotBlank() }?.trim()
            if (from == null) {
                if (keySet) throw ConfigurationException("EMAIL_FROM", "is required when RESEND_API_KEY is set")
                return null
            }
            if (!EMAIL_FROM.matches(from)) {
                throw ConfigurationException("EMAIL_FROM", "'$from' is not an email address like santa@example.org or Santa <santa@example.org>")
            }
            return from
        }

        private fun parseAppUrl(raw: String?): String? {
            val url = raw?.takeIf { it.isNotBlank() }?.trim() ?: return null
            if (!HTTP_URL.matches(url)) throw ConfigurationException("APP_URL", "'$url' is not an http or https address")
            return url
        }

        private fun parseAdminUsername(raw: String?): String {
            if (raw.isNullOrBlank()) return DEFAULT_ADMIN_USERNAME
            return try {
                Username.parse(raw, field = "ADMIN_USERNAME").value
            } catch (e: DomainException.ValidationFailed) {
                throw ConfigurationException("ADMIN_USERNAME", e.message.orEmpty())
            }
        }

        private fun parsePort(raw: String?): Int {
            if (raw.isNullOrBlank()) return DEFAULT_PORT
            val port = raw.trim().toIntOrNull()
                ?: throw ConfigurationException("PORT", "'$raw' is not an integer")
            if (port !in 1..65535) throw ConfigurationException("PORT", "$port is outside 1..65535")
            return port
        }
    }
}
