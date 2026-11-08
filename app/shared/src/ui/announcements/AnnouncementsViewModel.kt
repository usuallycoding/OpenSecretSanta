package com.usuallycoding.opensecretsanta.ui.announcements

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * The announcements the group's owner has sent, last one sent first. A participant reads them from the group's route, the owner,
 * who need not take part in the group, from the admin route. There is nothing here that changes or deletes one.
 */
class AnnouncementsViewModel(
    val groupId: Long,
    private val forOwner: Boolean,
    private val api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { load() }

    val data: StateFlow<LoadState<List<AnnouncementItem>>> = loader.state

    fun refresh(): Job = loader.refresh()

    private suspend fun load(): ApiResult<List<AnnouncementItem>> =
        (if (forOwner) api.adminGroupAnnouncements(groupId) else api.groupAnnouncements(groupId)).map { it.newestFirst() }
}
