package com.usuallycoding.opensecretsanta.server.notification

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.notification.Notifier
import com.usuallycoding.opensecretsanta.domain.notification.PushPlan
import com.usuallycoding.opensecretsanta.domain.user.Username
import org.slf4j.LoggerFactory
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
 * that a slow push service never delays a response and nothing it does can fail a request. Wishlist changes, questions,
 * answers and letters are held back until the person has been quiet for [quietMillis] (but at most [maxWaitMillis] after the
 * first of them), so that a list being filled in, or a run of questions, answers or letters, makes one notification. Each kind
 * of person, group and action is held on its own, and the number held is passed on so that the text can say "questions".
 * The held ones live in memory: a restart forgets them.
 */
class NotificationDispatcher(
    private val notifier: Notifier,
    private val executor: Executor,
    private val scheduler: Scheduler,
    private val clock: Clock,
    private val quietMillis: Long = QUIET_MILLIS,
    private val maxWaitMillis: Long = MAX_WAIT_MILLIS,
) {
    private val log = LoggerFactory.getLogger(NotificationDispatcher::class.java)

    /** What is held for: a person's wishlist changes, the questions they ask, the answers they give, or the letters they send. */
    private enum class Kind { WISHLIST, QUESTION, ANSWER, LETTER }

    private class Held(val firstAtMillis: Long, val timer: Cancellation, val count: Int)

    private val held = HashMap<Triple<Kind, Long, Username>, Held>()

    fun groupVisible(group: Group, recipients: Collection<Username>, actor: Username) =
        run { notifier.groupVisible(group, recipients, actor) }

    fun groupDrawn(group: Group, actor: Username) = run { notifier.groupDrawn(group, actor) }

    /** Sends the push notifications of an announcement, already planned and counted by the caller. */
    fun announce(plan: PushPlan) = run { notifier.send(plan) }

    fun questionAsked(groupId: Long, santa: Username) =
        hold(Kind.QUESTION, groupId, santa) { count -> notifier.questionAsked(groupId, santa, count) }

    fun questionAnswered(groupId: Long, receiver: Username) =
        hold(Kind.ANSWER, groupId, receiver) { count -> notifier.questionAnswered(groupId, receiver, count) }

    fun letterSent(groupId: Long, receiver: Username) =
        hold(Kind.LETTER, groupId, receiver) { count -> notifier.letterSent(groupId, receiver, count) }

    fun wishlistChanged(groupId: Long, editor: Username) =
        hold(Kind.WISHLIST, groupId, editor) { notifier.wishlistChanged(groupId, editor) }

    /**
     * Notes one more [kind] event of [actor] in the group and restarts the quiet period, never letting the notification wait
     * more than [maxWaitMillis] after the first; when it is due, [send] is given how many events it stands for.
     */
    private fun hold(kind: Kind, groupId: Long, actor: Username, send: (count: Int) -> Unit) {
        val key = Triple(kind, groupId, actor)
        synchronized(held) {
            val now = clock.nowMillis()
            val previous = held.remove(key)
            previous?.timer?.cancel()
            val first = previous?.firstAtMillis ?: now
            val count = (previous?.count ?: 0) + 1
            val delay = minOf(quietMillis, maxOf(0L, first + maxWaitMillis - now))
            val timer = try {
                scheduler.schedule(delay) {
                    synchronized(held) { held.remove(key) }
                    run { send(count) }
                }
            } catch (e: Exception) {
                log.warn("A {} notification could not be scheduled ({})", kind.name.lowercase(), e::class.simpleName)
                return
            }
            held[key] = Held(first, timer, count)
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
        const val QUIET_MILLIS = 60_000L
        const val MAX_WAIT_MILLIS = 300_000L
    }
}
