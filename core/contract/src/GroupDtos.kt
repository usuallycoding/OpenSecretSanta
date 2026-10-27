package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

/** The editable details of a group; [date] is an ISO date (2026-12-24) and [time] a time of day (18:30). */
@Serializable
data class GroupDetailsRequest(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val date: String? = null,
    val time: String? = null,
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val date: String? = null,
    val time: String? = null,
    val participants: List<String> = emptyList(),
)

/** Replaces the whole participant list. */
@Serializable
data class ParticipantsRequest(
    val participants: List<String>,
)

/** A participant as the owner sees them. */
@Serializable
data class ParticipantDto(
    val username: String,
    val hasAccount: Boolean,
    /** True when the account has both a full name and an address. */
    val profileComplete: Boolean,
)

/** [state] is one of `draft`, `pending`, `active`, `archived`. */
@Serializable
data class AdminGroupDto(
    val id: Long,
    val owner: String,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val date: String? = null,
    val time: String? = null,
    val state: String,
    val participants: List<ParticipantDto>,
)

@Serializable
data class CreatedAccountDto(
    val username: String,
    val temporaryPassword: String,
)

/** The group after a change, plus the accounts the change created (empty when it created none). */
@Serializable
data class GroupChangeResponse(
    val group: AdminGroupDto,
    val createdAccounts: List<CreatedAccountDto> = emptyList(),
)

/** What a participant sees of a group: details, state and the other participants' usernames. */
@Serializable
data class MemberGroupDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val date: String? = null,
    val time: String? = null,
    val state: String,
    val participants: List<String>,
)

@Serializable
data class AssignmentDto(
    val santa: String,
    val receiver: String,
)
