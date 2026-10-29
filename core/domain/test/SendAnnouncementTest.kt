package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.announcement.SendAnnouncement
import com.usuallycoding.opensecretsanta.domain.email.EmailMessage
import com.usuallycoding.opensecretsanta.domain.email.EmailSummary
import com.usuallycoding.opensecretsanta.domain.email.Mailer
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.domain.notification.NotificationSettings
import com.usuallycoding.opensecretsanta.domain.notification.Notifier
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class AnnouncementMailer(override var enabled: Boolean = true) : Mailer {
    val sent = mutableListOf<EmailMessage>()
    var failFor: Set<String> = emptySet()
    var throwing = false

    override fun send(messages: List<EmailMessage>): List<Boolean> {
        if (throwing) throw IllegalStateException("service exploded")
        sent += messages
        return messages.map { it.to !in failFor }
    }
}

class SendAnnouncementTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val settings = FakeSettings()
    private val devices = FakeDevices()
    private val sender = RecordingPushSender()
    private val notifier = Notifier(settings, devices, sender, groups)
    private val mailer = AnnouncementMailer()
    private val owner = users.add("boss", admin = true).let { users.userNamed("boss") }
    private val names = listOf("alice", "bob", "carol", "dave")

    private fun u(name: String) = Username.parse(name)

    init {
        for (name in names) {
            users.add(name)
            users.updateProfile(u(name), Profile(fullName = null, email = "$name@example.org", phone = null, address = null, deliveryInfo = null))
        }
    }

    private fun announce(
        group: Group,
        title: String = "Schedule change",
        body: String = "We start at 19:00.",
        email: Boolean = true,
        push: Boolean = false,
        appUrl: String? = "https://santa.example",
        actor: com.usuallycoding.opensecretsanta.domain.user.User = owner,
    ) = SendAnnouncement(OwnedGroupLoader(groups), users, mailer, notifier, appUrl)(actor, group.id, title, body, email, push)

    private fun group(state: GroupState, details: GroupDetails = GroupDetails("Office Party")): Group {
        val seeded = groups.seed(owner.username, state, *names.map(::u).toTypedArray())
        groups.setDetails(seeded.id, details)
        return groups.find(seeded.id)!!
    }

    @Test
    fun aPendingAndAnActiveGroupAreAnnouncedTo() {
        for (state in listOf(GroupState.PENDING, GroupState.ACTIVE)) {
            mailer.sent.clear()
            assertEquals(EmailSummary(4, 0, 0), announce(group(state)).emails)
            assertEquals(4, mailer.sent.size)
        }
    }

    @Test
    fun aDraftAndAnArchivedGroupAreRefusedAndNothingIsSent() {
        for (state in listOf(GroupState.DRAFT, GroupState.ARCHIVED)) {
            assertFailsWith<DomainException.InvalidState> { announce(group(state), push = true) }
        }
        assertTrue(mailer.sent.isEmpty())
    }

    @Test
    fun anotherAdministratorGetsNotFound() {
        val other = users.add("other", admin = true).let { users.userNamed("other") }
        assertFailsWith<DomainException.NotFound> { announce(group(GroupState.PENDING), actor = other) }
        assertTrue(mailer.sent.isEmpty())
    }

    @Test
    fun aParticipantWhoIsNotAnAdministratorIsForbidden() {
        assertFailsWith<DomainException.Forbidden> { announce(group(GroupState.PENDING), actor = users.userNamed("alice")) }
    }

    @Test
    fun theTitleAndTheMessageAreChecked() {
        val g = group(GroupState.PENDING)
        fun field(block: () -> Unit) = assertFailsWith<DomainException.ValidationFailed>(block = block).field
        assertEquals("title", field { announce(g, title = "   ") })
        assertEquals("title", field { announce(g, title = "a".repeat(101)) })
        assertEquals("title", field { announce(g, title = "two\nlines") })
        assertEquals("body", field { announce(g, body = " ") })
        assertEquals("body", field { announce(g, body = "a".repeat(2001)) })
        assertTrue(mailer.sent.isEmpty())
        announce(g, title = "a".repeat(100), body = "b".repeat(2000))
    }

    @Test
    fun inputIsCheckedBeforeStateAndStateBeforeAvailability() {
        val draft = group(GroupState.DRAFT)
        mailer.enabled = false
        assertEquals("title", assertFailsWith<DomainException.ValidationFailed> { announce(draft, title = "") }.field)
        assertFailsWith<DomainException.InvalidState> { announce(draft) }
        assertTrue("not set up" in assertFailsWith<DomainException.InvalidState> { announce(group(GroupState.PENDING)) }.message!!)
    }

    @Test
    fun atLeastOneChannelIsNeeded() {
        val field = assertFailsWith<DomainException.ValidationFailed> { announce(group(GroupState.PENDING), email = false, push = false) }.field
        assertEquals("channels", field)
    }

    @Test
    fun aChannelThatIsNotSetUpRefusesEverythingEvenTheOtherChannel() {
        val g = group(GroupState.PENDING)
        names.forEach { devices.register(u(it), "$it-phone", DevicePlatform.ANDROID, 1) }
        mailer.enabled = false
        assertTrue("Email" in assertFailsWith<DomainException.InvalidState> { announce(g, email = true, push = true) }.message!!)
        mailer.enabled = true
        sender.enabled = false
        assertTrue("Push" in assertFailsWith<DomainException.InvalidState> { announce(g, email = true, push = true) }.message!!)
        assertTrue(mailer.sent.isEmpty())
        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun theEmailHasTheDrawLayoutWithTheMessageAndNothingOfTheDraw() {
        val g = group(GroupState.ACTIVE, GroupDetails("Office Party", description = "Bring snacks", date = LocalDate(2026, 12, 20), time = LocalTime(19, 0), location = "Canteen"))
        groups.assign(g.id, names.indices.map { Assignment(u(names[it]), u(names[(it + 1) % 4])) })

        announce(g, body = "Line one\nLine two")

        val alice = mailer.sent.single { it.to == "alice@example.org" }
        assertEquals("Schedule change: Office Party", alice.subject)
        assertEquals(
            "Hi alice,\n\nLine one\nLine two\n\nBring snacks\nWhen: 20 Dec 2026, 19:00\nWhere: Canteen\n\nOpen the app: https://santa.example\n\nKeep it secret!",
            alice.text,
        )
        assertFalse("Secret Santa for" in alice.text)
        assertFalse("bob" in alice.text)
    }

    @Test
    fun theLinkIsLeftOutWithoutAnAppAddress() {
        announce(group(GroupState.PENDING), appUrl = null)
        assertFalse("Open the app" in mailer.sent.first().text)
    }

    @Test
    fun eachEmailGoesToOnePersonAndPeopleWithoutAnAddressAreCounted() {
        users.updateProfile(u("carol"), Profile(fullName = null, email = null, phone = null, address = null, deliveryInfo = null))
        val summary = announce(group(GroupState.PENDING)).emails
        assertEquals(EmailSummary(sent = 3, failed = 0, noAddress = 1), summary)
        assertEquals(listOf("alice@example.org", "bob@example.org", "dave@example.org"), mailer.sent.map { it.to }.sorted())
    }

    @Test
    fun failedEmailsAreCountedAndAThrowingMailerFailsAllOfThem() {
        mailer.failFor = setOf("bob@example.org")
        assertEquals(EmailSummary(3, 1, 0), announce(group(GroupState.PENDING)).emails)
        mailer.throwing = true
        assertEquals(EmailSummary(0, 4, 0), announce(group(GroupState.PENDING)).emails)
    }

    @Test
    fun aFailingEmailStillLetsThePushThrough() {
        val g = group(GroupState.PENDING)
        names.forEach { devices.register(u(it), "$it-phone", DevicePlatform.ANDROID, 1) }
        mailer.throwing = true
        val outcome = announce(g, push = true)
        assertEquals(EmailSummary(0, 4, 0), outcome.emails)
        assertEquals(4, outcome.pushPlan!!.messages.size)
    }

    @Test
    fun noPushSummaryWithoutPushAndNoEmailSummaryWithoutEmail() {
        val g = group(GroupState.PENDING)
        assertNull(announce(g, email = true, push = false).pushPlan)
        val pushOnly = announce(g, email = false, push = true)
        assertNull(pushOnly.emails)
        assertNotNull(pushOnly.pushPlan)
    }

    @Test
    fun thePushHasTheSubjectAsTitleTheMessageAsTextAndTheAnnouncementsCategory() {
        val g = group(GroupState.PENDING)
        names.forEach { devices.register(u(it), "$it-phone", DevicePlatform.ANDROID, 1) }

        val plan = announce(g, email = false, push = true, body = "Meet at the canteen").pushPlan!!
        notifier.send(plan)

        val message = sender.sent.first()
        assertEquals("Schedule change: Office Party", message.title)
        assertEquals("Meet at the canteen", message.body)
        assertEquals(NotificationCategory.ANNOUNCEMENTS, message.category)
        assertEquals(g.id, message.groupId)
        assertEquals(4, sender.sent.size)
    }

    @Test
    fun aLongMessageIsCutAtThreeHundredCharactersWithAnEllipsis() {
        val g = group(GroupState.PENDING)
        devices.register(u("alice"), "alice-phone", DevicePlatform.ANDROID, 1)
        val plan = announce(g, email = false, push = true, body = "x".repeat(800)).pushPlan!!
        assertEquals("x".repeat(300) + "…", plan.messages.single().body)
        val exact = announce(g, email = false, push = true, body = "y".repeat(300)).pushPlan!!
        assertEquals("y".repeat(300), exact.messages.single().body)
    }

    @Test
    fun theSenderAndPeopleWithTheSwitchOffOrNoDeviceAreNotTold() {
        // the owner takes part too
        val withOwner = groups.find(groups.seed(owner.username, GroupState.PENDING, owner.username, *names.map(::u).toTypedArray()).id)!!
        (names + "boss").forEach { devices.register(u(it), "$it-phone", DevicePlatform.ANDROID, 1) }
        settings.save(u("bob"), NotificationSettings(announcements = false))
        devices.all.removeAll { it.username == u("dave") }

        val plan = announce(withOwner, email = false, push = true).pushPlan!!
        notifier.send(plan)

        assertEquals(listOf("alice-phone", "carol-phone"), sender.sent.map { it.token }.sorted())
        assertEquals(2, plan.reached)
        assertEquals(2, plan.notReached)
    }

    @Test
    fun aPersonWithTwoDevicesIsOnePersonReached() {
        val g = group(GroupState.PENDING)
        devices.register(u("alice"), "a1", DevicePlatform.ANDROID, 1)
        devices.register(u("alice"), "a2", DevicePlatform.WEB, 1)
        val plan = announce(g, email = false, push = true).pushPlan!!
        assertEquals(2, plan.messages.size)
        assertEquals(1, plan.reached)
        assertEquals(3, plan.notReached)
    }
}
