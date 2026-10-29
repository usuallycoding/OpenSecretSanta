package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.domain.email.EmailSummary
import com.usuallycoding.opensecretsanta.domain.email.SendDrawEmails
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.GetMyGroup
import com.usuallycoding.opensecretsanta.domain.group.ListAssignments
import com.usuallycoding.opensecretsanta.domain.group.ListMyGroups
import com.usuallycoding.opensecretsanta.domain.group.MoveToPending
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("GroupLifecycleRoutes")

/** Owner-only state changes. Every response is the group after the change plus any accounts it created. */
fun Route.groupLifecycleRoutes(
    moveToPending: MoveToPending,
    activate: ActivateGroup,
    archive: ArchiveGroup,
    listAssignments: ListAssignments,
    sendDrawEmails: SendDrawEmails,
    notifications: NotificationDispatcher,
) {
    post(ApiRoutes.ADMIN_GROUP_PENDING) {
        val actor = call.currentUser()
        val change = blocking { moveToPending(actor, call.groupId()) }
        notifications.groupVisible(change.view.group, change.view.group.participants, actor.username)
        call.respond(change.toDto())
    }
    post(ApiRoutes.ADMIN_GROUP_ACTIVATE) {
        val actor = call.currentUser()
        val view = blocking { activate(actor, call.groupId()) }
        notifications.groupDrawn(view.group, actor.username)
        // the draw is stored: whatever happens to the emails cannot undo it
        val emails = blocking {
            try {
                sendDrawEmails(view.group)
            } catch (e: Exception) {
                logger.error("Sending the draw emails failed ({})", e::class.simpleName)
                EmailSummary(sent = 0, failed = view.participants.size, noAddress = 0)
            }
        }
        call.respond(GroupChangeResponse(group = view.toDto(), emails = emails?.let { EmailSummaryDto(it.sent, it.failed, it.noAddress) }))
    }
    post(ApiRoutes.ADMIN_GROUP_ARCHIVE) {
        val actor = call.currentUser()
        val view = blocking { archive(actor, call.groupId()) }
        call.respond(GroupChangeResponse(group = view.toDto()))
    }
    get(ApiRoutes.ADMIN_GROUP_ASSIGNMENTS) {
        val actor = call.currentUser()
        val pairs = blocking { listAssignments(actor, call.groupId()) }
        call.respond(pairs.map { AssignmentDto(it.santa.value, it.receiver.value) })
    }
}

/** The groups a participant belongs to, as far as they may see them. */
fun Route.memberGroupRoutes(listMyGroups: ListMyGroups, getMyGroup: GetMyGroup) {
    get(ApiRoutes.GROUPS) {
        val user = call.currentUser()
        call.respond(blocking { listMyGroups(user) }.map { it.toMemberDto() })
    }
    get(ApiRoutes.GROUP) {
        val user = call.currentUser()
        call.respond(blocking { getMyGroup(user, call.groupId()) }.toMemberDto())
    }
}
