package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.ui.groups.GroupListItem
import com.usuallycoding.opensecretsanta.ui.groups.GroupStateInfo
import com.usuallycoding.opensecretsanta.ui.groups.budgetLine
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
data class AdminParticipant(
    val username: String,
    val hasAccount: Boolean,
    val profileComplete: Boolean,
    val fullName: String? = null,
    /** The account still has a temporary password. */
    val mustChangePassword: Boolean = false,
)

data class AdminGroup(
    val id: Long,
    override val name: String,
    override val description: String?,
    val location: String?,
    val date: String?,
    val time: String?,
    val phase: AdminPhase,
    override val stateKey: String,
    val state: GroupStateInfo,
    val participants: List<AdminParticipant>,
    val budget: Int? = null,
    val currency: String? = null,
) : GroupListItem {
    val whenText: String? get() = formatWhen(date, time)
    val budgetText: String? get() = budgetLine(budget, currency)
}

internal fun AdminGroupDto.toGroup() = AdminGroup(
    id = id,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    location = location?.takeIf { it.isNotBlank() },
    date = date?.takeIf { it.isNotBlank() },
    time = time?.takeIf { it.isNotBlank() },
    phase = adminPhase(state),
    stateKey = state.lowercase(),
    state = groupStateInfo(state),
    participants = participants.map { AdminParticipant(it.username, it.hasAccount, it.profileComplete, it.fullName?.takeIf { name -> name.isNotBlank() }, it.mustChangePassword) },
    budget = budget,
    currency = currency?.takeIf { it.isNotBlank() },
)

/** The fields of the group form, with the names the server uses for them. */
enum class GroupField(val key: String) { Name("name"), Description("description"), Location("location"), Budget("budget"), Currency("currency"), Date("date"), Time("time") }

data class GroupForm(
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val budget: String = "",
    val currency: String = GroupDetails.DEFAULT_CURRENCY,
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

/** The bottom sheet open on a group's detail, if any: state of the screen, not a route, so nothing keeps it after the screen is left. */
sealed interface GroupSheet {
    /** A person's details, with the reset. */
    data class User(val username: String) : GroupSheet

    /** One pair: the receiver's wishlist and the questions the Santa asked them. */
    data class Pair(val santa: String, val receiver: String) : GroupSheet

    /** One participant's wishlist, in a Pending group. */
    data class Wishlist(val username: String) : GroupSheet

    /** The group's draw rules, in the pane beside the group page or, when there is no pane, a sheet. */
    data object Rules : GroupSheet
}

data class AdminGroupUi(
    val participants: ParticipantsEditor? = null,
    val confirming: AdminStep? = null,
    /** How the emails of the draw just made went, until the owner dismisses it; null when there is nothing to say. */
    val emails: EmailSummaryDto? = null,
    /** What deleting the group would also delete, once the server has asked for a second confirmation of it. */
    val confirmingDelete: DeletionImpactDto? = null,
    val busy: Boolean = false,
    val passwords: List<CreatedAccount>? = null,
    /** The passwords were asked for with "Get passwords", not handed over because accounts were just created. */
    val passwordsAskedFor: Boolean = false,
    val notice: AdminNotice? = null,
    val sheet: GroupSheet? = null,
    /** The group was deleted: the screen goes back to the list. */
    val deleted: Boolean = false,
    /** The draw rules that went with participants who were removed, until the owner dismisses the notice. */
    val rulesDeleted: List<DrawRuleItem> = emptyList(),
    /** The server's explanation of why it would not draw the names because of the rules, until dismissed. */
    val drawRefused: String? = null,
)

/** A user the administrator manages, and whether they still have a temporary password. */
data class ManagedUser(
    val username: String,
    val mustChangePassword: Boolean,
    /** What the server returns while the password is still the temporary one; null once the person changed it. */
    val temporaryPassword: String?,
    /** The server says this account can be deleted: one the administrator created that is in no group. */
    val canDelete: Boolean = false,
)

internal fun com.usuallycoding.opensecretsanta.contract.ManagedUserDto.toManagedUser() = ManagedUser(
    username = user.username,
    mustChangePassword = user.mustChangePassword,
    temporaryPassword = temporaryPassword,
    canDelete = canDelete,
)

/** Something that went wrong with a reset, said once. */
enum class UserNotice { Unreachable, Failed, HandOverRefused }

/** Something that went wrong with a deletion on the Users list, said once. */
enum class UsersNotice { Unreachable, DeleteFailed, DeleteRefused }

data class AdminUsersUi(
    /** The username the deletion confirmation is open for. */
    val confirmingDelete: String? = null,
    val busy: Boolean = false,
    val notice: UsersNotice? = null,
)

data class AdminUserUi(
    val confirming: Boolean = false,
    val busy: Boolean = false,
    /** The new temporary password after a reset, shown in its dialog until it is closed. */
    val created: CreatedAccount? = null,
    val notice: UserNotice? = null,
    /** The hand-over confirmation is open; the password typed in it is kept by the dialog, not here. */
    val handingOver: Boolean = false,
    /** What the server said about the password or the attempts, shown in the open confirmation. */
    val handOverError: String? = null,
)
