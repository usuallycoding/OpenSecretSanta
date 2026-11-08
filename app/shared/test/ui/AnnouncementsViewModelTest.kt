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

    // --- what is new ---

    private fun keepThree() {
        announcements.keep("2026-12-01T10:00:00Z")
        announcements.keep("2026-12-02T10:00:00Z")
        announcements.keep("2026-12-03T10:00:00Z")
    }

    private fun AnnouncementsViewModel.news() = data.value.value!!.associate { it.number to it.isNew }

    @Test
    fun whatWasSentAfterTheTimeSeenIsNew() = runTest {
        keepThree()
        announcements.seenUpTo = "2026-12-01T10:00:00Z"

        assertEquals(mapOf(3 to true, 2 to true, 1 to false), page().news())
    }

    @Test
    fun forAPersonWhoNeverLookedEverythingIsNew() = runTest {
        keepThree()

        assertEquals(mapOf(3 to true, 2 to true, 1 to true), page().news())
    }

    @Test
    fun theMarksStayWhileThePageIsOpenAndAnArrivalIsMarkedToo() = runTest {
        keepThree()
        announcements.seenUpTo = "2026-12-02T10:00:00Z"
        val model = page()
        model.markSeen()?.join()
        assertEquals(mapOf(3 to true, 2 to false, 1 to false), model.news())

        announcements.keep("2026-12-04T10:00:00Z")
        model.refresh().join()

        assertEquals(mapOf(4 to true, 3 to true, 2 to false, 1 to false), model.news())
    }

    @Test
    fun whenTheTimeSeenCannotBeReadNothingIsMarkedAndNothingIsSent() = runTest {
        keepThree()
        // the first load runs when the view model is made, and the page loads once more
        announcements.failNextReadsWith(HttpStatusCode.InternalServerError)
        announcements.failNextReadsWith(HttpStatusCode.InternalServerError)

        val model = page()

        assertEquals(mapOf(3 to false, 2 to false, 1 to false), model.news())
        assertNull(model.markSeen())
        assertTrue(announcements.marked.isEmpty())
    }

    @Test
    fun theNewestTimeIsSentAsSeenOnceAndNotAgainForTheSameTime() = runTest {
        keepThree()
        val model = page()

        model.markSeen()?.join()
        assertEquals(listOf("2026-12-03T10:00:00Z"), announcements.marked)

        model.refresh().join()
        assertNull(model.markSeen())
        assertEquals(1, announcements.marked.size)

        announcements.keep("2026-12-05T10:00:00Z")
        model.refresh().join()
        model.markSeen()?.join()
        assertEquals(listOf("2026-12-03T10:00:00Z", "2026-12-05T10:00:00Z"), announcements.marked)
    }

    @Test
    fun aFailedMarkLeavesThemNewForTheNextVisit() = runTest {
        keepThree()
        announcements.failNextMarkWith(HttpStatusCode.InternalServerError)
        page().markSeen()?.join()
        assertEquals(null, announcements.seenUpTo)

        val nextVisit = page()

        assertEquals(mapOf(3 to true, 2 to true, 1 to true), nextVisit.news())
        nextVisit.markSeen()?.join()
        assertEquals("2026-12-03T10:00:00Z", announcements.seenUpTo)
    }

    @Test
    fun anOpenedPageHasNothingNewAfterAMarkedVisit() = runTest {
        keepThree()
        page().markSeen()?.join()

        assertEquals(mapOf(3 to false, 2 to false, 1 to false), page().news())
    }

    @Test
    fun theOwnerHasNothingMarkedAndAsksForNoTime() = runTest {
        keepThree()

        val model = page(forOwner = true)

        assertEquals(mapOf(3 to false, 2 to false, 1 to false), model.news())
        assertNull(model.markSeen())
        assertTrue(server.calls.none { it.contains("announcements/reads") })
    }
}
