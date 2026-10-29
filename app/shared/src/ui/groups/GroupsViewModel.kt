package com.usuallycoding.opensecretsanta.ui.groups

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.MemberGroupDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/** One group as the list shows it; parts the group does not have are null. */
data class GroupRow(
    val id: Long,
    override val name: String,
    override val description: String?,
    override val stateKey: String,
    val state: GroupStateInfo,
    val whenText: String?,
    val location: String?,
    val participantCount: Int,
) : GroupListItem

internal fun MemberGroupDto.toRow() = GroupRow(
    id = id,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    stateKey = state.lowercase(),
    state = groupStateInfo(state),
    whenText = formatWhen(date, time),
    location = location?.takeIf { it.isNotBlank() },
    participantCount = participants.size,
)

/** The groups the person is in, in the order the server returns them (newest first), and the search and chips that narrow them. */
class GroupsViewModel(
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.groups().map { groups -> groups.map { it.toRow() } } }

    val state: StateFlow<LoadState<List<GroupRow>>> = loader.state

    /** What the person searched for and which states they chose; the screen applies it to the loaded groups. */
    val filter = GroupListFilter(participantStateChips)

    fun refresh(): Job = loader.refresh()
}
