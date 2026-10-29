package com.usuallycoding.opensecretsanta.server

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.domain.notification.PushMessage
import com.usuallycoding.opensecretsanta.domain.notification.PushResult
import com.usuallycoding.opensecretsanta.server.config.FcmCredentials
import com.usuallycoding.opensecretsanta.server.data.push.FcmPushSender
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.security.Signature
import java.time.Duration
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The FCM adapter against a stand-in for Google's token service and FCM on localhost. */
class FcmPushSenderTest {
    private class Received(val path: String, val authorization: String?, val body: String)

    private val received = mutableListOf<Received>()
    private var tokenAnswer: () -> Pair<Int, String> = { 200 to """{"access_token":"at-1","expires_in":3600,"token_type":"Bearer"}""" }
    private var sendAnswer: (Received) -> Pair<Int, String> = { 200 to """{"name":"projects/test-project/messages/1"}""" }
    private var delayMillis = 0L
    private var clock = 1_000_000L

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange: HttpExchange ->
            val request = Received(exchange.requestURI.path, exchange.requestHeaders.getFirst("Authorization"), exchange.requestBody.readBytes().decodeToString())
            synchronized(received) { received += request }
            if (delayMillis > 0) Thread.sleep(delayMillis)
            val (status, body) = if (request.path == "/token") tokenAnswer() else sendAnswer(request)
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }

    private val baseUrl = "http://127.0.0.1:${server.address.port}"
    private val keyFile = writeServiceAccountKey(tokenUri = "$baseUrl/token")
    private val credentials = FcmCredentials.load(keyFile.first.toString())
    private val logs = ListAppender<ILoggingEvent>().apply { start() }
    private val logger = LoggerFactory.getLogger(FcmPushSender::class.java) as Logger

    init {
        logger.addAppender(logs)
    }

    @AfterTest
    fun stop() {
        server.stop(0)
        logger.detachAppender(logs)
    }

    private fun sender(timeout: Duration = Duration.ofSeconds(5)) =
        FcmPushSender(credentials, baseUrl, timeout, nowSeconds = { clock })

    private fun message(token: String = "device-token-1") = PushMessage(token, NotificationCategory.QUESTIONS, 7, "Secret Santa", "A question for you in Party")

    private fun logged(): String = logs.list.joinToString("\n") { it.formattedMessage + (it.throwableProxy?.message ?: "") }

    private fun sends() = received.filter { it.path != "/token" }

    private fun fcmError(status: String, code: String, tokenField: Boolean = false) = buildString {
        append("""{"error":{"code":0,"message":"private words","status":"$status","details":[""")
        if (tokenField) append("""{"@type":"type.googleapis.com/google.rpc.BadRequest","fieldViolations":[{"field":"message.token","description":"x"}]},""")
        append("""{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError","errorCode":"$code"}]}}""")
    }

    @Test
    fun aMessageIsOnePostToTheProjectWithTheAccessTokenAndDataOnly() {
        val results = sender().send(listOf(message()))

        assertEquals(listOf(PushResult.Sent), results)
        val request = sends().single()
        assertEquals("/v1/projects/test-project/messages:send", request.path)
        assertEquals("Bearer at-1", request.authorization)
        val body = Json.parseToJsonElement(request.body).jsonObject["message"]!!.jsonObject
        assertEquals("device-token-1", body["token"]!!.jsonPrimitive.content)
        assertFalse("notification" in body, "the apps build the notification from the data")
        val data = body["data"]!!.jsonObject
        assertEquals("questions", data["category"]!!.jsonPrimitive.content)
        assertEquals("7", data["groupId"]!!.jsonPrimitive.content)
        assertEquals("Secret Santa", data["title"]!!.jsonPrimitive.content)
        assertEquals("A question for you in Party", data["body"]!!.jsonPrimitive.content)
        assertEquals("HIGH", body["android"]!!.jsonObject["priority"]!!.jsonPrimitive.content)
    }

    @Test
    fun theAccessTokenIsRequestedWithAJwtSignedByTheServiceAccountKey() {
        sender().send(listOf(message()))

        val form = received.first { it.path == "/token" }.body.split('&').associate { pair ->
            pair.substringBefore('=') to URLDecoder.decode(pair.substringAfter('='), Charsets.UTF_8)
        }
        assertEquals("urn:ietf:params:oauth:grant-type:jwt-bearer", form["grant_type"])
        val (header, claims, signature) = form["assertion"]!!.split('.')
        val decoder = Base64.getUrlDecoder()
        assertEquals("RS256", Json.parseToJsonElement(decoder.decode(header).decodeToString()).jsonObject["alg"]!!.jsonPrimitive.content)
        val claimSet = Json.parseToJsonElement(decoder.decode(claims).decodeToString()).jsonObject
        assertEquals("push@test-project.iam.gserviceaccount.com", claimSet["iss"]!!.jsonPrimitive.content)
        assertEquals("https://www.googleapis.com/auth/firebase.messaging", claimSet["scope"]!!.jsonPrimitive.content)
        assertEquals("$baseUrl/token", claimSet["aud"]!!.jsonPrimitive.content)
        val verifies = Signature.getInstance("SHA256withRSA").run {
            initVerify(keyFile.second.public)
            update("$header.$claims".toByteArray())
            verify(decoder.decode(signature))
        }
        assertTrue(verifies, "signed with the service account's private key")
    }

    @Test
    fun theAccessTokenIsKeptUntilShortlyBeforeItExpires() {
        val sender = sender()
        sender.send(listOf(message()))
        clock += 3000
        sender.send(listOf(message()))
        assertEquals(1, received.count { it.path == "/token" })

        clock += 600 // 3600 s after the first token was issued, less the safety margin
        sender.send(listOf(message()))
        assertEquals(2, received.count { it.path == "/token" })
    }

    @Test
    fun severalMessagesShareOneAccessToken() {
        val results = sender().send(listOf(message("a"), message("b"), message("c")))

        assertEquals(List(3) { PushResult.Sent }, results)
        assertEquals(1, received.count { it.path == "/token" })
        assertEquals(3, sends().size)
    }

    @Test
    fun aDeviceThatIsGoneIsReportedAsInvalid() {
        sendAnswer = { 404 to fcmError("NOT_FOUND", "UNREGISTERED") }
        assertEquals(listOf(PushResult.InvalidToken), sender().send(listOf(message())))

        sendAnswer = { 400 to fcmError("INVALID_ARGUMENT", "INVALID_ARGUMENT", tokenField = true) }
        assertEquals(listOf(PushResult.InvalidToken), sender().send(listOf(message())))

        sendAnswer = { 403 to fcmError("PERMISSION_DENIED", "SENDER_ID_MISMATCH") }
        assertEquals(listOf(PushResult.InvalidToken), sender().send(listOf(message())))
    }

    @Test
    fun otherRefusalsAreFailuresAndKeepTheDevice() {
        sendAnswer = { 400 to fcmError("INVALID_ARGUMENT", "INVALID_ARGUMENT") } // not about the token
        assertEquals(listOf(PushResult.Failed), sender().send(listOf(message())))

        sendAnswer = { 403 to fcmError("PERMISSION_DENIED", "PERMISSION_DENIED") }
        assertEquals(listOf(PushResult.Failed), sender().send(listOf(message())))

        sendAnswer = { 503 to "unavailable" }
        assertEquals(listOf(PushResult.Failed), sender().send(listOf(message())))
        assertEquals(3, sends().size, "nothing is retried")
    }

    @Test
    fun oneBadDeviceDoesNotStopTheOthers() {
        sendAnswer = { request -> if ("dead" in request.body) 404 to fcmError("NOT_FOUND", "UNREGISTERED") else 200 to "{}" }

        val results = sender().send(listOf(message("ok-1"), message("dead"), message("ok-2")))

        assertEquals(listOf(PushResult.Sent, PushResult.InvalidToken, PushResult.Sent), results)
    }

    @Test
    fun aRefusedAccessTokenFailsEverythingAndSendsNothing() {
        tokenAnswer = { 400 to """{"error":"invalid_grant","error_description":"Invalid JWT Signature."}""" }

        val results = sender().send(listOf(message("a"), message("b")))

        assertEquals(listOf(PushResult.Failed, PushResult.Failed), results)
        assertEquals(0, sends().size)
        assertTrue("invalid_grant" in logged())
    }

    @Test
    fun anExpiredAccessTokenIsAskedForAgainAfterA401() {
        var calls = 0
        tokenAnswer = { 200 to """{"access_token":"at-${++calls}","expires_in":3600}""" }
        sendAnswer = { request -> if (request.authorization == "Bearer at-1") 401 to fcmError("UNAUTHENTICATED", "UNAUTHENTICATED") else 200 to "{}" }
        val sender = sender()

        assertEquals(listOf(PushResult.Failed), sender.send(listOf(message())))
        assertEquals(listOf(PushResult.Sent), sender.send(listOf(message())))
    }

    @Test
    fun aSlowServiceTimesOutAsAFailure() {
        delayMillis = 1500

        val results = sender(timeout = Duration.ofMillis(300)).send(listOf(message()))

        assertEquals(listOf(PushResult.Failed), results)
    }

    @Test
    fun anUnreachableServiceFailsEveryMessage() {
        server.stop(0)

        assertEquals(List(2) { PushResult.Failed }, sender().send(listOf(message("a"), message("b"))))
    }

    @Test
    fun nothingToSendTouchesNoService() {
        assertEquals(emptyList(), sender().send(emptyList()))
        assertEquals(0, received.size)
    }

    @Test
    fun theLogNeverHoldsTheKeyATokenOrAMessage() {
        sendAnswer = { 404 to fcmError("NOT_FOUND", "UNREGISTERED") }
        tokenAnswer = { 200 to """{"access_token":"at-secret-1","expires_in":3600}""" }

        sender().send(listOf(message("device-token-secret")))
        server.stop(0)
        sender().send(listOf(message("device-token-secret")))

        val log = logged()
        assertTrue("UNREGISTERED" in log)
        for (secret in listOf("device-token-secret", "at-secret-1", "A question for you", "private words", "PRIVATE KEY")) {
            assertFalse(secret in log, secret)
        }
    }
}
