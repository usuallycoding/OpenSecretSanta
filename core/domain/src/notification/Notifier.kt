package com.usuallycoding.opensecretsanta.domain.notification

import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.Username

/** The words of the notifications. They are the same for everyone and say nothing about who is whose Santa. */
object NotificationTexts {
    const val TITLE = "Open Secret Santa"

    fun groupVisible(group: String) = "You are in a new group: $group"
    fun groupDrawn(group: String) = "The names for $group have been drawn"
    fun wishlistChanged(group: String) = "Your receiver changed their wishlist in $group"
    fun questionAsked(group: String) = "Your Secret Santa has a question for you in $group"
    fun questionAnswered(group: String) = "Your receiver answered a question in $group"
}

/**
 * Works out who a notification is for, drops those who switched that kind off, and sends it to their devices. It runs
 * after the action that caused it was stored and never throws: a failure only means that nobody is told. Devices the
 * push service says are gone are forgotten.
 *
 * Nothing it sends carries a question, an answer, a wishlist item or the name of a Santa; only the group's name.
 */
class Notifier(
    private val settings: NotificationSettingsRepository,
    private val devices: DeviceRepository,
    private val sender: PushSender,
    private val groups: GroupRepository,
) {
    /** [recipients] can now see [group]; [actor], who made it so, is not told. */
    fun groupVisible(group: Group, recipients: Collection<Username>, actor: Username) =
        attempt {
            deliver(NotificationCategory.ANNOUNCEMENTS, recipients.filter { it != actor }, group.id, NotificationTexts.groupVisible(name(group)))
        }

    fun groupDrawn(group: Group, actor: Username) =
        attempt {
            deliver(NotificationCategory.ANNOUNCEMENTS, group.participants.filter { it != actor }, group.id, NotificationTexts.groupDrawn(name(group)))
        }

    /** [editor] changed their wishlist: their Santa is told, once the group has been drawn. */
    fun wishlistChanged(groupId: Long, editor: Username) =
        attempt {
            val group = groups.find(groupId)?.takeIf { it.state == GroupState.ACTIVE } ?: return@attempt
            val santa = groups.santaOf(groupId, editor) ?: return@attempt
            deliver(NotificationCategory.WISHLIST, listOf(santa), groupId, NotificationTexts.wishlistChanged(name(group)))
        }

    /** [santa] asked a question: their receiver is told. */
    fun questionAsked(groupId: Long, santa: Username) =
        attempt {
            val group = groups.find(groupId) ?: return@attempt
            val receiver = groups.receiverOf(groupId, santa) ?: return@attempt
            deliver(NotificationCategory.QUESTIONS, listOf(receiver), groupId, NotificationTexts.questionAsked(name(group)))
        }

    /** [receiver] answered a question: their Santa is told. */
    fun questionAnswered(groupId: Long, receiver: Username) =
        attempt {
            val group = groups.find(groupId) ?: return@attempt
            val santa = groups.santaOf(groupId, receiver) ?: return@attempt
            deliver(NotificationCategory.ANSWERS, listOf(santa), groupId, NotificationTexts.questionAnswered(name(group)))
        }

    /** Whether a push service is set up at all. */
    val available: Boolean get() = sender.enabled

    /**
     * Works out the push notifications of an announcement to the participants of [group] other than [author]: one for
     * each device of everyone who has the announcements switch on. Nothing is sent; see [send].
     */
    fun planAnnouncement(group: Group, title: String, text: String, author: Username): PushPlan {
        val others = group.participants.filter { it != author }
        if (!available || others.isEmpty()) return PushPlan(emptyList(), reached = 0, notReached = others.size)
        val wanting = settings.findAll(others).filterValues { it.allows(NotificationCategory.ANNOUNCEMENTS) }.keys
        val owned = devices.devicesOf(wanting)
        val body = cut(text)
        val heading = "${oneLine(title)}: ${oneLine(group.details.name)}"
        val messages = owned.map { PushMessage(it.token, NotificationCategory.ANNOUNCEMENTS, group.id, heading, body) }
        val reached = owned.map { it.username }.toSet().size
        return PushPlan(messages, reached = reached, notReached = others.size - reached)
    }

    /** Sends a plan's messages and forgets devices the push service says are gone; never throws. */
    fun send(plan: PushPlan) = attempt { dispatch(plan.messages) }

    private fun cut(text: String): String = if (text.length <= PUSH_TEXT_LIMIT) text else text.take(PUSH_TEXT_LIMIT).trimEnd() + "\u2026"

    private fun deliver(category: NotificationCategory, recipients: List<Username>, groupId: Long, body: String) {
        if (!sender.enabled || recipients.isEmpty()) return
        val wanting = settings.findAll(recipients).filterValues { it.allows(category) }.keys
        dispatch(devices.devicesOf(wanting).map { PushMessage(it.token, category, groupId, NotificationTexts.TITLE, body) })
    }

    private fun dispatch(messages: List<PushMessage>) {
        if (!sender.enabled || messages.isEmpty()) return
        val results = sender.send(messages)
        if (results.size != messages.size) return
        val gone = messages.zip(results).filter { it.second == PushResult.InvalidToken }.map { it.first.token }
        if (gone.isNotEmpty()) devices.removeInvalid(gone)
    }

    /** A group name goes onto a lock screen on one line. */
    private fun name(group: Group): String = "\"" + group.details.name.replace(Regex("[\\r\\n]+"), " ") + "\""

    private fun oneLine(text: String): String = text.replace(Regex("[\\r\\n]+"), " ")

    private inline fun attempt(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            // nobody is told; the action that led here is already stored
        }
    }

    companion object {
        /** Push messages are data messages, which Firebase limits to about 4KB: the text is cut well short of that. */
        const val PUSH_TEXT_LIMIT = 300
    }
}

/** The push notifications of an announcement: the messages to send, how many people they are for and how many others cannot be told. */
class PushPlan(val messages: List<PushMessage>, val reached: Int, val notReached: Int)
