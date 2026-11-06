package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.letters.InboxViewModel
import com.usuallycoding.opensecretsanta.ui.letters.LetterNotice
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LettersViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val letters get() = server.letters

    /** The group as the server says it is, for the letters and for the group's own answer. */
    private fun groupIs(state: String) {
        server.gift.state = state
        server.groupAnswer = { HttpStatusCode.OK to """{"id":1,"name":"Office party","state":"$state","participants":["alice","bob","carol","dave"]}""" }
    }

    init {
        groupIs("active")
    }

    private suspend fun send() = SendLetterViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    private suspend fun inbox() = InboxViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    private fun postBodies() = server.bodies.filterIndexed { index, _ -> server.calls[index].startsWith("POST /groups/1/santa/letters") }

    // --- the letters the person sent ---

    @Test
    fun theSentLettersAreShownNewestFirstInAnActiveGroup() = runTest {
        groupIs("active")
        letters.sentBehindTheScenes(3)

        val data = send().data.value.value!!

        assertEquals(listOf(3, 2, 1), data.letters.map { it.number })
        assertEquals(GroupPhase.Active, data.phase)
        assertEquals("4 Dec 2026, 11:00", data.letters.first().sent)
    }

    @Test
    fun noLettersIsAnEmptyList() = runTest {
        assertEquals(emptyList(), send().data.value.value!!.letters)
    }

    @Test
    fun aGroupThatIsNotDrawnCannotShowLetters() = runTest {
        groupIs("pending")

        val state = send().data.value

        assertNull(state.value)
        assertTrue(state.error != null)
    }

    @Test
    fun anArchivedGroupShowsTheLettersAndOffersNoWriting() = runTest {
        groupIs("archived")
        letters.sentBehindTheScenes(2)
        val model = send()

        assertEquals(2, model.data.value.value!!.letters.size)
        assertFalse(model.canSend)
        model.openSheet()
        assertNull(model.ui.value.editor)
    }

    @Test
    fun theSheetOpensEmptyOnlyWhileTheGroupIsActive() = runTest {
        val model = send()

        assertTrue(model.canSend)
        model.openSheet()

        assertEquals("", model.ui.value.editor!!.title)
        assertEquals("", model.ui.value.editor!!.text)
    }

    @Test
    fun aSentLetterIsAddedAtTheTopAndTheSheetCloses() = runTest {
        letters.sentBehindTheScenes(1)
        val model = send()
        model.openSheet()
        model.editTitle("  Thank you  ")
        model.editText("  For the scarf  ")

        model.save()?.join()

        assertNull(model.ui.value.editor)
        val shown = model.data.value.value!!.letters
        assertEquals(listOf(2, 1), shown.map { it.number })
        assertEquals("Thank you", shown.first().title)
        assertEquals("For the scarf", shown.first().text)
        assertTrue(postBodies().single().contains("\"title\":\"Thank you\""))
    }

    @Test
    fun bothFieldsAreRequiredAndEachProblemIsShownAtOnceWithNothingSent() = runTest {
        val model = send()
        model.openSheet()
        model.editTitle("   ")

        assertNull(model.save())

        assertEquals("Title is required.", model.ui.value.editor!!.titleError)
        assertEquals("Description is required.", model.ui.value.editor!!.textError)
        assertTrue(postBodies().isEmpty())
    }

    @Test
    fun aTitleOverTheLimitAndADescriptionOverTheLimitAreRefusedBeforeSending() = runTest {
        val model = send()
        model.openSheet()
        model.editTitle("t".repeat(101))
        model.editText("x".repeat(2001))

        assertNull(model.save())

        assertEquals("Must be 1 to 100 characters long.", model.ui.value.editor!!.titleError)
        assertEquals("Must be 1 to 2000 characters long.", model.ui.value.editor!!.textError)
        assertTrue(postBodies().isEmpty())
        model.editTitle("t".repeat(100))
        model.editText("x".repeat(2000))
        model.save()?.join()
        assertEquals(1, postBodies().size)
    }

    @Test
    fun editingAFieldClearsItsOwnProblem() = runTest {
        val model = send()
        model.openSheet()
        model.save()
        assertTrue(model.ui.value.editor!!.titleError != null && model.ui.value.editor!!.textError != null)

        model.editTitle("Hello")

        assertNull(model.ui.value.editor!!.titleError)
        assertTrue(model.ui.value.editor!!.textError != null)
    }

    @Test
    fun aValidationErrorFromTheServerLandsOnItsFieldAndKeepsWhatWasTyped() = runTest {
        val model = send()
        model.openSheet()
        model.editTitle("Hello")
        model.editText("Text")
        letters.failNextSendWith(HttpStatusCode.BadRequest, "validation_failed", "Title is not allowed.", field = "title")

        model.save()?.join()

        assertEquals("Title is not allowed.", model.ui.value.editor!!.titleError)
        assertEquals("Hello", model.ui.value.editor!!.title)
        assertEquals("Text", model.ui.value.editor!!.text)
        assertFalse(model.ui.value.editor!!.saving)
    }

    @Test
    fun theLimitReachedElsewhereReloadsAndSaysSo() = runTest {
        val model = send()
        model.openSheet()
        model.editTitle("One more")
        model.editText("Text")
        letters.sentBehindTheScenes(30)
        letters.failNextSendWith(HttpStatusCode.Conflict, "invalid_state", "The letter limit has been reached.")

        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(LetterNotice.LimitReached, model.ui.value.notice)
        assertEquals(30, model.data.value.value!!.letters.size)
        assertFalse(model.canSend)
    }

    @Test
    fun aGroupArchivedMeanwhileReloadsAndSaysLettersAreClosed() = runTest {
        val model = send()
        model.openSheet()
        model.editTitle("Late")
        model.editText("Text")
        groupIs("archived")
        letters.failNextSendWith(HttpStatusCode.Conflict, "invalid_state", "Letters can only be sent while the group is active.")

        model.save()?.join()

        assertEquals(LetterNotice.Locked, model.ui.value.notice)
        assertEquals(GroupPhase.Archived, model.data.value.value!!.phase)
    }

    @Test
    fun anUnreachableServerIsExplainedAndWhatWasTypedIsKept() = runTest {
        val model = send()
        model.openSheet()
        model.editTitle("Hello")
        model.editText("Text")
        server.unreachable = true

        model.save()?.join()

        assertEquals(Strings.SERVER_UNREACHABLE, model.ui.value.editor!!.general)
        assertEquals("Hello", model.ui.value.editor!!.title)
        assertFalse(model.ui.value.editor!!.saving)
    }

    @Test
    fun dismissingTheSheetSendsNothingAndForgetsWhatWasTyped() = runTest {
        val model = send()
        model.openSheet()
        model.editTitle("Never sent")

        model.dismissEditor()
        model.openSheet()

        assertEquals("", model.ui.value.editor!!.title)
        assertTrue(postBodies().isEmpty())
    }

    @Test
    fun aFailedRefreshKeepsTheLettersAndAReloadCanBeRetried() = runTest {
        letters.sentBehindTheScenes(2)
        val model = send()

        server.unreachable = true
        model.refresh().join()
        assertEquals(2, model.data.value.value!!.letters.size)
        assertTrue(model.data.value.error != null)

        server.unreachable = false
        model.refresh().join()
        assertNull(model.data.value.error)
    }

    // --- the letters from the person's receiver ---

    @Test
    fun theInboxShowsTheReceiversLettersNewestFirstWithTheirName() = runTest {
        letters.receiverSends("First", "one")
        letters.receiverSends("Second", "two")

        val data = inbox().data.value.value!!

        assertEquals(listOf("Second", "First"), data.letters.map { it.title })
        assertEquals("bob", data.receiver)
        assertEquals("Letters from bob", Strings.lettersFrom(data.receiver))
    }

    @Test
    fun anEmptyInboxIsAnEmptyList() = runTest {
        assertEquals(emptyList(), inbox().data.value.value!!.letters)
    }

    @Test
    fun theTitleFallsBackWhileTheReceiverIsNotKnown() = runTest {
        assertEquals("Letters from your receiver", Strings.lettersFrom(null))
    }

    @Test
    fun theInboxOfAGroupThatIsNotDrawnFailsAndCanBeRetried() = runTest {
        groupIs("pending")
        val model = inbox()
        assertNull(model.data.value.value)
        assertTrue(model.data.value.error != null)

        groupIs("active")
        letters.receiverSends("Hello", "x")
        model.refresh().join()

        assertEquals(1, model.data.value.value!!.letters.size)
    }

    @Test
    fun theInboxReadsOnlyAndNeverSends() = runTest {
        letters.receiverSends("Hello", "x")
        inbox()

        assertTrue(server.calls.filter { it.contains("/letters") }.all { it.startsWith("GET") })
        assertFalse(inbox().data.value.isGone())
    }
}
