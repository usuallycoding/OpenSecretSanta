package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.AnnouncementDto
import com.usuallycoding.opensecretsanta.contract.AnnouncementRequest
import com.usuallycoding.opensecretsanta.contract.AnnouncementResponse
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.contract.MarkReadRequest
import com.usuallycoding.opensecretsanta.contract.ReadMarkDto
import com.usuallycoding.opensecretsanta.contract.PushSummaryDto
import com.usuallycoding.opensecretsanta.domain.announcement.Announcement
import com.usuallycoding.opensecretsanta.domain.announcement.AnnouncementReadMark
import com.usuallycoding.opensecretsanta.domain.announcement.GetMyAnnouncementReads
import com.usuallycoding.opensecretsanta.domain.announcement.ListAnnouncements
import com.usuallycoding.opensecretsanta.domain.announcement.MarkAnnouncementsRead
import com.usuallycoding.opensecretsanta.domain.announcement.SendAnnouncement
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.time.Instant
import java.time.format.DateTimeParseException

private fun Announcement.toDto() = AnnouncementDto(number, title, body, Instant.ofEpochMilli(sentAtMillis).toString())

private fun AnnouncementReadMark.toDto() = ReadMarkDto(unread, seenUpToMillis?.let { Instant.ofEpochMilli(it).toString() })

/**
 * The owner tells a group's participants something. The announcement is kept first; the emails are sent before the answer,
 * which counts them, and the push notifications are counted at once and sent in the background. What was kept is read back by
 * the participants and by the owner, in the order sent, and cannot be changed. A participant also reads and marks what they have
 * seen of it, their own mark and nobody else's. Nothing of the title, the message or the recipients is logged.
 */
fun Route.announcementRoutes(
    sendAnnouncement: SendAnnouncement,
    listAnnouncements: ListAnnouncements,
    getReads: GetMyAnnouncementReads,
    markRead: MarkAnnouncementsRead,
    notifications: NotificationDispatcher,
) {
    get(ApiRoutes.GROUP_ANNOUNCEMENT_READS) {
        val user = call.currentUser()
        call.respond(blocking { getReads(user, call.groupId()) }.toDto())
    }
    put(ApiRoutes.GROUP_ANNOUNCEMENT_READS) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val request = call.receive<MarkReadRequest>()
        val seenUpTo = try {
            Instant.parse(request.seenUpTo ?: throw DomainException.ValidationFailed("seenUpTo", "The time seen up to is required."))
        } catch (e: DateTimeParseException) {
            throw DomainException.ValidationFailed("seenUpTo", "The time seen up to must be an ISO-8601 instant such as 2026-12-24T18:30:00Z.")
        }
        call.respond(blocking { markRead(user, groupId, seenUpTo.toEpochMilli()) }.toDto())
    }
    get(ApiRoutes.GROUP_ANNOUNCEMENTS) {
        val user = call.currentUser()
        call.respond(blocking { listAnnouncements.forParticipant(user, call.groupId()) }.map { it.toDto() })
    }
    get(ApiRoutes.ADMIN_GROUP_ANNOUNCEMENTS) {
        val actor = call.currentUser()
        call.respond(blocking { listAnnouncements.forOwner(actor, call.groupId()) }.map { it.toDto() })
    }
    post(ApiRoutes.ADMIN_GROUP_ANNOUNCEMENTS) {
        val actor = call.currentUser()
        val request = call.receive<AnnouncementRequest>()
        val outcome = blocking { sendAnnouncement(actor, call.groupId(), request.title, request.body, request.email, request.push) }
        outcome.pushPlan?.let { notifications.announce(it) }
        call.respond(
            AnnouncementResponse(
                emails = outcome.emails?.let { EmailSummaryDto(it.sent, it.failed, it.noAddress) },
                push = outcome.pushPlan?.let { PushSummaryDto(it.reached, it.notReached) },
            ),
        )
    }
}
