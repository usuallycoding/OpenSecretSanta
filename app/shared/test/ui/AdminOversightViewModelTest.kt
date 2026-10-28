package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminPairThreadViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminParticipantWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.admin.DrawState
import com.usuallycoding.opensecretsanta.ui.admin.GroupSheet
import com.usuallycoding.opensecretsanta.ui.groups.formatInstant
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminOversightViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val groups get() = server.adminGroups
    private val oversight get() = server.oversight

    private suspend fun group(id: Long) = AdminGroupViewModel(id, "admin", server.api, immediateScope()).also { it.refresh().join() }

    private suspend fun wishlist(id: Long, username: String) =
        AdminParticipantWishlistViewModel(id, username, server.api, immediateScope()).also { it.refresh().join() }

    private suspend fun thread(id: Long, santa: String, receiver: String) =
        AdminPairThreadViewModel(id, santa, receiver, server.api, immediateScope()).also { it.refresh().join() }

    private fun assignmentCalls() = server.calls.count { it.endsWith("/assignments") }

    // --- the draw on the group's detail --------------------------------------------------------------

    @Test
    fun anActiveGroupShowsEveryPairAndMarksTheOwnersOwn() = runTest {
        val id = groups.add("Party", "active", listOf("admin", "bob", "carol"))
        oversight.draw(id, "admin" to "bob", "bob" to "carol", "carol" to "admin")

        val draw = group(id).draw.value as DrawState.Pairs

        assertEquals(listOf("admin" to "bob", "bob" to "carol", "carol" to "admin"), draw.pairs.map { it.santa to it.receiver })
        assertEquals(listOf(true, false, false), draw.pairs.map { it.santaIsMe })
        assertEquals(listOf(false, false, true), draw.pairs.map { it.receiverIsMe })
    }

    @Test
    fun anArchivedGroupShowsTheDrawToo() = runTest {
        val id = groups.add("Done", "archived", listOf("alice", "bob"))
        oversight.draw(id, "alice" to "bob", "bob" to "alice")

        assertEquals(2, (group(id).draw.value as DrawState.Pairs).pairs.size)
    }

    @Test
    fun aDraftAndAPendingGroupAreNeverAskedForTheDraw() = runTest {
        val draft = group(groups.add("Idea", "draft", listOf("alice")))
        val pending = group(groups.add("Soon", "pending", listOf("alice")))

        assertEquals(DrawState.None, draft.draw.value)
        assertEquals(DrawState.None, pending.draw.value)
        assertEquals(0, assignmentCalls())
        assertFalse(draft.canReadDraw || pending.canReadDraw)
    }

    @Test
    fun drawingTheNamesLoadsTheDrawWithoutLeavingTheScreen() = runTest {
        val id = groups.add("Party", "pending", listOf("alice", "bob", "carol", "dave"))
        oversight.draw(id, "alice" to "bob")
        val model = group(id)
        assertEquals(DrawState.None, model.draw.value)

        model.ask(com.usuallycoding.opensecretsanta.ui.admin.AdminStep.DrawNames)
        model.confirmStep()!!.join()

        assertTrue(model.draw.value is DrawState.Pairs)
        assertTrue(model.canReadDraw)
    }

    @Test
    fun aFailedDrawLeavesTheRestOfTheDetail() = runTest {
        val id = groups.add("Party", "active", listOf("alice", "bob"))
        oversight.failNextWith(HttpStatusCode.InternalServerError, "server_error")

        val model = group(id)

        assertEquals(DrawState.Failed, model.draw.value)
        assertEquals("Party", model.data.value.value!!.name)
        assertNull(model.data.value.error)
    }

    @Test
    fun aUsernameWithAnIncompleteProfileIsFlaggedInEveryRowItIsIn() = runTest {
        groups.completeProfiles += setOf("alice", "carol", "admin")
        val id = groups.add("Party", "active", listOf("admin", "alice", "bob", "carol"))
        oversight.draw(id, "alice" to "bob", "bob" to "carol", "carol" to "admin", "admin" to "alice")

        val pairs = (group(id).draw.value as DrawState.Pairs).pairs

        assertEquals(listOf(false, true, false, false), pairs.map { it.santaIncomplete })
        assertEquals(listOf(true, false, false, false), pairs.map { it.receiverIncomplete })
    }

    @Test
    fun noOneIsFlaggedWhenEveryProfileIsComplete() = runTest {
        groups.completeProfiles += setOf("alice", "bob")
        val id = groups.add("Party", "archived", listOf("alice", "bob"))
        oversight.draw(id, "alice" to "bob", "bob" to "alice")

        val pairs = (group(id).draw.value as DrawState.Pairs).pairs

        assertTrue(pairs.none { it.santaIncomplete || it.receiverIncomplete })
    }

    @Test
    fun onlyAPendingGroupOffersTheWishlistLink() = runTest {
        assertFalse(group(groups.add("Idea", "draft", listOf("alice"))).canReadPendingWishlists)
        assertTrue(group(groups.add("Soon", "pending", listOf("alice"))).canReadPendingWishlists)
        assertFalse(group(groups.add("Live", "active", listOf("alice", "bob"))).canReadPendingWishlists)
        assertFalse(group(groups.add("Done", "archived", listOf("alice", "bob"))).canReadPendingWishlists)
    }

    // --- the sheets on the group's detail ------------------------------------------------------------

    @Test
    fun aDrawnGroupOpensAUserSheetAndAPairSheetOneAtATime() = runTest {
        val id = groups.add("Party", "active", listOf("alice", "bob"))
        oversight.draw(id, "alice" to "bob", "bob" to "alice")
        val model = group(id)

        model.openUserSheet("alice")
        assertEquals(GroupSheet.User("alice"), model.ui.value.sheet)

        model.openPairSheet("alice", "bob")
        assertEquals(GroupSheet.Pair("alice", "bob"), model.ui.value.sheet)

        model.dismissSheet()
        assertNull(model.ui.value.sheet)
    }

    @Test
    fun aPendingGroupOpensOnlyTheWishlistSheet() = runTest {
        val model = group(groups.add("Soon", "pending", listOf("alice", "bob")))

        model.openUserSheet("alice")
        model.openPairSheet("alice", "bob")
        assertNull(model.ui.value.sheet)

        model.openWishlistSheet("bob")
        assertEquals(GroupSheet.Wishlist("bob"), model.ui.value.sheet)
    }

    @Test
    fun aDraftOpensNoSheet() = runTest {
        val model = group(groups.add("Idea", "draft", listOf("alice", "bob")))

        model.openUserSheet("alice")
        model.openWishlistSheet("alice")

        assertNull(model.ui.value.sheet)
    }

    @Test
    fun anActiveGroupHasNoWishlistSheetOfItsOwn() = runTest {
        val model = group(groups.add("Live", "active", listOf("alice", "bob")))

        model.openWishlistSheet("bob")

        assertNull(model.ui.value.sheet)
    }

    // --- a participant's wishlist ----------------------------------------------------------------------

    @Test
    fun theWishlistKeepsTheParticipantsOrder() = runTest {
        val id = groups.add("Soon", "pending", listOf("alice", "bob"))
        oversight.wishlist(id, "bob", "Socks", "Book", "Mug")

        assertEquals(listOf("Socks", "Book", "Mug"), wishlist(id, "bob").state.value.value!!.map { it.title })
    }

    @Test
    fun anEmptyWishlistIsAnEmptyList() = runTest {
        val id = groups.add("Soon", "pending", listOf("alice"))

        assertEquals(emptyList(), wishlist(id, "alice").state.value.value)
    }

    @Test
    fun theTimesAreReadableAndTheChangeShowsOnlyWhenThereWasOne() = runTest {
        val id = groups.add("Soon", "pending", listOf("alice"))
        oversight.wishlistItems(
            id, "alice",
            listOf(
                SharedWishlistItemDto("Kept", null, null, "2026-12-01T10:00:00Z", "2026-12-01T10:00:00Z"),
                SharedWishlistItemDto("Edited", "Blue", "https://shop.example/x", "2026-12-01T10:00:00Z", "2026-12-05T08:30:00Z"),
            ),
        )

        val items = wishlist(id, "alice").state.value.value!!

        assertEquals("1 Dec 2026, 10:00 UTC", items[0].added)
        assertNull(items[0].changed)
        assertEquals("5 Dec 2026, 08:30 UTC", items[1].changed)
        assertEquals("Blue", items[1].note)
        assertEquals("https://shop.example/x", items[1].url)
    }

    @Test
    fun aParticipantWhoLeftIsShownAsGone() = runTest {
        val id = groups.add("Soon", "pending", listOf("alice"))

        assertTrue(wishlist(id, "zed").state.value.isGone())
    }

    @Test
    fun aGroupThatMovedBackToADraftAnswers409() = runTest {
        val id = groups.add("Idea", "draft", listOf("alice"))

        val error = wishlist(id, "alice").state.value.error

        assertEquals(409, (error as ApiError.Rejected).status)
    }

    @Test
    fun anUnreachableServerKeepsTheListAndOffersARetry() = runTest {
        val id = groups.add("Soon", "pending", listOf("alice"))
        oversight.wishlist(id, "alice", "Socks")
        val model = wishlist(id, "alice")

        server.unreachable = true
        model.refresh().join()

        assertEquals(ApiError.Network, model.state.value.error)
        assertEquals(listOf("Socks"), model.state.value.value!!.map { it.title })
    }

    // --- one pair's thread ---------------------------------------------------------------------------

    private fun threadOf(santa: String, receiver: String, vararg questions: QuestionDto) = AdminQuestionThreadDto(santa, receiver, questions.toList())

    @Test
    fun onlyTheChosenPairsThreadIsKept() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob", "carol"))
        oversight.threads(
            id,
            threadOf("alice", "bob", QuestionDto(1, "Size?", "2026-12-02T09:00:00Z")),
            threadOf("bob", "carol", QuestionDto(1, "Colour?", "2026-12-02T09:01:00Z"), QuestionDto(2, "Allergies?", "2026-12-02T09:02:00Z")),
            threadOf("carol", "alice"),
        )

        val questions = thread(id, "bob", "carol").state.value.value!!.questions

        assertEquals(listOf("Colour?", "Allergies?"), questions.map { it.text })
    }

    @Test
    fun anUnansweredQuestionHasNoAnswerAndAChangedAnswerHasBothTimes() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob"))
        oversight.threads(
            id,
            threadOf(
                "alice", "bob",
                QuestionDto(1, "Size?", "2026-12-02T09:00:00Z"),
                QuestionDto(2, "Colour?", "2026-12-02T09:05:00Z", "Blue", "2026-12-03T09:00:00Z", "2026-12-04T10:15:00Z"),
                QuestionDto(3, "Allergies?", "2026-12-02T09:06:00Z", "None", "2026-12-03T09:10:00Z", "2026-12-03T09:10:00Z"),
            ),
        )

        val (first, second, third) = thread(id, "alice", "bob").state.value.value!!.questions

        assertNull(first.answer)
        assertNull(first.answered)
        assertEquals("2 Dec 2026, 09:00 UTC", first.asked)
        assertEquals("3 Dec 2026, 09:00 UTC", second.answered)
        assertEquals("4 Dec 2026, 10:15 UTC", second.answerChanged)
        assertNull(third.answerChanged)
    }

    @Test
    fun aPairWithNothingAskedHasAnEmptyThread() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob"))
        oversight.threads(id, threadOf("alice", "bob"))

        assertEquals(emptyList(), thread(id, "alice", "bob").state.value.value!!.questions)
    }

    @Test
    fun aPairWithNoThreadAtAllIsAlsoEmpty() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob"))

        assertEquals(emptyList(), thread(id, "bob", "alice").state.value.value!!.questions)
    }

    @Test
    fun aPendingGroupHasNoThreadsToRead() = runTest {
        val id = groups.add("Soon", "pending", listOf("alice", "bob"))

        assertEquals(409, (thread(id, "alice", "bob").state.value.error as ApiError.Rejected).status)
    }

    @Test
    fun aGroupOfAnotherAdministratorIsShownAsGone() = runTest {
        assertTrue(thread(groups.addForeign("Not mine"), "alice", "bob").state.value.isGone())
    }

    @Test
    fun anUnreachableServerKeepsTheThreadAndOffersARetry() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob"))
        oversight.threads(id, threadOf("alice", "bob", QuestionDto(1, "Size?", "2026-12-02T09:00:00Z")))
        val model = thread(id, "alice", "bob")

        server.unreachable = true
        model.refresh().join()

        assertEquals(ApiError.Network, model.state.value.error)
        assertEquals(1, model.state.value.value!!.questions.size)
    }

    // --- reading is never writing ------------------------------------------------------------------------

    @Test
    fun theOversightScreensOnlyEverSendGets() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob"))
        oversight.draw(id, "alice" to "bob")
        group(id)
        wishlist(id, "alice")
        thread(id, "alice", "bob")

        assertTrue(server.calls.all { it.startsWith("GET ") }, server.calls.toString())
    }

    @Test
    fun instantsThatCannotBeReadAreShownAsTheyCame() {
        assertEquals("yesterday", formatInstant("yesterday"))
        assertEquals("24 Dec 2026, 18:30 UTC", formatInstant("2026-12-24T18:30:15.123Z"))
    }
}
