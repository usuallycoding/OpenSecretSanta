package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.domain.notification.Notifier
import com.usuallycoding.opensecretsanta.domain.notification.PushMessage
import com.usuallycoding.opensecretsanta.domain.notification.PushResult
import com.usuallycoding.opensecretsanta.domain.notification.PushSender
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteDeviceRepository
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteNotificationSettingsRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import com.usuallycoding.opensecretsanta.server.notification.Cancellation
import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import com.usuallycoding.opensecretsanta.server.notification.Scheduler
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Scheduled tasks that run only when a test says so, in the order of their due time. */
class ManualScheduler : Scheduler {
    class Job(val dueMillis: Long, val task: Runnable) {
        var cancelled = false
    }

    val jobs = mutableListOf<Job>()
    var nowMillis = 0L

    override fun schedule(delayMillis: Long, task: Runnable): Cancellation {
        val job = Job(nowMillis + delayMillis, task)
        jobs += job
        return Cancellation { job.cancelled = true }
    }

    /** Moves time forward and runs what has become due. */
    fun advance(millis: Long) {
        nowMillis += millis
        jobs.filter { !it.cancelled && it.dueMillis <= nowMillis }.sortedBy { it.dueMillis }.forEach {
            jobs.remove(it)
            it.task.run()
        }
    }

    val pending: Int get() = jobs.count { !it.cancelled }
}

class RecordingSender : PushSender {
    override val enabled = true
    val sent = mutableListOf<PushMessage>()
    override fun send(messages: List<PushMessage>): List<PushResult> {
        sent += messages
        return messages.map { PushResult.Sent }
    }
}

class NotificationDispatcherTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val groups = SqliteGroupRepository(database)
    private val devices = SqliteDeviceRepository(database)
    private val sender = RecordingSender()
    private val scheduler = ManualScheduler()
    private val clock = Clock { scheduler.nowMillis }
    private val direct = Executor { it.run() }

    private val alice = Username.parse("alice")
    private val bob = Username.parse("bob")
    private val carol = Username.parse("carol")
    private val boss = Username.parse("boss")

    private fun dispatcher(executor: Executor = direct) =
        NotificationDispatcher(Notifier(SqliteNotificationSettingsRepository(database), devices, sender, groups), executor, scheduler, clock)

    private fun group(pending: Boolean = false): Long {
        SqliteUserRepository(database).insertAll(listOf(alice, bob, carol, boss).map { NewUser(it, "h", null, false, it == boss) })
        val g = groups.create(boss, GroupDetails("Party"), listOf(alice, bob, carol))
        groups.moveToPending(g.id, emptyList())
        if (!pending) groups.activate(g.id, listOf(Assignment(alice, bob), Assignment(bob, carol), Assignment(carol, alice)))
        listOf(alice, bob, carol).forEach { devices.register(it, "${it.value}-phone", DevicePlatform.ANDROID, 1) }
        return g.id
    }

    private fun bodies() = sender.sent.map { it.token to it.category }

    @Test
    fun threeQuickChangesMakeOneNotificationAfterTheQuietPeriod() {
        val id = group()
        val dispatcher = dispatcher()

        dispatcher.wishlistChanged(id, bob)
        scheduler.advance(10_000)
        dispatcher.wishlistChanged(id, bob)
        scheduler.advance(10_000)
        dispatcher.wishlistChanged(id, bob)
        scheduler.advance(59_000)
        assertEquals(emptyList(), sender.sent, "still inside the quiet period")

        scheduler.advance(2_000)

        assertEquals(listOf("alice-phone" to NotificationCategory.WISHLIST), bodies())
    }

    @Test
    fun aLaterChangeAfterTheNotificationStartsANewRound() {
        val id = group()
        val dispatcher = dispatcher()

        dispatcher.wishlistChanged(id, bob)
        scheduler.advance(61_000)
        dispatcher.wishlistChanged(id, bob)
        scheduler.advance(61_000)

        assertEquals(2, sender.sent.size)
    }

    @Test
    fun aListThatIsNeverQuietIsStillAnnouncedWithinFiveMinutes() {
        val id = group()
        val dispatcher = dispatcher()

        repeat(12) {
            dispatcher.wishlistChanged(id, bob)
            scheduler.advance(30_000) // never a full quiet minute
        }

        assertTrue(sender.sent.isNotEmpty(), "sent despite the changes going on")
        assertEquals("alice-phone", sender.sent.first().token)
    }

    @Test
    fun differentPeopleAreHeldSeparately() {
        val id = group()
        val dispatcher = dispatcher()

        dispatcher.wishlistChanged(id, bob)
        dispatcher.wishlistChanged(id, carol)
        scheduler.advance(61_000)

        assertEquals(listOf("alice-phone", "bob-phone"), sender.sent.map { it.token }.sorted())
    }

    @Test
    fun aChangeBeforeTheDrawNotifiesNobody() {
        val id = group(pending = true)

        dispatcher().wishlistChanged(id, bob)
        scheduler.advance(61_000)

        assertEquals(emptyList(), sender.sent)
    }

    @Test
    fun questionsAndAnswersAreNotHeldBack() {
        val id = group()
        val dispatcher = dispatcher()

        dispatcher.questionAsked(id, alice)
        dispatcher.questionAnswered(id, bob)

        assertEquals(listOf("bob-phone" to NotificationCategory.QUESTIONS, "alice-phone" to NotificationCategory.ANSWERS), bodies())
    }

    @Test
    fun theWorkIsHandedToTheExecutorNotDoneByTheCaller() {
        val id = group()
        val queued = mutableListOf<Runnable>()
        val dispatcher = dispatcher(executor = { queued += it })

        dispatcher.questionAsked(id, alice)

        assertEquals(emptyList(), sender.sent, "nothing done on the calling thread")
        queued.forEach { it.run() }
        assertEquals(1, sender.sent.size)
    }

    @Test
    fun anExecutorThatRefusesOrAWorkerThatThrowsNeverReachesTheCaller() {
        val id = group()

        dispatcher(executor = { throw java.util.concurrent.RejectedExecutionException("shut down") }).questionAsked(id, alice)
        dispatcher(executor = { it.run() }).questionAsked(9999, alice)
        dispatcher(executor = { throw IllegalStateException("boom") }).groupDrawn(groups.find(id)!!, boss)
    }
}
