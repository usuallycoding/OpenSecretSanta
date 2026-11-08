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
    val budget: Int? = null,
    val currency: String? = null,
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val date: String? = null,
    val time: String? = null,
    val participants: List<String> = emptyList(),
    val budget: Int? = null,
    val currency: String? = null,
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
    /** True when the participant's delivery info for the group has a full name, an email, a phone and an address. */
    val profileComplete: Boolean,
    /** The participant's full name for the group, when there is one: what the owner can search a group's people by. */
    val fullName: String? = null,
    /** True when the account exists and still has a temporary password; false with no account. Only a flag. */
    val mustChangePassword: Boolean = false,
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
    val budget: Int? = null,
    val currency: String? = null,
)

@Serializable
data class CreatedAccountDto(
    val username: String,
    val temporaryPassword: String,
)

/** How the emails of a draw went: how many were sent, how many failed and how many participants have no email address. */
@Serializable
data class EmailSummaryDto(
    val sent: Int,
    val failed: Int,
    val noAddress: Int,
)

/** An announcement to a group's participants: [email] and [push] say how to send it, and at least one must be true. */
@Serializable
data class AnnouncementRequest(
    val title: String,
    val body: String,
    val email: Boolean = false,
    val push: Boolean = false,
)

/** How the push notifications of an announcement went: for how many people one was queued, and how many others could not be told. */
@Serializable
data class PushSummaryDto(
    val reached: Int,
    val notReached: Int,
)

/** What became of an announcement; a summary is there only for a channel that was used. */
@Serializable
data class AnnouncementResponse(
    val emails: EmailSummaryDto? = null,
    val push: PushSummaryDto? = null,
)

/**
 * An announcement the owner sent, as the people of the group read it later. It names no one, not even the sender. [number] counts
 * from 1 within the group, [body] keeps its line breaks and [sentAt] is an ISO-8601 UTC instant.
 */
@Serializable
data class AnnouncementDto(
    val number: Int,
    val title: String,
    val body: String,
    val sentAt: String,
)

/**
 * The group after a change, plus the accounts the change created (empty when it created none). Only the answer to an
 * activation carries [emails], and only when email is configured.
 */
@Serializable
data class GroupChangeResponse(
    val group: AdminGroupDto,
    val createdAccounts: List<CreatedAccountDto> = emptyList(),
    val emails: EmailSummaryDto? = null,
    /** The draw rules that were deleted because the participants they name left the group. */
    val deletedRules: List<DrawRuleDto> = emptyList(),
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
    val budget: Int? = null,
    val currency: String? = null,
)

@Serializable
data class AssignmentDto(
    val santa: String,
    val receiver: String,
)

/** One draw rule; [kind] is `must` (the giver gives to the receiver) or `never` (does not). */
@Serializable
data class DrawRuleDto(
    val id: Long,
    val giver: String,
    val receiver: String,
    val kind: String,
)

/** The rules of a group, and whether fewer than 10 different draws satisfy them. */
@Serializable
data class DrawRulesDto(
    val rules: List<DrawRuleDto>,
    val fewDraws: Boolean = false,
)

/** Adds a rule; with [bothWays] also the rule for the other direction. */
@Serializable
data class AddDrawRuleRequest(
    val giver: String,
    val receiver: String,
    val kind: String,
    val bothWays: Boolean = false,
)
