package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.ui.groups.GroupStateInfo
import com.usuallycoding.opensecretsanta.ui.groups.formatWhen
import com.usuallycoding.opensecretsanta.ui.groups.groupStateInfo

/** Where one of the administrator's groups is in its life, for deciding what can be done with it. */
enum class AdminPhase { Draft, Pending, Active, Archived, Other }

fun adminPhase(state: String): AdminPhase = when (state.lowercase()) {
    "draft" -> AdminPhase.Draft
    "pending" -> AdminPhase.Pending
    "active" -> AdminPhase.Active
    "archived" -> AdminPhase.Archived
    else -> AdminPhase.Other
}

/** A participant as the owner sees them, with what is missing for the gift to reach them. */
data class AdminParticipant(val username: String, val hasAccount: Boolean, val profileComplete: Boolean)

data class AdminGroup(
    val id: Long,
    val name: String,
    val description: String?,
    val location: String?,
    val date: String?,
    val time: String?,
    val phase: AdminPhase,
    val state: GroupStateInfo,
    val participants: List<AdminParticipant>,
) {
    val whenText: String? get() = formatWhen(date, time)
}

internal fun AdminGroupDto.toGroup() = AdminGroup(
    id = id,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    location = location?.takeIf { it.isNotBlank() },
    date = date?.takeIf { it.isNotBlank() },
    time = time?.takeIf { it.isNotBlank() },
    phase = adminPhase(state),
    state = groupStateInfo(state),
    participants = participants.map { AdminParticipant(it.username, it.hasAccount, it.profileComplete) },
)

/** The fields of the group form, with the names the server uses for them. */
enum class GroupField(val key: String) { Name("name"), Description("description"), Location("location"), Date("date"), Time("time") }

data class GroupForm(
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val time: String = "",
    val errors: Map<GroupField, String> = emptyMap(),
    val general: String? = null,
    val saving: Boolean = false,
)

/** A step that changes a group's state or removes it; each waits for a confirmation before anything is sent. */
enum class AdminStep { MakePending, DrawNames, Archive, Delete }

/** The working copy of the participant list while its sheet is open. */
data class ParticipantsEditor(
    val names: List<String>,
    val input: String = "",
    val error: String? = null,
    val general: String? = null,
    val saving: Boolean = false,
)

/** An account that a step created, with the password the administrator hands over. Shown once, then gone. */
data class CreatedAccount(val username: String, val temporaryPassword: String)

/** Something that happened behind the screen's back, said once. */
enum class AdminNotice { ChangedElsewhere, RosterFrozen, Unreachable }

data class AdminGroupUi(
    val participants: ParticipantsEditor? = null,
    val confirming: AdminStep? = null,
    val busy: Boolean = false,
    val passwords: List<CreatedAccount>? = null,
    val notice: AdminNotice? = null,
    /** The group was deleted: the screen goes back to the list. */
    val deleted: Boolean = false,
)
