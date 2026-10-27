package com.usuallycoding.opensecretsanta.ui.groups

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.MemberGroupDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

data class ParticipantItem(val username: String, val isMe: Boolean)

/** One group as its detail screen shows it. Nothing here says who gives to whom. */
data class GroupDetail(
    val name: String,
    val state: GroupStateInfo,
    val phase: GroupPhase,
    val description: String?,
    val whenText: String?,
    val location: String?,
    val participants: List<ParticipantItem>,
)

internal fun MemberGroupDto.toDetail(me: String) = GroupDetail(
    name = name,
    state = groupStateInfo(state),
    phase = groupPhase(state),
    description = description?.takeIf { it.isNotBlank() },
    whenText = formatWhen(date, time),
    location = location?.takeIf { it.isNotBlank() },
    participants = participants.map { ParticipantItem(it, isMe(it, me)) },
)

/** The server answers 404 for a group that is gone or that the person no longer belongs to. */
fun LoadState<*>.isGone(): Boolean = (error as? ApiError.Rejected)?.status == 404

class GroupDetailViewModel(
    val groupId: Long,
    me: String,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.group(groupId).map { it.toDetail(me) } }

    val state: StateFlow<LoadState<GroupDetail>> = loader.state

    fun refresh(): Job = loader.refresh()
}
