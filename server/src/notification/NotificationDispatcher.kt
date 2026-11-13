package com.usuallycoding.opensecretsanta.server.notification

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.notification.Notifier
import com.usuallycoding.opensecretsanta.domain.notification.PushPlan
import com.usuallycoding.opensecretsanta.domain.user.Username
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** A scheduled task that can still be called off. */
fun interface Cancellation {
    fun cancel()
}

/** Runs a task once, after a delay; replaceable by a manual one in tests. */
fun interface Scheduler {
    fun schedule(delayMillis: Long, task: Runnable): Cancellation
}

class ThreadScheduler(private val executor: ScheduledExecutorService = daemonScheduler()) : Scheduler {
    override fun schedule(delayMillis: Long, task: Runnable): Cancellation {
        val future = executor.schedule(task, delayMillis, TimeUnit.MILLISECONDS)
        return Cancellation { future.cancel(false) }
    }

    private companion object {
        fun daemonScheduler(): ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "wishlist-notifications").apply { isDaemon = true } }
    }
}

/** One thread, one notification at a time and in order, that does not keep the server from stopping. */
fun daemonSingleThreadExecutor(name: String = "notifications"): Executor =
    Executors.newSingleThreadExecutor { r -> Thread(r, name).apply { isDaemon = true } }

/**
 * What the routes call after an action has been stored. Every notification is worked out and sent by [executor], so
 * that a slow push service never delays a response and nothing it does can fail a request. Announcements are sent at
 * once. A change to a wishlist, a question, an answer or a letter only marks the person it is for, and [startDigest] sends
 * the daily digest to everyone marked at [DIGEST_HOUR]:00 in Bucharest, then schedules the next one. The marks are in the
 * database; a digest due while the server was down is not made up.
 */
class NotificationDispatcher(
    private val notifier: Notifier,
    private val executor: Executor,
    private val scheduler: Scheduler,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(NotificationDispatcher::class.java)

    fun groupVisible(group: Group, recipients: Collection<Username>, actor: Username) =
        run { notifier.groupVisible(group, recipients, actor) }

    fun groupDrawn(group: Group, actor: Username) = run { notifier.groupDrawn(group, actor) }

    /** Sends the push notifications of an announcement, already planned and counted by the caller. */
    fun announce(plan: PushPlan) = run { notifier.send(plan) }

    fun questionAsked(groupId: Long, santa: Username) = run { notifier.questionAsked(groupId, santa) }

    fun questionAnswered(groupId: Long, receiver: Username) = run { notifier.questionAnswered(groupId, receiver) }

    fun letterSent(groupId: Long, receiver: Username) = run { notifier.letterSent(groupId, receiver) }

    fun wishlistChanged(groupId: Long, editor: Username) = run { notifier.wishlistChanged(groupId, editor) }

    /** Schedules the next daily digest; once it has been sent, the one after it is scheduled. */
    fun startDigest() {
        try {
            scheduler.schedule(millisUntilNextDigest(clock.nowMillis())) {
                run { notifier.sendDigest() }
                startDigest()
            }
        } catch (e: Exception) {
            log.warn("The daily digest could not be scheduled ({})", e::class.simpleName)
        }
    }

    private fun run(work: () -> Unit) {
        try {
            executor.execute {
                try {
                    work()
                } catch (e: Exception) {
                    log.warn("Sending a notification failed ({})", e::class.simpleName)
                }
            }
        } catch (e: Exception) {
            log.warn("A notification could not be queued ({})", e::class.simpleName)
        }
    }

    companion object {
        /** The hour of the day, in Bucharest, at which the daily digest goes out. */
        const val DIGEST_HOUR = 20

        private val BUCHAREST = ZoneId.of("Europe/Bucharest")

        /** How long from [nowMillis] to the next [DIGEST_HOUR]:00 in Bucharest, which is a day later when it is that time already. */
        fun millisUntilNextDigest(nowMillis: Long): Long {
            val now = Instant.ofEpochMilli(nowMillis).atZone(BUCHAREST)
            var next = now.toLocalDate().atTime(DIGEST_HOUR, 0).atZone(BUCHAREST)
            if (!next.toInstant().isAfter(now.toInstant())) next = now.toLocalDate().plusDays(1).atTime(DIGEST_HOUR, 0).atZone(BUCHAREST)
            return next.toInstant().toEpochMilli() - nowMillis
        }
    }
}
