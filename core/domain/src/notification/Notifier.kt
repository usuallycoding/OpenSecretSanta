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

    private fun deliver(category: NotificationCategory, recipients: List<Username>, groupId: Long, body: String) {
        if (!sender.enabled || recipients.isEmpty()) return
        val wanting = settings.findAll(recipients).filterValues { it.allows(category) }.keys
        val messages = devices.devicesOf(wanting).map { PushMessage(it.token, category, groupId, NotificationTexts.TITLE, body) }
        if (messages.isEmpty()) return
        val results = sender.send(messages)
        if (results.size != messages.size) return
        val gone = messages.zip(results).filter { it.second == PushResult.InvalidToken }.map { it.first.token }
        if (gone.isNotEmpty()) devices.removeInvalid(gone)
    }

    /** A group name goes onto a lock screen on one line. */
    private fun name(group: Group): String = "\"" + group.details.name.replace(Regex("[\\r\\n]+"), " ") + "\""

    private inline fun attempt(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            // nobody is told; the action that led here is already stored
        }
    }
}
