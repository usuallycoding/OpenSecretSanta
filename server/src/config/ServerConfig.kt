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
) {
    fun encryptionKeyBytes(): ByteArray = Base64.getDecoder().decode(dataEncryptionKey.trim())

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
