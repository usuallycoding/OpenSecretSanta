package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.AnnouncementRequest
import com.usuallycoding.opensecretsanta.contract.AnnouncementResponse
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.contract.PushSummaryDto
import com.usuallycoding.opensecretsanta.domain.email.EmailMessage
import com.usuallycoding.opensecretsanta.domain.email.Mailer
import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.domain.notification.PushMessage
import com.usuallycoding.opensecretsanta.domain.notification.PushResult
import com.usuallycoding.opensecretsanta.domain.notification.PushSender
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteDeviceRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Announcements through the real route, with stand-in email and push services. */
class AnnouncementRoutesTest {
    private class FakeMailer(private val accepts: (EmailMessage) -> Boolean = { true }) : Mailer {
        override val enabled: Boolean = true
        val sent = mutableListOf<EmailMessage>()
        var throwing = false

        override fun send(messages: List<EmailMessage>): List<Boolean> {
            if (throwing) throw IllegalStateException("the mail service exploded")
            sent += messages
            return messages.map(accepts)
        }
    }

    private class FakeSender : PushSender {
        override val enabled = true
        val sent = mutableListOf<PushMessage>()

        override fun send(messages: List<PushMessage>): List<PushResult> {
            sent += messages
            return messages.map { PushResult.Sent }
        }
    }

    private val fx = TestFixtures()
    private val mailer = FakeMailer()
    private val sender = FakeSender()
    private val devices = SqliteDeviceRepository(fx.database)
    private val names = listOf("alice", "bob", "carol", "dave")

    private fun ApplicationTestBuilder.startApp(withMail: Boolean = true, withPush: Boolean = true) = application {
        module(
            fx.config,
            mailer = mailer.takeIf { withMail },
            pushSender = sender.takeIf { withPush },
            notificationExecutor = Executor { it.run() },
            notificationScheduler = ManualScheduler(),
        )
    }

