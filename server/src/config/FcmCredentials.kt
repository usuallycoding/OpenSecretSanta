package com.usuallycoding.opensecretsanta.server.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * The parts of a Firebase service-account key file that the server needs to send push messages. The private key never
 * appears in [toString], so that the configuration can be logged.
 */
class FcmCredentials(
    val projectId: String,
    val clientEmail: String,
    val tokenUri: String,
    val privateKey: PrivateKey,
) {
    override fun toString(): String = "FcmCredentials(projectId=$projectId, clientEmail=$clientEmail, privateKey=hidden)"

    companion object {
        const val VARIABLE = "FCM_SERVICE_ACCOUNT_FILE"

        /** Reads the key file at [path]; a missing, unreadable or wrong file stops the server naming [VARIABLE]. */
        fun load(path: String): FcmCredentials {
            val text = try {
                File(path).readText()
            } catch (_: IOException) {
                throw ConfigurationException(VARIABLE, "'$path' cannot be read")
            }
            val json = try {
                Json.parseToJsonElement(text).jsonObject
            } catch (_: Exception) {
                throw ConfigurationException(VARIABLE, "'$path' is not a JSON file")
            }
            if (json.text("type") != "service_account") {
                throw ConfigurationException(VARIABLE, "'$path' is not a service-account key (its type is not service_account)")
            }
            fun required(name: String) = json.text(name) ?: throw ConfigurationException(VARIABLE, "'$path' has no $name")
            val tokenUri = required("token_uri")
            if (!tokenUri.startsWith("https://") && !tokenUri.startsWith("http://localhost") && !tokenUri.startsWith("http://127.0.0.1")) {
                throw ConfigurationException(VARIABLE, "the token_uri in '$path' is not an https address")
            }
            return FcmCredentials(
                projectId = required("project_id"),
                clientEmail = required("client_email"),
                tokenUri = tokenUri,
                privateKey = parsePrivateKey(required("private_key"), path),
            )
        }

        private fun JsonObject.text(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

        private fun parsePrivateKey(pem: String, path: String): PrivateKey {
            val body = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").filterNot { it.isWhitespace() }
            return try {
                KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)))
            } catch (_: GeneralSecurityException) {
                throw ConfigurationException(VARIABLE, "the private_key in '$path' is not an RSA key")
            } catch (_: IllegalArgumentException) {
                throw ConfigurationException(VARIABLE, "the private_key in '$path' is not an RSA key")
            }
        }
    }
}
