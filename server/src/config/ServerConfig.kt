package com.usuallycoding.opensecretsanta.server.config

class ConfigurationException(val variable: String, reason: String) :
    IllegalStateException("Invalid configuration for $variable: $reason")

data class ServerConfig(
    val jwtSecret: String,
    val dbPath: String,
    val port: Int,
) {
    companion object {
        const val DEFAULT_DB_PATH = "opensecretsanta.db"
        const val DEFAULT_PORT = 8080

        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig = ServerConfig(
            jwtSecret = env["JWT_SECRET"]?.takeIf { it.isNotBlank() }
                ?: throw ConfigurationException("JWT_SECRET", "is required"),
            dbPath = env["DB_PATH"]?.takeIf { it.isNotBlank() } ?: DEFAULT_DB_PATH,
            port = parsePort(env["PORT"]),
        )

        private fun parsePort(raw: String?): Int {
            if (raw.isNullOrBlank()) return DEFAULT_PORT
            val port = raw.trim().toIntOrNull()
                ?: throw ConfigurationException("PORT", "'$raw' is not an integer")
            if (port !in 1..65535) throw ConfigurationException("PORT", "$port is outside 1..65535")
            return port
        }
    }
}
