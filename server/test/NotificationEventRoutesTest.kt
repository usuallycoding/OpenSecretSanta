package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.ParticipantsRequest
import com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest
import com.usuallycoding.opensecretsanta.contract.SendLetterRequest
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import com.usuallycoding.opensecretsanta.domain.notification.NotificationSettings
import com.usuallycoding.opensecretsanta.domain.notification.PushMessage
import com.usuallycoding.opensecretsanta.domain.notification.PushResult
import com.usuallycoding.opensecretsanta.domain.notification.PushSender
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteDeviceRepository
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteNotificationSettingsRepository
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
import kotlin.test.assertTrue

/** Which actions send which notification to whom, through the real routes, with a stand-in push service. */
class NotificationEventRoutesTest {
    private class FakeSender : PushSender {
        override val enabled = true
        val sent = mutableListOf<PushMessage>()
        var throwing = false

        override fun send(messages: List<PushMessage>): List<PushResult> {
            if (throwing) throw IllegalStateException("the push service exploded")
            sent += messages
            return messages.map { PushResult.Sent }
        }
    }

    private val fx = TestFixtures()
    private val sender = FakeSender()
    private val scheduler = ManualScheduler()
    private val devices = SqliteDeviceRepository(fx.database)
    private val names = listOf("alice", "bob", "carol", "dave")

