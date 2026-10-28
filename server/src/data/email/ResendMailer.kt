package com.usuallycoding.opensecretsanta.server.data.email

import com.usuallycoding.opensecretsanta.domain.email.EmailMessage
import com.usuallycoding.opensecretsanta.domain.email.Mailer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Sends email through Resend (`https://resend.com/docs/api-reference`). Messages go in batches of at most [BATCH_LIMIT]
 * to `/emails/batch`, one request at a time, which also keeps within Resend's per-second request limit. A 2xx answer
 * marks the whole batch sent; a 429 is retried once after a short pause; a refusal of the batch for a reason that
 * may be one message's own (a 4xx other than 401, 403 and 429) makes the messages go one by one to `/emails`, so that
 * one bad address does not cost the others theirs. A timeout, an I/O error or any other answer marks the messages failed.
 *
 * Only the status and Resend's error name are logged: never the key, an address or a message.
 */
class ResendMailer(
    private val apiKey: String,
    private val from: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val requestTimeout: Duration = Duration.ofSeconds(10),
    private val pause: (Long) -> Unit = { Thread.sleep(it) },
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
) : Mailer {
    override val enabled: Boolean = true

    private val log = LoggerFactory.getLogger(ResendMailer::class.java)
    private val json = Json

    override fun send(messages: List<EmailMessage>): List<Boolean> =
        messages.chunked(BATCH_LIMIT).flatMap { batch -> sendBatch(batch) }

    private fun sendBatch(batch: List<EmailMessage>): List<Boolean> {
        val answer = post("/emails/batch", JsonArray(batch.map(::toResend)).toString())
        return when {
            answer.ok -> batch.map { true }
            answer.status in FALL_BACK_STATUSES && batch.size > 1 -> batch.map { sendOne(it) }
            else -> batch.map { false }
        }
    }

    private fun sendOne(message: EmailMessage): Boolean = post("/emails", toResend(message).toString()).ok

    private fun toResend(message: EmailMessage): JsonObject = buildJsonObject {
        put("from", from)
        putJsonArray("to") { add(kotlinx.serialization.json.JsonPrimitive(message.to)) }
        put("subject", message.subject)
        put("text", message.text)
    }

    private class Answer(val status: Int) {
        val ok: Boolean get() = status in 200..299
    }

    /** Posts [body]; a 429 is retried once. Never throws: a failure to get an answer is status 0. */
    private fun post(path: String, body: String): Answer {
        var answer = attempt(path, body)
        if (answer.status == 429) {
            pause(RETRY_PAUSE_MILLIS)
            answer = attempt(path, body)
        }
        return answer
    }

    private fun attempt(path: String, body: String): Answer {
        val request = HttpRequest.newBuilder(URI.create(baseUrl.trimEnd('/') + path))
            .timeout(requestTimeout)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return try {
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                log.warn("Resend answered {} ({})", response.statusCode(), errorName(response.body()))
            }
            Answer(response.statusCode())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("Sending the emails was interrupted")
            Answer(0)
        } catch (e: Exception) {
            log.warn("Resend could not be reached ({})", e::class.simpleName)
            Answer(0)
        }
    }

    /** Resend's error `name` (such as `validation_error`), or "no error name"; its message may quote an address, so it is left out. */
    private fun errorName(body: String): String = try {
        json.parseToJsonElement(body).jsonObject["name"]?.jsonPrimitive?.content ?: "no error name"
    } catch (_: Exception) {
        "no error name"
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.resend.com"
        const val BATCH_LIMIT = 100
        private const val RETRY_PAUSE_MILLIS = 1_000L

        /** Refusals of a batch that one message may have caused: asked again message by message. */
        private val FALL_BACK_STATUSES = setOf(400, 404, 409, 413, 422)
    }
}
