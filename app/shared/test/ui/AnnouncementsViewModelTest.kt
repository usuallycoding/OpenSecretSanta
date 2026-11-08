package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.announcements.AnnouncementsViewModel
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnnouncementsViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val announcements get() = server.announcements

    private suspend fun page(forOwner: Boolean = false) =
        AnnouncementsViewModel(1, forOwner, server.api, immediateScope()).also { it.refresh().join() }

    @Test
    fun theAnnouncementsAreShownNewestFirstWithTheTimeAsTheAppShowsTimes() = runTest {
        announcements.keptBehindTheScenes(3)

        val items = page().data.value.value!!

        assertEquals(listOf(3, 2, 1), items.map { it.number })
        assertEquals("Title 3", items.first().title)
        assertEquals("Message 3", items.first().text)
        assertEquals("4 Dec 2026, 11:00", items.first().sent)
    }

    @Test
    fun noAnnouncementsIsAnEmptyList() = runTest {
        assertEquals(emptyList(), page().data.value.value!!)
    }

    @Test
    fun aParticipantReadsTheGroupRouteAndTheOwnerTheAdminRoute() = runTest {
        announcements.keptBehindTheScenes(1)

        page(forOwner = false)
        page(forOwner = true)

        assertTrue(server.calls.any { it.startsWith("GET /groups/1/announcements") })
        assertTrue(server.calls.any { it.startsWith("GET /admin/groups/1/announcements") })
    }

    @Test
    fun aReloadShowsWhatWasSentSince() = runTest {
        announcements.keptBehindTheScenes(1)
        val model = page()

        announcements.keptBehindTheScenes(1)
        model.refresh().join()

        assertEquals(listOf(2, 1), model.data.value.value!!.map { it.number })
    }

    @Test
    fun aGroupThatIsGoneReadsAsGone() = runTest {
        announcements.failNextReadWith(HttpStatusCode.NotFound, "not_found", "Group not found.")

        val state = page().data.value

        assertNull(state.value)
        assertTrue(state.isGone())
    }

    @Test
    fun aFailedReloadKeepsTheAnnouncementsAlreadyShown() = runTest {
        announcements.keptBehindTheScenes(2)
        val model = page()

        announcements.failNextReadWith(HttpStatusCode.InternalServerError, "internal_error")
        model.refresh().join()

        assertEquals(2, model.data.value.value!!.size)
        assertNotNull(model.data.value.error)
    }
}
