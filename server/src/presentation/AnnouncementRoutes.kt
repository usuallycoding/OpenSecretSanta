package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.AnnouncementDto
import com.usuallycoding.opensecretsanta.contract.AnnouncementRequest
import com.usuallycoding.opensecretsanta.contract.AnnouncementResponse
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.contract.PushSummaryDto
import com.usuallycoding.opensecretsanta.domain.announcement.Announcement
import com.usuallycoding.opensecretsanta.domain.announcement.ListAnnouncements
import com.usuallycoding.opensecretsanta.domain.announcement.SendAnnouncement
import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.Instant

private fun Announcement.toDto() = AnnouncementDto(number, title, body, Instant.ofEpochMilli(sentAtMillis).toString())

/**
 * The owner tells a group's participants something. The announcement is kept first; the emails are sent before the answer,
 * which counts them, and the push notifications are counted at once and sent in the background. What was kept is read back by
 * the participants and by the owner, in the order sent, and cannot be changed. Nothing of the title, the message or the
 * recipients is logged.
 */
fun Route.announcementRoutes(sendAnnouncement: SendAnnouncement, listAnnouncements: ListAnnouncements, notifications: NotificationDispatcher) {
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
