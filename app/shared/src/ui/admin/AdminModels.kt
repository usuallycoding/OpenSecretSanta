package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.ui.groups.GroupListItem
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
data class AdminParticipant(val username: String, val hasAccount: Boolean, val profileComplete: Boolean, val fullName: String? = null)

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
) : GroupListItem {
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
    stateKey = state.lowercase(),
    state = groupStateInfo(state),
    participants = participants.map { AdminParticipant(it.username, it.hasAccount, it.profileComplete, it.fullName?.takeIf { name -> name.isNotBlank() }) },
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

/** The bottom sheet open on a group's detail, if any: state of the screen, not a route, so nothing keeps it after the screen is left. */
sealed interface GroupSheet {
    /** A person's details, with the reset. */
    data class User(val username: String) : GroupSheet

    /** One pair: the receiver's wishlist and the questions the Santa asked them. */
    data class Pair(val santa: String, val receiver: String) : GroupSheet

    /** One participant's wishlist, in a Pending group. */
    data class Wishlist(val username: String) : GroupSheet
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
    val notice: AdminNotice? = null,
    val sheet: GroupSheet? = null,
    /** The group was deleted: the screen goes back to the list. */
    val deleted: Boolean = false,
)

/** A user the administrator manages, with the profile fields they have set and whether they still have a temporary password. */
data class ManagedUser(
    val username: String,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val deliveryInfo: String?,
    val mustChangePassword: Boolean,
    /** What the server returns while the password is still the temporary one; null once the person changed it. */
    val temporaryPassword: String?,
)

internal fun com.usuallycoding.opensecretsanta.contract.ManagedUserDto.toManagedUser() = ManagedUser(
    username = user.username,
    fullName = user.fullName?.trim()?.ifEmpty { null },
    email = user.email?.trim()?.ifEmpty { null },
    phone = user.phone?.trim()?.ifEmpty { null },
    address = user.address?.trim()?.ifEmpty { null },
    deliveryInfo = user.deliveryInfo?.trim()?.ifEmpty { null },
    mustChangePassword = user.mustChangePassword,
    temporaryPassword = temporaryPassword,
)

/** Something that went wrong with a reset, said once. */
enum class UserNotice { Unreachable, Failed }

data class AdminUserUi(
    val confirming: Boolean = false,
    val busy: Boolean = false,
    /** The new temporary password after a reset, shown in its dialog until it is closed. */
    val created: CreatedAccount? = null,
    val notice: UserNotice? = null,
)