    private suspend inline fun <reified T> HttpClient.send(method: String, path: String, token: String?, body: T? = null): HttpResponse =
        request(path) {
            this.method = HttpMethod.parse(method)
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(body))
            }
        }

    private fun setup() {
        fx.addUser("admin", admin = true)
        names.forEach {
            fx.addUser(it)
            fx.users.updateProfile(Username.parse(it), Profile(fullName = "$it Smith", email = "$it@example.org", phone = "+40 123", address = "1 Main St", deliveryInfo = "Ring twice"))
            devices.register(Username.parse(it), "$it-phone", DevicePlatform.ANDROID, 1)
        }
    }

    private suspend fun HttpClient.group(pending: Boolean = true): AdminGroupDto {
        val created = send("POST", ApiRoutes.ADMIN_GROUPS, fx.token("admin"), CreateGroupRequest("Party", description = "Bring snacks", participants = names))
        val group = Json.decodeFromString<AdminGroupDto>(created.bodyAsText())
        if (pending) assertEquals(HttpStatusCode.OK, send<Unit>("POST", ApiRoutes.adminGroupPending(group.id), fx.token("admin")).status)
        sender.sent.clear() // going Pending told everyone already
        return group
    }

    private suspend fun HttpClient.announce(id: Long, request: AnnouncementRequest, token: String? = fx.token("admin")) =
        send("POST", ApiRoutes.adminGroupAnnouncements(id), token, request)

    private val both = AnnouncementRequest("Schedule change", "We start at 19:00.", email = true, push = true)

    @Test
    fun anAnnouncementAsEmailAndPushReportsBothChannels() = testApplication {
        startApp()
        setup()
        val group = client.group()

        val response = client.announce(group.id, both)

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val body = Json.decodeFromString<AnnouncementResponse>(response.bodyAsText())
        assertEquals(EmailSummaryDto(sent = 4, failed = 0, noAddress = 0), body.emails)
        assertEquals(PushSummaryDto(reached = 4, notReached = 0), body.push)
        assertEquals(listOf("alice@example.org", "bob@example.org", "carol@example.org", "dave@example.org"), mailer.sent.map { it.to }.sorted())
        assertEquals("Schedule change: Party", mailer.sent.first().subject)
        assertTrue("We start at 19:00." in mailer.sent.first().text)
        assertEquals(4, sender.sent.size)
        assertTrue(sender.sent.all { it.title == "Schedule change: Party" && it.body == "We start at 19:00." && it.category == NotificationCategory.ANNOUNCEMENTS && it.groupId == group.id })
    }

    @Test
    fun anActiveGroupCanBeAnnouncedToToo() = testApplication {
        startApp()
        setup()
        val group = client.group()
        assertEquals(HttpStatusCode.OK, client.send<Unit>("POST", ApiRoutes.adminGroupActivate(group.id), fx.token("admin")).status)
        mailer.sent.clear()

        assertEquals(HttpStatusCode.OK, client.announce(group.id, both.copy(push = false)).status)
        assertEquals(4, mailer.sent.size)
    }

    @Test
    fun aSingleChannelHasNoSummaryForTheOther() = testApplication {
        startApp()
        setup()
        val group = client.group()

        val emailOnly = Json.decodeFromString<AnnouncementResponse>(client.announce(group.id, both.copy(push = false)).bodyAsText())
        assertNull(emailOnly.push)
        assertEquals(0, sender.sent.size)
        val pushOnly = Json.decodeFromString<AnnouncementResponse>(client.announce(group.id, both.copy(email = false)).bodyAsText())
        assertNull(pushOnly.emails)
    }

    @Test
    fun aDraftGroupIsRefusedWith409AndNothingIsSent() = testApplication {
        startApp()
        setup()
        val group = client.group(pending = false)

        assertEquals(HttpStatusCode.Conflict, client.announce(group.id, both).status)
        assertTrue(mailer.sent.isEmpty() && sender.sent.isEmpty())
    }

    @Test
    fun badInputIs400NamingTheField() = testApplication {
        startApp()
        setup()
        val group = client.group()

        for ((request, field) in listOf(
            both.copy(title = "") to "title",
            both.copy(title = "a\nb") to "title",
            both.copy(body = " ") to "body",
            both.copy(body = "x".repeat(2001)) to "body",
            both.copy(email = false, push = false) to "channels",
        )) {
            val response = client.announce(group.id, request)
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue("\"$field\"" in response.bodyAsText(), response.bodyAsText())
        }
        assertTrue(mailer.sent.isEmpty() && sender.sent.isEmpty())
    }

    @Test
    fun aChannelThatIsNotSetUpIs409AndSendsNothingOnTheOther() = testApplication {
        startApp(withMail = false)
        setup()
        val group = client.group()

        val response = client.announce(group.id, both)

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue("Email" in response.bodyAsText())
        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun onlyTheOwnerMayAnnounce() = testApplication {
        startApp()
        setup()
        fx.addUser("other", admin = true)
        val group = client.group()

        assertEquals(HttpStatusCode.NotFound, client.announce(group.id, both, fx.token("other")).status)
        assertEquals(HttpStatusCode.Forbidden, client.announce(group.id, both, fx.token("alice")).status)
        assertEquals(HttpStatusCode.Unauthorized, client.announce(group.id, both, token = null).status)
        assertTrue(mailer.sent.isEmpty() && sender.sent.isEmpty())
    }

    @Test
    fun aMailerThatExplodesStillLetsThePushThrough() = testApplication {
        startApp()
        setup()
        mailer.throwing = true
        val group = client.group()

        val body = Json.decodeFromString<AnnouncementResponse>(client.announce(group.id, both).bodyAsText())

        assertEquals(EmailSummaryDto(sent = 0, failed = 4, noAddress = 0), body.emails)
        assertEquals(4, sender.sent.size)
    }

    @Test
    fun theAccountSaysWhatTheServerCanSend() = testApplication {
        startApp(withMail = true, withPush = false)
        setup()

        val me = Json.decodeFromString<AccountDto>(client.send<Unit>("GET", ApiRoutes.ME, fx.token("alice")).bodyAsText())

        assertTrue(me.emailAvailable)
        assertFalse(me.pushAvailable)
    }
}
