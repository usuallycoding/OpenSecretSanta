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
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteDigestMarkRepository
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

    private val marks = SqliteDigestMarkRepository(database)

    private fun notifier() = Notifier(SqliteNotificationSettingsRepository(database), devices, sender, groups, marks, clock)

    private fun dispatcher(executor: Executor = direct) = NotificationDispatcher(notifier(), executor, scheduler, clock)

    private fun group(pending: Boolean = false): Long {
        SqliteUserRepository(database).insertAll(listOf(alice, bob, carol, boss).map { NewUser(it, "h", null, false, it == boss) })
        val g = groups.create(boss, GroupDetails("Party"), listOf(alice, bob, carol))
        groups.moveToPending(g.id, emptyList())
        if (!pending) groups.activate(g.id, listOf(Assignment(alice, bob), Assignment(bob, carol), Assignment(carol, alice)))
        listOf(alice, bob, carol).forEach { devices.register(it, "${it.value}-phone", DevicePlatform.ANDROID, 1) }
        return g.id
    }

    private fun sent() = sender.sent.map { it.token to it.category }

    private val hour = 60 * 60 * 1000L

    @Test
    fun changesMarkThePersonTheyAreForAndSendNothing() {
        val id = group()
        val dispatcher = dispatcher()

        dispatcher.wishlistChanged(id, bob)
        dispatcher.questionAsked(id, alice)
        dispatcher.questionAnswered(id, carol)
        dispatcher.letterSent(id, bob)
        scheduler.advance(10 * 60 * 1000)

        assertEquals(emptyList(), sender.sent)
        // alice's receiver is bob, bob's is carol, carol's is alice: the wishlist and the letter mark alice, the question bob, the answer bob
        assertEquals(listOf(alice, bob), marks.takeAll().sortedBy { it.value })
    }

    @Test
    fun aChangeBeforeTheDrawMarksNobody() {
        val id = group(pending = true)
        val dispatcher = dispatcher()

        dispatcher.wishlistChanged(id, bob)
        dispatcher.questionAsked(id, alice)
        dispatcher.letterSent(id, bob)

        assertEquals(emptyList(), marks.takeAll())
    }

    @Test
    fun theDigestGoesOutAtTwentyHundredInBucharestAndOnlyToMarkedPeople() {
        val id = group()
        val dispatcher = dispatcher()
        dispatcher.startDigest()
        dispatcher.wishlistChanged(id, bob) // marks alice

        // the manual clock starts at the epoch, 02:00 in Bucharest in winter, so 20:00 is 18 hours away
        scheduler.advance(18 * hour - 1)
        assertEquals(emptyList(), sender.sent, "not before 20:00")
        scheduler.advance(1)

        assertEquals(listOf("alice-phone" to NotificationCategory.DAILY), sent())
        assertEquals(null, sender.sent.single().groupId)
        assertEquals(emptyList(), marks.takeAll(), "the mark is cleared")
    }

    @Test
    fun theNextDigestIsScheduledAfterEachOneAndNothingIsSentWithoutNews() {
        val id = group()
        val dispatcher = dispatcher()
        dispatcher.startDigest()
        dispatcher.wishlistChanged(id, bob)
        scheduler.advance(18 * hour)
        assertEquals(1, sender.sent.size)

        scheduler.advance(24 * hour)
        assertEquals(1, sender.sent.size, "no news, no digest")

        dispatcher.questionAsked(id, alice) // marks bob
        scheduler.advance(24 * hour)
        assertEquals(listOf("alice-phone" to NotificationCategory.DAILY, "bob-phone" to NotificationCategory.DAILY), sent())
        assertEquals(1, scheduler.pending, "the one after it is waiting")
    }

    @Test
    fun marksSurviveARestartAndAreSentAtTheNextDigest() {
        val id = group()
        dispatcher().wishlistChanged(id, bob)

        val restarted = dispatcher()
        restarted.startDigest()
        scheduler.advance(18 * hour)

        assertEquals(listOf("alice-phone" to NotificationCategory.DAILY), sent())
    }

    @Test
    fun theTimeToTheNextDigestFollowsBucharestTime() {
        fun hoursFrom(utc: String) = NotificationDispatcher.millisUntilNextDigest(java.time.Instant.parse(utc).toEpochMilli()) / hour.toDouble()

        assertEquals(8.0, hoursFrom("2026-12-01T10:00:00Z"), "winter, before 20:00")
        assertEquals(24.0, hoursFrom("2026-12-01T18:00:00Z"), "exactly 20:00 means tomorrow's")
        assertEquals(7.0, hoursFrom("2026-07-01T10:00:00Z"), "summer, 20:00 is 17:00 UTC")
        assertEquals(22.0, hoursFrom("2026-03-28T19:00:00Z"), "the night the clocks go forward")
        assertEquals(24.0, hoursFrom("2026-10-24T18:00:00Z"), "the night the clocks go back")
    }

    @Test
    fun aSchedulerThatRefusesFailsNothing() {
        val id = group()
        val refusing = Scheduler { _, _ -> throw java.util.concurrent.RejectedExecutionException("shut down") }
        val dispatcher = NotificationDispatcher(notifier(), direct, refusing, clock)

        dispatcher.startDigest()
        dispatcher.questionAsked(id, alice)
        dispatcher.questionAnswered(id, bob)
        dispatcher.wishlistChanged(id, bob)

        assertEquals(emptyList(), sender.sent)
    }

    @Test
    fun theWorkIsHandedToTheExecutorNotDoneByTheCaller() {
        val id = group()
        val queued = mutableListOf<Runnable>()
        val dispatcher = dispatcher(executor = { queued += it })

        dispatcher.questionAsked(id, alice)

        assertEquals(emptyList(), marks.takeAll(), "nothing done on the calling thread")
        dispatcher.questionAsked(id, alice)
        queued.forEach { it.run() }
        assertEquals(listOf(bob), marks.takeAll())
    }

    @Test
    fun anExecutorThatRefusesOrAWorkerThatThrowsNeverReachesTheCaller() {
        val id = group()

        dispatcher(executor = { throw java.util.concurrent.RejectedExecutionException("shut down") }).questionAsked(id, alice)
        dispatcher(executor = { it.run() }).questionAsked(9999, alice)
        dispatcher(executor = { throw IllegalStateException("boom") }).groupDrawn(groups.find(id)!!, boss)
    }
}
