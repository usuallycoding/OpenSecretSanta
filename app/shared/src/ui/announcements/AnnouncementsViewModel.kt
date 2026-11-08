package com.usuallycoding.opensecretsanta.ui.announcements

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import com.usuallycoding.opensecretsanta.ui.readmarks.ReadTracker
import com.usuallycoding.opensecretsanta.ui.readmarks.newestOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * The announcements the group's owner has sent, last one sent first, each marked new when it was sent after what the person had seen
 * on opening the page. A participant reads them from the group's route, the owner, who need not take part in the group and has nothing
 * to mark, from the admin route. There is nothing here that changes or deletes one.
 */
class AnnouncementsViewModel(
    val groupId: Long,
    private val forOwner: Boolean,
    private val api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val tracker = if (forOwner) null else ReadTracker(scope, { api.announcementReads(groupId) }, { time -> api.markAnnouncementsRead(groupId, time) })
    private val loader = Loader(scope) { load() }

    val data: StateFlow<LoadState<List<AnnouncementItem>>> = loader.state

    fun refresh(): Job = loader.refresh()

    /** Tells the server the announcements have been shown, so that the next visit has none marked; call it once the page is on screen. */
    fun markSeen(): Job? = tracker?.markSeen()

    private suspend fun load(): ApiResult<List<AnnouncementItem>> {
        val announcements = when (val result = if (forOwner) api.adminGroupAnnouncements(groupId) else api.groupAnnouncements(groupId)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        tracker?.ensureBaseline()
        tracker?.shown(newestOf(announcements.map { it.sentAt }))
        return ApiResult.Success(announcements.newestFirst(tracker?.baseline))
    }
}
