package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.CreatedAccountDto
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.contract.GroupDetailsRequest
import com.usuallycoding.opensecretsanta.contract.MemberGroupDto
import com.usuallycoding.opensecretsanta.contract.ParticipantDto
import com.usuallycoding.opensecretsanta.contract.ParticipantsRequest
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.CreateGroup
import com.usuallycoding.opensecretsanta.domain.group.DeleteGroup
import com.usuallycoding.opensecretsanta.domain.group.GetOwnedGroup
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupChange
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupView
import com.usuallycoding.opensecretsanta.domain.group.ListOwnedGroups
import com.usuallycoding.opensecretsanta.domain.group.SetParticipants
import com.usuallycoding.opensecretsanta.domain.group.UpdateGroupDetails
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

/** A non-numeric id can never match a group, so it is reported like any other missing group. */
fun ApplicationCall.groupId(): Long =
    parameters["id"]?.toLongOrNull() ?: throw DomainException.NotFound("Group not found.")

fun GroupView.toDto() = AdminGroupDto(
    id = group.id,
    owner = group.owner.value,
    name = group.details.name,
    description = group.details.description,
    location = group.details.location,
    date = group.details.date?.toString(),
    time = group.details.time?.toString(),
    state = group.state.storedName,
    participants = participants.map { ParticipantDto(it.username.value, it.hasAccount, it.profileComplete) },
)

fun GroupChange.toDto() = GroupChangeResponse(
    group = view.toDto(),
    createdAccounts = createdAccounts.map { CreatedAccountDto(it.username.value, it.temporaryPassword) },
)

/** What a participant may see: details, state and the other participants' usernames. */
fun Group.toMemberDto() = MemberGroupDto(
    id = id,
    name = details.name,
    description = details.description,
    location = details.location,
    date = details.date?.toString(),
    time = details.time?.toString(),
    state = state.storedName,
    participants = participants.map { it.value },
)

private fun GroupDetailsRequest.toDetails() = GroupDetails.of(name, description, location, date, time)

/** Owner-only routes to create, read, edit and delete groups and replace their participants. */
fun Route.adminGroupRoutes(
    createGroup: CreateGroup,
    listOwned: ListOwnedGroups,
    getOwned: GetOwnedGroup,
    updateDetails: UpdateGroupDetails,
    setParticipants: SetParticipants,
    deleteGroup: DeleteGroup,
    notifications: NotificationDispatcher,
) {
    post(ApiRoutes.ADMIN_GROUPS) {
        val actor = call.currentUser()
        val request = call.receive<CreateGroupRequest>()
        val details = GroupDetails.of(request.name, request.description, request.location, request.date, request.time)
        call.respond(HttpStatusCode.Created, blocking { createGroup(actor, details, request.participants) }.toDto())
    }
    get(ApiRoutes.ADMIN_GROUPS) {
        val actor = call.currentUser()
        call.respond(blocking { listOwned(actor) }.map { it.toDto() })
    }
    get(ApiRoutes.ADMIN_GROUP) {
        val actor = call.currentUser()
        call.respond(blocking { getOwned(actor, call.groupId()) }.toDto())
    }
    put(ApiRoutes.ADMIN_GROUP) {
        val actor = call.currentUser()
        val id = call.groupId()
        val details = call.receive<GroupDetailsRequest>().toDetails()
        call.respond(blocking { updateDetails(actor, id, details) }.toDto())
    }
    put(ApiRoutes.ADMIN_GROUP_PARTICIPANTS) {
        val actor = call.currentUser()
        val id = call.groupId()
        val request = call.receive<ParticipantsRequest>()
        val change = blocking { setParticipants(actor, id, request.participants) }
        // only people who can now see the group are told: Draft is not visible to participants
        if (change.added.isNotEmpty()) notifications.groupVisible(change.view.group, change.added, actor.username)
        call.respond(change.toDto())
    }
    delete(ApiRoutes.ADMIN_GROUP) {
        val actor = call.currentUser()
        val confirmed = call.request.queryParameters[ApiRoutes.CONFIRM] == "true"
        blocking { deleteGroup(actor, call.groupId(), confirmed) }
        call.respond(HttpStatusCode.NoContent)
    }
}
