package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminParticipantWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminQuestionsViewModel
import com.usuallycoding.opensecretsanta.ui.admin.DrawState
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

    private suspend fun questions(id: Long) = AdminQuestionsViewModel(id, "admin", server.api, immediateScope()).also { it.refresh().join() }

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
    fun theOwnerReadsWishlistsFromPendingOnButNotInADraft() = runTest {
        assertFalse(group(groups.add("Idea", "draft", listOf("alice"))).canReadWishlists)
        assertTrue(group(groups.add("Soon", "pending", listOf("alice"))).canReadWishlists)
        assertTrue(group(groups.add("Live", "active", listOf("alice", "bob"))).canReadWishlists)
        assertTrue(group(groups.add("Done", "archived", listOf("alice", "bob"))).canReadWishlists)
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

    // --- all question threads --------------------------------------------------------------------------

    private fun thread(santa: String, receiver: String, vararg questions: QuestionDto) = AdminQuestionThreadDto(santa, receiver, questions.toList())

    @Test
    fun everyThreadNamesBothPeopleInOrderAndMarksTheOwner() = runTest {
        val id = groups.add("Live", "active", listOf("admin", "bob", "carol"))
        oversight.threads(id, thread("admin", "bob"), thread("bob", "carol"), thread("carol", "admin"))

        val threads = questions(id).state.value.value!!

        assertEquals(listOf("admin" to "bob", "bob" to "carol", "carol" to "admin"), threads.map { it.santa to it.receiver })
        assertEquals(listOf(true, false, false), threads.map { it.santaIsMe })
        assertEquals(listOf(false, false, true), threads.map { it.receiverIsMe })
    }

    @Test
    fun anUnansweredQuestionHasNoAnswerAndAChangedAnswerHasBothTimes() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob"))
        oversight.threads(
            id,
            thread(
                "alice", "bob",
                QuestionDto(1, "Size?", "2026-12-02T09:00:00Z"),
                QuestionDto(2, "Colour?", "2026-12-02T09:05:00Z", "Blue", "2026-12-03T09:00:00Z", "2026-12-04T10:15:00Z"),
                QuestionDto(3, "Allergies?", "2026-12-02T09:06:00Z", "None", "2026-12-03T09:10:00Z", "2026-12-03T09:10:00Z"),
            ),
        )

        val (first, second, third) = questions(id).state.value.value!!.single().questions

        assertNull(first.answer)
        assertNull(first.answered)
        assertEquals("2 Dec 2026, 09:00 UTC", first.asked)
        assertEquals("3 Dec 2026, 09:00 UTC", second.answered)
        assertEquals("4 Dec 2026, 10:15 UTC", second.answerChanged)
        assertNull(third.answerChanged)
    }

    @Test
    fun aThreadWithNoQuestionsIsStillThere() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob"))
        oversight.threads(id, thread("alice", "bob"))

        assertEquals(emptyList(), questions(id).state.value.value!!.single().questions)
    }

    @Test
    fun aPendingGroupHasNoThreadsToRead() = runTest {
        val id = groups.add("Soon", "pending", listOf("alice", "bob"))

        assertEquals(409, (questions(id).state.value.error as ApiError.Rejected).status)
    }

    @Test
    fun aGroupOfAnotherAdministratorIsShownAsGone() = runTest {
        assertTrue(questions(groups.addForeign("Not mine")).state.value.isGone())
    }

    // --- reading is never writing ------------------------------------------------------------------------

    @Test
    fun theOversightScreensOnlyEverSendGets() = runTest {
        val id = groups.add("Live", "active", listOf("alice", "bob"))
        oversight.draw(id, "alice" to "bob")
        group(id)
        wishlist(id, "alice")
        questions(id)

        assertTrue(server.calls.all { it.startsWith("GET ") }, server.calls.toString())
    }

    @Test
    fun instantsThatCannotBeReadAreShownAsTheyCame() {
        assertEquals("yesterday", formatInstant("yesterday"))
        assertEquals("24 Dec 2026, 18:30 UTC", formatInstant("2026-12-24T18:30:15.123Z"))
    }
}