    private fun ApplicationTestBuilder.startApp() = application {
        module(fx.config, pushSender = sender, notificationExecutor = Executor { it.run() }, notificationScheduler = scheduler)
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

    private fun setup(extra: List<String> = emptyList()) {
        fx.addUser("admin", admin = true)
        (names + extra).forEach { fx.addUser(it) }
        (names + extra + "admin").forEach { devices.register(Username.parse(it), "$it-phone", DevicePlatform.ANDROID, 1) }
    }

    private suspend fun HttpClient.draft(vararg participants: String): AdminGroupDto {
        val created = send("POST", ApiRoutes.ADMIN_GROUPS, fx.token("admin"), CreateGroupRequest("Party", participants = participants.toList()))
        return Json.decodeFromString(created.bodyAsText())
    }

    private suspend fun HttpClient.toPending(id: Long) = send<Unit>("POST", ApiRoutes.adminGroupPending(id), fx.token("admin"))

    private suspend fun HttpClient.activate(id: Long) = send<Unit>("POST", ApiRoutes.adminGroupActivate(id), fx.token("admin"))

    private suspend fun HttpClient.setParticipants(id: Long, vararg participants: String) =
        send("PUT", ApiRoutes.adminGroupParticipants(id), fx.token("admin"), ParticipantsRequest(participants.toList()))

    private suspend fun HttpClient.activeGroup(): Pair<Long, Map<String, String>> {
        val group = draft(*names.toTypedArray())
        assertEquals(HttpStatusCode.OK, toPending(group.id).status)
        assertEquals(HttpStatusCode.OK, activate(group.id).status)
        val pairs = Json.decodeFromString<List<AssignmentDto>>(send<Unit>("GET", ApiRoutes.adminGroupAssignments(group.id), fx.token("admin")).bodyAsText())
        sender.sent.clear()
        return group.id to pairs.associate { it.santa to it.receiver }
    }

    private fun recipients() = sender.sent.map { it.token.removeSuffix("-phone") }.sorted()

    // --- announcements ---

    @Test
    fun aGroupInDraftNotifiesNobodyAndLeavingDraftNotifiesEveryParticipant() = testApplication {
        startApp()
        setup()

        val group = client.draft("alice", "bob", "carol")
        client.setParticipants(group.id, "alice", "bob", "carol", "dave")
        assertEquals(emptyList(), sender.sent)

        client.toPending(group.id)

        assertEquals(listOf("alice", "bob", "carol", "dave"), recipients())
        assertTrue(sender.sent.all { it.category == NotificationCategory.ANNOUNCEMENTS && it.groupId == group.id })
    }

    @Test
    fun addingSomeoneToAPendingGroupNotifiesOnlyThem() = testApplication {
        startApp()
        setup(extra = listOf("erin"))
        val group = client.draft("alice", "bob", "carol")
        client.toPending(group.id)
        sender.sent.clear()

        client.setParticipants(group.id, "alice", "bob", "carol", "erin")

        assertEquals(listOf("erin"), recipients())
    }

    @Test
    fun theDrawNotifiesEveryParticipantAndNamesNoReceiver() = testApplication {
        startApp()
        setup()
        val group = client.draft(*names.toTypedArray())
        client.toPending(group.id)
        sender.sent.clear()

        val response = client.activate(group.id)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(names, recipients())
        assertTrue(sender.sent.all { message -> names.none { it in message.body } })
    }

    @Test
    fun aRefusedActivationNotifiesNobody() = testApplication {
        startApp()
        setup()
        val group = client.draft("alice", "bob") // two people are too few
        client.toPending(group.id)
        sender.sent.clear()

        val response = client.activate(group.id)

        assertTrue(response.status.value in 400..499, response.status.toString())
        assertEquals(emptyList(), sender.sent)
    }

    // --- the daily digest ---

    private fun digestSent() = sender.sent.filter { it.category == NotificationCategory.DAILY }

    /** The app schedules its digest from the real clock, so this moves the manual scheduler past the next real 20:00 in Bucharest. */
    private fun ManualScheduler.toDigest() = advance(NotificationDispatcher.millisUntilNextDigest(System.currentTimeMillis()) + 60_000L)

    @Test
    fun aQuestionAndItsAnswerSendNothingAtOnceAndAreToldInTheDigest() = testApplication {
        startApp()
        setup()
        val (group, receivers) = client.activeGroup()
        val santa = names.first()
        val receiver = receivers.getValue(santa)

        val asked = client.send("POST", ApiRoutes.groupReceiverQuestions(group), fx.token(santa), AskQuestionRequest("Favourite colour?"))
        assertEquals(HttpStatusCode.Created, asked.status, asked.bodyAsText())
        val answered = client.send("PUT", ApiRoutes.groupSantaQuestionAnswer(group, 1), fx.token(receiver), AnswerRequest("Green"))
        assertEquals(HttpStatusCode.OK, answered.status, answered.bodyAsText())
        scheduler.advance(10 * 60 * 1000)
        assertEquals(emptyList(), sender.sent, "nothing is sent when it happens")

        scheduler.toDigest()

        assertEquals(listOf(santa, receiver).sorted(), recipients())
        assertTrue(sender.sent.all { it.category == NotificationCategory.DAILY && it.groupId == null })
        assertTrue(sender.sent.all { "Favourite colour" !in it.body && "Green" !in it.body })
    }

    @Test
    fun lettersAreToldToTheSantaInTheDigestWithoutTheirContent() = testApplication {
        startApp()
        setup()
        val (group, receivers) = client.activeGroup()
        val receiver = names[1]
        val santa = receivers.entries.single { it.value == receiver }.key

        repeat(3) { client.send("POST", ApiRoutes.groupSantaLetters(group), fx.token(receiver), SendLetterRequest("Zebra title $it", "Zebra text $it")) }
        assertEquals(emptyList(), sender.sent)
        scheduler.toDigest()

        assertEquals(listOf(santa), recipients())
        val message = sender.sent.single()
        assertEquals("Some things changed in your groups", message.body)
        assertTrue("Zebra" !in message.body && "Zebra" !in message.title)
    }

    @Test
    fun aPersonWhoSwitchedTheDigestOffIsNotToldButTheActionIsStored() = testApplication {
        startApp()
        setup()
        val (group, receivers) = client.activeGroup()
        val santa = names.first()
        val receiver = receivers.getValue(santa)
        SqliteNotificationSettingsRepository(fx.database).save(Username.parse(receiver), NotificationSettings(daily = false))

        val asked = client.send("POST", ApiRoutes.groupReceiverQuestions(group), fx.token(santa), AskQuestionRequest("Size?"))
        scheduler.toDigest()

        assertEquals(HttpStatusCode.Created, asked.status)
        assertEquals(emptyList(), sender.sent)
    }

    @Test
    fun aRefusedQuestionMarksNobody() = testApplication {
        startApp()
        setup()
        val group = client.draft(*names.toTypedArray())
        client.toPending(group.id) // not drawn yet
        sender.sent.clear()

        val asked = client.send("POST", ApiRoutes.groupReceiverQuestions(group.id), fx.token("alice"), AskQuestionRequest("Too early?"))

        assertTrue(asked.status.value in 400..499, asked.status.toString())
        scheduler.toDigest()
        assertEquals(emptyList(), digestSent())
    }

    @Test
    fun aFailingPushServiceDoesNotFailTheAction() = testApplication {
        startApp()
        setup()
        val (group, _) = client.activeGroup()
        sender.throwing = true

        val asked = client.send("POST", ApiRoutes.groupReceiverQuestions(group), fx.token(names.first()), AskQuestionRequest("Anything?"))
        scheduler.toDigest()

        assertEquals(HttpStatusCode.Created, asked.status, asked.bodyAsText())
    }

    @Test
    fun wishlistChangesAreToldToTheSantaInTheDigestAndReorderingMarksNobody() = testApplication {
        startApp()
        setup()
        val (group, receivers) = client.activeGroup()
        val editor = names[1]
        val santa = receivers.entries.single { it.value == editor }.key

        val first = Json.decodeFromString<WishlistItemDto>(client.send("POST", ApiRoutes.groupWishlistItems(group), fx.token(editor), WishlistItemRequest("Scarf")).bodyAsText())
        val second = Json.decodeFromString<WishlistItemDto>(client.send("POST", ApiRoutes.groupWishlistItems(group), fx.token(editor), WishlistItemRequest("Socks")).bodyAsText())
        client.send("PUT", ApiRoutes.groupWishlistItem(group, first.id), fx.token(editor), WishlistItemRequest("Blue scarf"))
        client.send("PUT", ApiRoutes.groupWishlistOrder(group), fx.token(editor), ReorderWishlistRequest(listOf(second.id, first.id)))
        client.send<Unit>("DELETE", ApiRoutes.groupWishlistItem(group, second.id), fx.token(editor))
        assertEquals(emptyList(), sender.sent)

        scheduler.toDigest()

        assertEquals(listOf(santa), recipients())
        assertTrue(sender.sent.single().body.let { "Scarf" !in it && "Socks" !in it })
    }

    @Test
    fun onlyReorderingMarksNobody() = testApplication {
        startApp()
        setup()
        val (group, _) = client.activeGroup()
        val editor = names[1]
        val item = Json.decodeFromString<WishlistItemDto>(client.send("POST", ApiRoutes.groupWishlistItems(group), fx.token(editor), WishlistItemRequest("Scarf")).bodyAsText())
        scheduler.toDigest() // the digest for the item
        sender.sent.clear()

        client.send("PUT", ApiRoutes.groupWishlistOrder(group), fx.token(editor), ReorderWishlistRequest(listOf(item.id)))
        scheduler.toDigest()

        assertEquals(emptyList(), sender.sent)
    }
}
