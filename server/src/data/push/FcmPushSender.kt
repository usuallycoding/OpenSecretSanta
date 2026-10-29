package com.usuallycoding.opensecretsanta.server.data.push

import com.usuallycoding.opensecretsanta.contract.NotificationData
import com.usuallycoding.opensecretsanta.domain.notification.PushMessage
import com.usuallycoding.opensecretsanta.domain.notification.PushResult
import com.usuallycoding.opensecretsanta.domain.notification.PushSender
import com.usuallycoding.opensecretsanta.server.config.FcmCredentials
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.Signature
import java.time.Duration
import java.util.Base64

/**
 * Sends push messages through Firebase Cloud Messaging's HTTP v1 API (`https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages`),
 * one request per message. It signs a short-lived JWT with the service account's key, exchanges it for an access token
 * at the key's token address and keeps that token until shortly before it expires.
 *
 * A message is [PushResult.InvalidToken] when FCM says the device is gone (404, or 400 naming the token, or a token from
 * another project); every other refusal, a timeout or an I/O error is [PushResult.Failed], and nothing is retried.
 *
 * Only the status and FCM's error code are logged: never the key, a token, an access token or a message.
 */
class FcmPushSender(
    private val credentials: FcmCredentials,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val requestTimeout: Duration = Duration.ofSeconds(10),
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
) : PushSender {
    override val enabled: Boolean = true

    private val log = LoggerFactory.getLogger(FcmPushSender::class.java)
    private val json = Json

    private var accessToken: String? = null
    private var accessTokenExpiresAt = 0L

    override fun send(messages: List<PushMessage>): List<PushResult> {
        if (messages.isEmpty()) return emptyList()
        val token = accessToken() ?: return messages.map { PushResult.Failed }
        return messages.map { sendOne(it, token) }
    }

    private fun sendOne(message: PushMessage, bearer: String): PushResult {
        val request = HttpRequest.newBuilder(URI.create("${baseUrl.trimEnd('/')}/v1/projects/${credentials.projectId}/messages:send"))
            .timeout(requestTimeout)
            .header("Authorization", "Bearer $bearer")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toFcm(message).toString()))
            .build()
        return try {
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            val status = response.statusCode()
            if (status in 200..299) return PushResult.Sent
            val error = FcmError.of(response.body())
            log.warn("FCM answered {} ({})", status, error.code ?: "no error code")
            if (status == 401) forgetAccessToken()
            if (error.meansDeviceIsGone(status)) PushResult.InvalidToken else PushResult.Failed
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("Sending the push messages was interrupted")
            PushResult.Failed
        } catch (e: Exception) {
            log.warn("FCM could not be reached ({})", e::class.simpleName)
            PushResult.Failed
        }
    }

    /** Data only, so that the apps build the notification themselves; high priority so that it arrives while the device dozes. */
    private fun toFcm(message: PushMessage): JsonObject = buildJsonObject {
        putJsonObject("message") {
            put("token", message.token)
            putJsonObject("data") {
                put(NotificationData.CATEGORY, message.category.name.lowercase())
                put(NotificationData.GROUP_ID, message.groupId.toString())
                put(NotificationData.TITLE, message.title)
                put(NotificationData.BODY, message.body)
            }
            putJsonObject("android") {
                put("priority", "HIGH")
                put("ttl", "$TTL_SECONDS" + "s")
            }
            putJsonObject("webpush") {
                putJsonObject("headers") {
                    put("Urgency", "high")
                    put("TTL", "$TTL_SECONDS")
                }
            }
        }
    }

    @Synchronized
    private fun accessToken(): String? {
        val now = nowSeconds()
        accessToken?.let { if (now < accessTokenExpiresAt - EXPIRY_MARGIN_SECONDS) return it }
        return try {
            val request = HttpRequest.newBuilder(URI.create(credentials.tokenUri))
                .timeout(requestTimeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=${encode(JWT_BEARER)}&assertion=${encode(signedJwt(now))}"))
                .build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                log.warn("Google's token service answered {} ({})", response.statusCode(), FcmError.oauthName(response.body()))
                return null
            }
            val body = json.parseToJsonElement(response.body()).jsonObject
            val token = body["access_token"]?.jsonPrimitive?.content ?: return null
            accessToken = token
            accessTokenExpiresAt = now + (body["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: DEFAULT_LIFETIME_SECONDS)
            token
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: Exception) {
            log.warn("Google's token service could not be used ({})", e::class.simpleName)
            null
        }
    }

    @Synchronized
    private fun forgetAccessToken() {
        accessToken = null
    }

    private fun signedJwt(now: Long): String {
        val header = base64Url("""{"alg":"RS256","typ":"JWT"}""".toByteArray())
        val claims = base64Url(
            buildJsonObject {
                put("iss", credentials.clientEmail)
                put("scope", SCOPE)
                put("aud", credentials.tokenUri)
                put("iat", now)
                put("exp", now + JWT_LIFETIME_SECONDS)
            }.toString().toByteArray(),
        )
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(credentials.privateKey)
            update("$header.$claims".toByteArray())
            sign()
        }
        return "$header.$claims.${base64Url(signature)}"
    }

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

    /** What FCM said was wrong: its `errorCode` and, for a 400, whether the token field is the one it objects to. */
    private class FcmError(val code: String?, private val objectsToToken: Boolean) {
        fun meansDeviceIsGone(status: Int): Boolean = when (status) {
            404 -> true
            400 -> code == "INVALID_ARGUMENT" && objectsToToken
            403 -> code == "SENDER_ID_MISMATCH"
            else -> false
        }

        companion object {
            fun of(body: String): FcmError = try {
                val details = Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("details")?.jsonArray ?: JsonArray(emptyList())
                val code = details.firstNotNullOfOrNull { it.jsonObject["errorCode"]?.jsonPrimitive?.content }
                val token = details.any { detail ->
                    detail.jsonObject["fieldViolations"]?.jsonArray?.any { it.jsonObject["field"]?.jsonPrimitive?.content == "message.token" } == true
                }
                FcmError(code, token)
            } catch (_: Exception) {
                FcmError(null, false)
            }

            /** The OAuth error name (such as `invalid_grant`); its description is left out. */
            fun oauthName(body: String): String = try {
                Json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content ?: "no error name"
            } catch (_: Exception) {
                "no error name"
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://fcm.googleapis.com"
        private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
        private const val JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        private const val JWT_LIFETIME_SECONDS = 3600L
        private const val DEFAULT_LIFETIME_SECONDS = 3600L
        private const val EXPIRY_MARGIN_SECONDS = 120L

        /** A notification that cannot be delivered within a day is not worth showing any more. */
        private const val TTL_SECONDS = 86_400
    }
}
