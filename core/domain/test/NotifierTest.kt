package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.notification.DeviceRepository
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.domain.notification.NotificationSettings
import com.usuallycoding.opensecretsanta.domain.notification.NotificationSettingsRepository
import com.usuallycoding.opensecretsanta.domain.notification.Notifier
import com.usuallycoding.opensecretsanta.domain.notification.PushMessage
import com.usuallycoding.opensecretsanta.domain.notification.PushResult
import com.usuallycoding.opensecretsanta.domain.notification.PushSender
import com.usuallycoding.opensecretsanta.domain.notification.RegisteredDevice
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeDevices : DeviceRepository {
    val all = mutableListOf<RegisteredDevice>()

    override fun register(username: Username, token: String, platform: DevicePlatform, atMillis: Long) {
        all.removeAll { it.token == token }
        all += RegisteredDevice(token, username, platform)
    }

    override fun remove(username: Username, token: String) {
        all.removeAll { it.token == token && it.username == username }
    }

    override fun removeInvalid(tokens: Collection<String>) {
        all.removeAll { it.token in tokens }
    }

    override fun devicesOf(usernames: Collection<Username>) = all.filter { it.username in usernames }
}

class FakeSettings : NotificationSettingsRepository {
    val saved = mutableMapOf<Username, NotificationSettings>()

    override fun find(username: Username) = saved[username] ?: NotificationSettings()
    override fun findAll(usernames: Collection<Username>) = usernames.associateWith { find(it) }
    override fun save(username: Username, settings: NotificationSettings) {
        saved[username] = settings
    }
}

class RecordingPushSender(override var enabled: Boolean = true) : PushSender {
    val sent = mutableListOf<PushMessage>()
    var results: (PushMessage) -> PushResult = { PushResult.Sent }
    var failWith: RuntimeException? = null

    override fun send(messages: List<PushMessage>): List<PushResult> {
        failWith?.let { throw it }
        sent += messages
        return messages.map(results)
    }
}

class NotifierTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val settings = FakeSettings()
    private val devices = FakeDevices()
    private val sender = RecordingPushSender()
    private val notifier = Notifier(settings, devices, sender, groups)

    private val names = listOf("alice", "bob", "carol", "dave")
    private val owner = users.add("boss", admin = true).let { users.userNamed("boss") }

    init {
        for (name in names) users.add(name)
    }

    // identity shuffle: alice gives to bob, bob to carol, carol to dave, dave to alice
    private fun group(state: GroupState): Group {
        val seeded = groups.seed(owner.username, GroupState.PENDING, *names.map(::u).toTypedArray())
        if (state == GroupState.ACTIVE) {
            ActivateGroup(OwnedGroupLoader(groups), groups, AssignmentPlanner(IdentityShuffler()), GroupViewer(users))(owner, seeded.id)
        }
        return groups.find(seeded.id)!!
    }

    private fun register(name: String, token: String = "$name-phone", platform: DevicePlatform = DevicePlatform.ANDROID) =
        devices.register(u(name), token, platform, 1)

    private fun tokens() = sender.sent.map { it.token }.sorted()

    @Test
    fun aGroupBecomingVisibleTellsTheRecipientsExceptTheActor() {
        val g = group(GroupState.PENDING)
        names.forEach { register(it) }
        register("boss")

        notifier.groupVisible(g, g.participants + u("boss"), actor = u("boss"))

        assertEquals(names.map { "$it-phone" }.sorted(), tokens())
        val message = sender.sent.first()
        assertEquals(NotificationCategory.ANNOUNCEMENTS, message.category)
        assertEquals(g.id, message.groupId)
        assertTrue("\"${g.details.name}\"" in message.body)
    }

    @Test
    fun theDrawTellsEveryParticipantButTheActorAndNamesNoOne() {
        val g = group(GroupState.ACTIVE)
        names.forEach { register(it) }

        notifier.groupDrawn(g, actor = u("alice"))

        assertEquals(listOf("bob-phone", "carol-phone", "dave-phone"), tokens())
        for (message in sender.sent) {
            assertEquals(NotificationCategory.ANNOUNCEMENTS, message.category)
            assertTrue("drawn" in message.body)
            assertTrue(names.none { it in message.body + message.title }, "no username in ${message.body}")
        }
    }

    @Test
    fun aPersonWithSeveralDevicesGetsOneMessageForEach() {
        val g = group(GroupState.ACTIVE)
        register("bob", "bob-phone")
        register("bob", "bob-browser", DevicePlatform.WEB)

        notifier.questionAsked(g.id, u("alice"))

        assertEquals(listOf("bob-browser", "bob-phone"), tokens())
    }

    @Test
    fun aSwitchedOffKindSendsNothingToThatPersonOnly() {
        val g = group(GroupState.PENDING)
        names.forEach { register(it) }
        settings.save(u("carol"), NotificationSettings(announcements = false, questions = false))

        notifier.groupVisible(g, g.participants, actor = u("boss"))

        assertEquals(listOf("alice-phone", "bob-phone", "dave-phone"), tokens())
    }

    @Test
    fun otherSwitchesDoNotAffectAKind() {
        val g = group(GroupState.ACTIVE)
        register("bob")
        settings.save(u("bob"), NotificationSettings(announcements = false, wishlist = false, answers = false))

        notifier.questionAsked(g.id, u("alice"))

        assertEquals(listOf("bob-phone"), tokens())
    }

    @Test
    fun aWishlistChangeTellsTheSantaOfTheEditor() {
        val g = group(GroupState.ACTIVE)
        names.forEach { register(it) }

        notifier.wishlistChanged(g.id, editor = u("bob"))

        assertEquals(listOf("alice-phone"), tokens())
        assertEquals(NotificationCategory.WISHLIST, sender.sent.single().category)
    }

    @Test
    fun aWishlistChangeBeforeTheDrawTellsNobody() {
        val g = group(GroupState.PENDING)
        names.forEach { register(it) }

        notifier.wishlistChanged(g.id, editor = u("bob"))

        assertEquals(emptyList(), sender.sent)
    }

    @Test
    fun aQuestionTellsTheReceiverAndAnAnswerTellsTheSanta() {
        val g = group(GroupState.ACTIVE)
        names.forEach { register(it) }

        notifier.questionAsked(g.id, santa = u("alice"))
        notifier.questionAnswered(g.id, receiver = u("bob"))

        assertEquals(listOf("bob-phone" to NotificationCategory.QUESTIONS, "alice-phone" to NotificationCategory.ANSWERS), sender.sent.map { it.token to it.category })
    }

    @Test
    fun noTextNamesTheSantaOrCarriesPrivateContent() {
        val g = group(GroupState.ACTIVE)
        names.forEach { register(it) }

        notifier.questionAsked(g.id, u("alice"))
        notifier.questionAnswered(g.id, u("bob"))
        notifier.wishlistChanged(g.id, u("bob"))
        notifier.groupDrawn(g, u("boss"))

        assertEquals(7, sender.sent.size)
        for (message in sender.sent) {
            for (name in names + "boss") assertFalse(name in message.body + message.title, "${message.body} names $name")
            assertTrue("\"${g.details.name}\"" in message.body)
        }
    }

    @Test
    fun aMessageIsTheSameForWhoeverGetsIt() {
        val g = group(GroupState.ACTIVE)
        names.forEach { register(it) }

        notifier.groupDrawn(g, u("boss"))

        assertEquals(1, sender.sent.map { it.body }.toSet().size)
    }

    @Test
    fun aGroupNameStaysOnOneLine() {
        val g = group(GroupState.PENDING).let { groups.updateDetails(it.id, GroupDetails("Office\nParty")) }
        register("alice")

        notifier.groupVisible(g, listOf(u("alice")), actor = u("boss"))

        assertTrue("\n" !in sender.sent.single().body)
        assertTrue("Office Party" in sender.sent.single().body)
    }

    @Test
    fun deviceTheServiceSaysIsGoneIsForgottenAndOthersKeepTheirs() {
        val g = group(GroupState.ACTIVE)
        register("bob", "dead")
        register("bob", "alive")
        sender.results = { if (it.token == "dead") PushResult.InvalidToken else PushResult.Sent }

        notifier.questionAsked(g.id, u("alice"))

        assertEquals(listOf("alive"), devices.all.map { it.token })
    }

    @Test
    fun aFailedSendKeepsTheDevice() {
        val g = group(GroupState.ACTIVE)
        register("bob")
        sender.results = { PushResult.Failed }

        notifier.questionAsked(g.id, u("alice"))

        assertEquals(listOf("bob-phone"), devices.all.map { it.token })
    }

    @Test
    fun withoutAPushServiceNothingIsLookedUpOrSent() {
        val g = group(GroupState.ACTIVE)
        register("bob")
        sender.enabled = false

        notifier.questionAsked(g.id, u("alice"))

        assertEquals(emptyList(), sender.sent)
    }

    @Test
    fun aFailureNeverEscapes() {
        val g = group(GroupState.ACTIVE)
        register("bob")
        sender.failWith = IllegalStateException("push service exploded")

        notifier.questionAsked(g.id, u("alice"))
        notifier.groupDrawn(g, u("boss"))
    }

    @Test
    fun anUnknownGroupOrMissingPairingTellsNobody() {
        val g = group(GroupState.PENDING)
        names.forEach { register(it) }

        notifier.questionAsked(9999, u("alice"))
        notifier.questionAsked(g.id, u("alice")) // not drawn: no receiver yet
        notifier.questionAnswered(g.id, u("bob"))

        assertEquals(emptyList(), sender.sent)
    }
}
