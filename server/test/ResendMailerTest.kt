package com.usuallycoding.opensecretsanta.server

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.usuallycoding.opensecretsanta.domain.email.EmailMessage
import com.usuallycoding.opensecretsanta.server.data.email.ResendMailer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Resend adapter against a stand-in server on localhost: what it sends, how it batches and what it does with refusals. */
class ResendMailerTest {
    private class Received(val path: String, val authorization: String?, val contentType: String?, val body: String)

    private val received = mutableListOf<Received>()
    private var answer: (Received) -> Pair<Int, String> = { 200 to """{"data":[]}""" }
    private var delayMillis = 0L

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange: HttpExchange ->
            val request = Received(
                exchange.requestURI.path,
                exchange.requestHeaders.getFirst("Authorization"),
                exchange.requestHeaders.getFirst("Content-Type"),
                exchange.requestBody.readBytes().decodeToString(),
            )
            synchronized(received) { received += request }
            if (delayMillis > 0) Thread.sleep(delayMillis)
            val (status, body) = answer(request)
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }

    private val baseUrl = "http://127.0.0.1:${server.address.port}"
    private val pauses = mutableListOf<Long>()
    private val logs = ListAppender<ILoggingEvent>().apply { start() }
    private val logger = LoggerFactory.getLogger(ResendMailer::class.java) as Logger

    init {
        logger.addAppender(logs)
    }

    @AfterTest
    fun stop() {
        server.stop(0)
        logger.detachAppender(logs)
    }

    private fun mailer(timeout: Duration = Duration.ofSeconds(5)) =
        ResendMailer("re_topsecret_key", "Santa <santa@mail.example.org>", baseUrl, timeout, pause = { pauses += it })

    private fun messages(count: Int) = (1..count).map { EmailMessage("p$it@example.org", "Your draw $it", "Body $it for p$it") }

    private fun logged(): String = logs.list.joinToString("\n") { it.formattedMessage + (it.throwableProxy?.message ?: "") }

    @Test
    fun aBatchIsOnePostWithTheKeyTheSenderAndEveryMessage() {
        val results = mailer().send(messages(3))

        assertEquals(listOf(true, true, true), results)
        val request = received.single()
        assertEquals("/emails/batch", request.path)
        assertEquals("Bearer re_topsecret_key", request.authorization)
        assertEquals("application/json", request.contentType)
        val sent = Json.parseToJsonElement(request.body).jsonArray.map { it.jsonObject }
        assertEquals(3, sent.size)
        assertEquals("Santa <santa@mail.example.org>", sent[0]["from"]!!.jsonPrimitive.content)
        assertEquals(listOf("p1@example.org"), sent[0]["to"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("Your draw 1", sent[0]["subject"]!!.jsonPrimitive.content)
        assertEquals("Body 1 for p1", sent[0]["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun noMessagesSendNothing() {
        assertEquals(emptyList(), mailer().send(emptyList()))
        assertTrue(received.isEmpty())
    }

    @Test
    fun morePeopleThanAHundredGoInBatchesOfAtMostAHundred() {
        val results = mailer().send(messages(250))

        assertEquals(250, results.size)
        assertTrue(results.all { it })
        val sizes = received.map { (Json.parseToJsonElement(it.body) as JsonArray).size }
        assertEquals(listOf(100, 100, 50), sizes)
    }

    @Test
    fun aRefusedKeyFailsEverythingWithoutTryingOneByOne() {
        answer = { 401 to """{"name":"restricted_api_key","message":"nope"}""" }

        val results = mailer().send(messages(3))

        assertEquals(listOf(false, false, false), results)
        assertEquals(1, received.size)
    }

    @Test
    fun aBatchRefusedForOneBadAddressFallsBackToOneByOneSoTheOthersAreSent() {
        answer = { request ->
            when {
                request.path == "/emails/batch" -> 422 to """{"name":"validation_error","message":"bad address"}"""
                "p2@example.org" in request.body -> 422 to """{"name":"validation_error","message":"bad address"}"""
                else -> 200 to """{"id":"x"}"""
            }
        }

        val results = mailer().send(messages(3))

        assertEquals(listOf(true, false, true), results)
        assertEquals(listOf("/emails/batch", "/emails", "/emails", "/emails"), received.map { it.path })
        // one by one is a single object, not an array
        assertTrue(Json.parseToJsonElement(received[1].body) is JsonObject)
    }

    @Test
    fun aServerErrorFailsTheBatch() {
        answer = { 500 to """{"name":"application_error","message":"boom"}""" }

        assertEquals(listOf(false, false), mailer().send(messages(2)))
        assertEquals(1, received.size)
    }

    @Test
    fun aTooManyRequestsAnswerIsRetriedOnceAfterAPause() {
        var first = true
        answer = {
            if (first) {
                first = false
                429 to """{"name":"rate_limit_exceeded","message":"slow down"}"""
            } else {
                200 to """{"data":[]}"""
            }
        }

        val results = mailer().send(messages(2))

        assertEquals(listOf(true, true), results)
        assertEquals(2, received.size)
        assertEquals(1, pauses.size)
    }

    @Test
    fun aSecondTooManyRequestsAnswerFails() {
        answer = { 429 to """{"name":"rate_limit_exceeded","message":"slow down"}""" }

        assertEquals(listOf(false), mailer().send(messages(1)))
        assertEquals(2, received.size)
    }

    @Test
    fun aServerThatTakesTooLongFailsTheMessagesInsteadOfWaiting() {
        delayMillis = 1_500

        val results = mailer(timeout = Duration.ofMillis(300)).send(messages(2))

        assertEquals(listOf(false, false), results)
    }

    @Test
    fun anUnreachableServerFailsTheMessages() {
        server.stop(0)

        assertEquals(listOf(false, false), mailer().send(messages(2)))
    }

    @Test
    fun theLogNeverHoldsTheKeyAnAddressOrAMessage() {
        answer = { 422 to """{"name":"validation_error","message":"The to field p1@example.org is invalid"}""" }
        mailer().send(messages(2))
        server.stop(0)
        mailer().send(messages(2))

        val log = logged()
        assertTrue(log.isNotBlank())
        assertTrue("validation_error" in log)
        assertFalse("re_topsecret_key" in log)
        assertFalse("example.org" in log)
        assertFalse("Body 1" in log)
    }
}
