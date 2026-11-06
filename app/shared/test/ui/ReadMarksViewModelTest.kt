package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.ReadMarkDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverViewModel
import com.usuallycoding.opensecretsanta.ui.letters.InboxViewModel
import com.usuallycoding.opensecretsanta.ui.gift.countBadgeText
import com.usuallycoding.opensecretsanta.ui.readmarks.Change
import com.usuallycoding.opensecretsanta.ui.readmarks.ReadMarksViewModel
import com.usuallycoding.opensecretsanta.ui.readmarks.UnreadCounts
import com.usuallycoding.opensecretsanta.ui.readmarks.known
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadMarksViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val marks get() = server.readMarks

    private fun groupIs(state: String) {
        server.gift.state = state
        server.groupAnswer = { HttpStatusCode.OK to """{"id":1,"name":"Office party","state":"$state","participants":["alice","bob","carol","dave"]}""" }
    }

    init {
        groupIs("active")
    }

    private val old = "2026-12-01T09:00:00Z"
    private val seen = "2026-12-04T10:00:00Z"
    private val newer = "2026-12-05T09:00:00Z"
    private val newest = "2026-12-06T09:00:00Z"

    private fun item(title: String, created: String, updated: String = created) = SharedWishlistItemDto(title, null, null, created, updated)

    private suspend fun wishlist() = ReceiverWishlistViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    private suspend fun answers() = ReceiverViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    private suspend fun inbox() = InboxViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    private fun changes(model: ReceiverWishlistViewModel) = model.state.value.value!!.associate { it.title to it.change }

    // --- the wishlist ---

    @Test
    fun newAndEditedItemsAreMarkedAndTheRestIsNot() = runTest {
        marks.wishlist = ReadMarkDto(2, seen)
        server.receiverWishlist = listOf(item("Old", old), item("Edited", old, newer), item("New", newer), item("Both", newer, newest))

        assertEquals(mapOf("Old" to null, "Edited" to Change.Edited, "New" to Change.New, "Both" to Change.New), changes(wishlist()))
    }

    @Test
    fun everythingIsNewWhenTheSantaNeverLooked() = runTest {
        server.receiverWishlist = listOf(item("One", old), item("Two", old))

        assertEquals(mapOf("One" to Change.New, "Two" to Change.New), changes(wishlist()))
    }

    @Test
    fun theBaselineIsFixedForTheVisitSoARefreshKeepsTheMarksAndMarksNewArrivals() = runTest {
        marks.wishlist = ReadMarkDto(1, seen)
        server.receiverWishlist = listOf(item("Old", old), item("New", newer))
        val model = wishlist()
        model.markSeen()?.join() // the server's mark moves on, as the next visit would see it
        server.receiverWishlist = listOf(item("Old", old), item("New", newer), item("Arrived", newest))

        model.refresh().join()

        assertEquals(mapOf("Old" to null, "New" to Change.New, "Arrived" to Change.New), changes(model))
        assertEquals(1, server.calls.count { it == "GET /groups/1/receiver/reads" }, "the baseline is asked for once")
    }

    @Test
    fun markingSendsTheNewestTimeShownOnceAndNeverTwiceForTheSameTime() = runTest {
        marks.wishlist = ReadMarkDto(1, seen)
        server.receiverWishlist = listOf(item("Old", old), item("Edited", old, newer), item("New", newest))
        val model = wishlist()

        model.markSeen()?.join()
        assertNull(model.markSeen())

        assertEquals(listOf("wishlist" to newest), marks.sent)
    }

    @Test
    fun anEmptyListSendsNothing() = runTest {
        server.receiverWishlist = emptyList()
        val model = wishlist()

        assertNull(model.markSeen())
        assertTrue(marks.sent.isEmpty())
    }

    @Test
    fun theNextVisitHasNothingMarkedBecauseTheServerMovedOn() = runTest {
        marks.wishlist = ReadMarkDto(1, seen)
        server.receiverWishlist = listOf(item("Old", old), item("New", newer))
        wishlist().markSeen()?.join()

        val second = wishlist()

        assertEquals(mapOf("Old" to null, "New" to null), changes(second))
    }

    @Test
    fun aChangeAfterTheLastVisitIsMarkedAgain() = runTest {
        marks.wishlist = ReadMarkDto(0, newer)
        server.receiverWishlist = listOf(item("Old", old), item("Seen", newer), item("Later", newest))

        assertEquals(mapOf("Old" to null, "Seen" to null, "Later" to Change.New), changes(wishlist()))
    }

    @Test
    fun whenTheServerCannotSayWhatWasSeenNothingIsMarkedAndNothingIsSent() = runTest {
        marks.failing = true
        server.receiverWishlist = listOf(item("One", old), item("Two", newer))
        val model = wishlist()

        assertEquals(mapOf("One" to null, "Two" to null), changes(model))
        assertNull(model.markSeen())
        assertTrue(marks.sent.isEmpty())
    }

    @Test
    fun aFailedMarkLeavesTheScreenAsItWasAndTheMarksComeBack() = runTest {
        marks.wishlist = ReadMarkDto(1, seen)
        server.receiverWishlist = listOf(item("New", newer))
        val model = wishlist()
        marks.failing = true

        model.markSeen()?.join()

        assertEquals(mapOf("New" to Change.New), changes(model))
        assertEquals(seen, marks.wishlist.seenUpTo, "the failed mark changed nothing on the server")
        marks.failing = false
        assertEquals(mapOf("New" to Change.New), changes(wishlist()))
    }

    // --- the answers ---

    @Test
    fun anAnswerGivenAfterTheBaselineIsNewAndOlderOnesAreNot() = runTest {
        marks.answers = ReadMarkDto(1, seen)
        server.gift.askedBehindTheScenes(3)
        server.gift.receiverAnswers(1, "Long ago")
        server.gift.receiverAnswers(2, "Just now")

        val model = answers()

        val questions = model.data.value.value!!.questions
        assertEquals(3, questions.size)
        assertEquals(listOf(false, false, false), questions.map { it.answerIsNew }, "fake answers are dated 3 Dec, before the baseline")
    }

    @Test
    fun withNoTimeSeenEveryAnswerIsNewButAnUnansweredQuestionIsNot() = runTest {
        server.gift.askedBehindTheScenes(2)
        server.gift.receiverAnswers(1, "Yes")

        val questions = answers().data.value.value!!.questions

        assertEquals(listOf(true, false), questions.map { it.answerIsNew })
    }

    @Test
    fun markingTheAnswersSendsTheNewestAnswerTimeAndOnlyForAnswers() = runTest {
        server.gift.askedBehindTheScenes(2)
        server.gift.receiverAnswers(1, "Yes")
        val model = answers()

        model.markSeen()?.join()

        assertEquals(listOf("answers" to "2026-12-03T09:00:00Z"), marks.sent)
    }

    @Test
    fun noAnswersMeansNothingToMark() = runTest {
        server.gift.askedBehindTheScenes(2)
        val model = answers()

        assertNull(model.markSeen())
    }

    // --- the letters ---

    @Test
    fun newLettersAreMarkedNewestFirstAndSeenOnesAreNot() = runTest {
        marks.letters = ReadMarkDto(1, "2026-12-04T09:00:00Z")
        server.letters.receiverSends("Before", "x") // fake letters are dated 4 Dec 09:00, which is not after the baseline
        val model = inbox()

        assertEquals(listOf(false), model.data.value.value!!.letters.map { it.isNew })
    }

    @Test
    fun withNoTimeSeenEveryLetterIsNew() = runTest {
        server.letters.receiverSends("One", "x")
        server.letters.receiverSends("Two", "y")

        assertEquals(listOf(true, true), inbox().data.value.value!!.letters.map { it.isNew })
    }

    @Test
    fun markingTheLettersSendsTheNewestSentTimeOnce() = runTest {
        server.letters.receiverSends("One", "x")
        val model = inbox()

        model.markSeen()?.join()
        assertNull(model.markSeen())

        assertEquals(listOf("letters" to "2026-12-04T09:00:00Z"), marks.sent)
    }

    @Test
    fun anEmptyInboxSendsNothingAndAFailingServerMarksNothing() = runTest {
        assertNull(inbox().markSeen())

        marks.failing = true
        server.letters.receiverSends("One", "x")
        val model = inbox()
        assertEquals(listOf(false), model.data.value.value!!.letters.map { it.isNew })
        assertNull(model.markSeen())
        assertFalse(marks.sent.isNotEmpty())
    }
}

class UnreadCountsTest {
    private val server = ScriptedServer().also { it.session() }

    private suspend fun counts() = ReadMarksViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    @Test
    fun theCountsAreWhatTheServerSays() = runTest {
        server.readMarks.wishlist = ReadMarkDto(3, null)
        server.readMarks.letters = ReadMarkDto(2, null)

        assertEquals(UnreadCounts(wishlist = 3, answers = 0, letters = 2), counts().counts.value.known())
    }

    @Test
    fun aFailedLoadGivesNoCountsAndSoNoBadge() = runTest {
        server.readMarks.failing = true

        assertNull(counts().counts.value.known())
    }

    @Test
    fun badgesAndSpokenFormsFollowTheCounts() {
        assertNull(countBadgeText(0))
        assertEquals("9+", countBadgeText(12))
        assertEquals("See Wishlist, 3 new or edited", Strings.withNewOrEdited(Strings.SEE_WISHLIST, 3))
        assertEquals("Ask Questions, 1 new", Strings.withNew(Strings.ASK_QUESTIONS, 1))
        assertEquals("Inbox, 2 new", Strings.withNew(Strings.INBOX, 2))
    }
}
