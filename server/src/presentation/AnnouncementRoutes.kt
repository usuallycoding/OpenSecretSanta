package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.AnnouncementRequest
import com.usuallycoding.opensecretsanta.contract.AnnouncementResponse
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.contract.PushSummaryDto
import com.usuallycoding.opensecretsanta.domain.announcement.SendAnnouncement
import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * The owner tells a group's participants something. The emails are sent before the answer, which counts them; the push
 * notifications are counted at once and sent in the background. Nothing of the title, the message or the recipients
 * is logged, and nothing is stored.
 */
fun Route.announcementRoutes(sendAnnouncement: SendAnnouncement, notifications: NotificationDispatcher) {
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
