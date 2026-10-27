package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
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

/** Owner-only state changes. Every response is the group after the change plus any accounts it created. */
fun Route.groupLifecycleRoutes(
    moveToPending: MoveToPending,
    activate: ActivateGroup,
    archive: ArchiveGroup,
    listAssignments: ListAssignments,
) {
    post(ApiRoutes.ADMIN_GROUP_PENDING) {
        val actor = call.currentUser()
        call.respond(blocking { moveToPending(actor, call.groupId()) }.toDto())
    }
    post(ApiRoutes.ADMIN_GROUP_ACTIVATE) {
        val actor = call.currentUser()
        val view = blocking { activate(actor, call.groupId()) }
        call.respond(GroupChangeResponse(group = view.toDto()))
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
