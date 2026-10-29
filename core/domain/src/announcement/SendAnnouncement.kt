package com.usuallycoding.opensecretsanta.domain.announcement

import com.usuallycoding.opensecretsanta.domain.email.EmailMessage
import com.usuallycoding.opensecretsanta.domain.email.EmailSummary
import com.usuallycoding.opensecretsanta.domain.email.Mailer
import com.usuallycoding.opensecretsanta.domain.email.announcementEmail
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.notification.Notifier
import com.usuallycoding.opensecretsanta.domain.notification.PushPlan
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

/** Which ways of reaching people this server has been set up for. */
class Channels(private val mailer: Mailer, private val notifier: Notifier) {
    val emailAvailable: Boolean get() = mailer.enabled
    val pushAvailable: Boolean get() = notifier.available
}

/** What an announcement came to: a summary for each channel used, and the push messages still to be sent. */
class AnnouncementOutcome(val emails: EmailSummary?, val pushPlan: PushPlan?)

/**
 * The owner of a Pending or Active group tells its participants something, by email, by push notification or both. The
 * emails are sent here; the push notifications are only planned, so that the caller can send them in the background.
 * Everything is checked before anything is sent, and a failing email never keeps the other channel from going out.
 */
class SendAnnouncement(
    private val loadOwned: OwnedGroupLoader,
    private val users: UserRepository,
    private val mailer: Mailer,
    private val notifier: Notifier,
    private val appUrl: String? = null,
) {
    operator fun invoke(actor: User, groupId: Long, rawTitle: String, rawBody: String, email: Boolean, push: Boolean): AnnouncementOutcome {
        val group = loadOwned(actor, groupId)
        val title = rawTitle.trim()
        val body = rawBody.trim()
        if (title.isEmpty() || title.length > MAX_TITLE) throw DomainException.ValidationFailed("title", "Title must be 1 to $MAX_TITLE characters long.")
        if ('\n' in title || '\r' in title) throw DomainException.ValidationFailed("title", "Title must be on one line.")
        if (body.isEmpty() || body.length > MAX_BODY) throw DomainException.ValidationFailed("body", "Message must be 1 to $MAX_BODY characters long.")
        if (!email && !push) throw DomainException.ValidationFailed("channels", "Choose email, push notification or both.")
        if (group.state != GroupState.PENDING && group.state != GroupState.ACTIVE) {
            throw DomainException.InvalidState("Announcements can only be sent to a pending or active group.")
        }
        if (email && !mailer.enabled) throw DomainException.InvalidState("Email is not set up on this server.")
        if (push && !notifier.available) throw DomainException.InvalidState("Push notifications are not set up on this server.")

        val summary = if (email) sendEmails(group.participants, group.details, title, body) else null
        val plan = if (push) {
            try {
                notifier.planAnnouncement(group, title, body, actor.username)
            } catch (_: Exception) {
                PushPlan(emptyList(), reached = 0, notReached = group.participants.count { it != actor.username })
            }
        } else {
            null
        }
        return AnnouncementOutcome(summary, plan)
    }

    private fun sendEmails(
        participants: List<Username>,
        details: GroupDetails,
        title: String,
        body: String,
    ): EmailSummary {
        val accounts = try {
            users.findAll(participants).associateBy { it.username }
        } catch (_: Exception) {
            return EmailSummary(sent = 0, failed = participants.size, noAddress = 0)
        }
        var noAddress = 0
        val messages = ArrayList<EmailMessage>()
        for (participant in participants) {
            val address = accounts[participant]?.profile?.email
            if (address == null) noAddress++ else messages += announcementEmail(address, participant, details, title, body, appUrl)
        }
        val results = try {
            mailer.send(messages).let { if (it.size == messages.size) it else messages.map { false } }
        } catch (_: Exception) {
            messages.map { false }
        }
        return EmailSummary(sent = results.count { it }, failed = results.count { !it }, noAddress = noAddress)
    }

    companion object {
        const val MAX_TITLE = 100
        const val MAX_BODY = 2000
    }
}
