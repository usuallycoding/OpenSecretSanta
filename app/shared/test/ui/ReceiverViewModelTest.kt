package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.ReceiverDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.gift.GiftNotice
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverViewModel
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReceiverViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private fun groupIs(state: String) {
        server.gift.state = state
        server.groupAnswer = { HttpStatusCode.OK to """{"id":1,"name":"Office party","state":"$state","participants":["alice","bob","carol","dave"]}""" }
    }

    private suspend fun viewModel(state: String = "active"): ReceiverViewModel {
        groupIs(state)
        return ReceiverViewModel(1, server.api, immediateScope()).also { it.refresh().join() }
    }

    private fun ReceiverViewModel.texts() = data.value.value!!.questions.map { it.text }

    private fun writes() = server.calls.count { it.startsWith("POST") || it.startsWith("PUT") }

    // --- the receiver ----------------------------------------------------------------------------

    @Test
    fun theReceiversPostalDetailsAreShown() = runTest {
        val receiver = viewModel().data.value.value!!.receiver

        assertEquals("bob", receiver.username)
        assertEquals("Bob Builder", receiver.fullName)
        assertEquals("1 Main Street", receiver.address)
        assertEquals("555-0100", receiver.phone)
        assertEquals("Ring twice", receiver.deliveryInfo)
        assertTrue(receiver.hasPostalDetails)
    }

    @Test
    fun detailsTheReceiverHasNotSetAreLeftOut() = runTest {
        server.gift.receiver = ReceiverDto("bob", fullName = "Bob Builder", address = "  ", phone = null, deliveryInfo = "")

        val receiver = viewModel().data.value.value!!.receiver

        assertEquals("Bob Builder", receiver.fullName)
        assertNull(receiver.address)
        assertNull(receiver.phone)
        assertNull(receiver.deliveryInfo)
        assertTrue(receiver.hasPostalDetails)
    }

    @Test
    fun aReceiverWithNoDetailsHasNoPostalDetails() = runTest {
        server.gift.receiver = ReceiverDto("bob")

        assertFalse(viewModel().data.value.value!!.receiver.hasPostalDetails)
    }

    @Test
    fun beforeTheDrawThereIsNothingToShowAndItIsAnErrorNotAScreen() = runTest {
        val model = viewModel("pending")

        assertNull(model.data.value.value)
        assertEquals(409, (model.data.value.error as ApiError.Rejected).status)
        assertFalse(model.canAsk)
    }

    @Test
    fun aGroupThatIsGoneIsReportedAsGone() = runTest {
        server.groupAnswer = { HttpStatusCode.NotFound to """{"code":"not_found","message":"Group not found."}""" }

        val model = ReceiverViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        assertTrue(model.data.value.isGone())
    }

    @Test
    fun anUnreachableServerCanBeRetried() = runTest {
        groupIs("active")
        server.unreachable = true
        val model = ReceiverViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        assertEquals(ApiError.Network, model.data.value.error)

        server.unreachable = false
        model.refresh().join()

        assertEquals("bob", model.data.value.value!!.receiver.username)
    }

    // --- the questions ------------------------------------------------------------------------------

    @Test
    fun questionsAreShownInOrderWithAnswersWhereThereAreAny() = runTest {
        server.gift.askedBehindTheScenes(2)
        server.gift.receiverAnswers(1, "Yes")

        val questions = viewModel().data.value.value!!.questions

        assertEquals(listOf(1, 2), questions.map { it.number })
        assertEquals(listOf("Yes", null), questions.map { it.answer })
    }

    @Test
    fun noQuestionsIsAnEmptyListNotAnError() = runTest {
        val state = viewModel().data.value

        assertEquals(emptyList(), state.value!!.questions)
        assertNull(state.error)
    }

    // --- asking --------------------------------------------------------------------------------------

    @Test
    fun anAskedQuestionAppearsLastAndTheSheetCloses() = runTest {
        server.gift.askedBehindTheScenes(1)
        val model = viewModel()

        model.openAsk()
        assertNotNull(model.ui.value.editor)
        model.edit("  Any allergies?  ")
        model.save()?.join()

        assertEquals(listOf("Question 1", "Any allergies?"), model.texts())
        assertNull(model.ui.value.editor)
        assertEquals(2, server.gift.myQuestions.size)
    }

    @Test
    fun aBlankOrTooLongQuestionIsNotSentAndTheSheetSaysWhy() = runTest {
        val model = viewModel()
        model.openAsk()

        model.edit("   ")
        assertNull(model.save())
        assertEquals("Must be 1 to 1000 characters long.", model.ui.value.editor!!.error)

        model.edit("x".repeat(1001))
        assertNull(model.save())
        assertEquals("Must be 1 to 1000 characters long.", model.ui.value.editor!!.error)
        assertEquals(0, writes())

        model.edit("x".repeat(1000))
        assertNull(model.ui.value.editor!!.error)
        model.save()?.join()
        assertEquals(1, writes())
    }

    @Test
    fun theSheetOpensOnlyWhileTheGroupIsActive() = runTest {
        val archived = viewModel("archived")
        assertFalse(archived.canAsk)
        archived.openAsk()
        assertNull(archived.ui.value.editor)
        assertEquals(GroupPhase.Archived, archived.data.value.value!!.phase)

        assertTrue(viewModel("active").canAsk)
    }

    @Test
    fun anArchivedGroupStillShowsItsQuestionsAndAnswers() = runTest {
        server.gift.askedBehindTheScenes(1)
        server.gift.receiverAnswers(1, "Yes")

        val model = viewModel("archived")

        assertEquals(listOf("Question 1"), model.texts())
        assertEquals("Yes", model.data.value.value!!.questions.single().answer)
    }

    @Test
    fun atThirtyQuestionsAskingIsUnavailable() = runTest {
        server.gift.askedBehindTheScenes(30)
        val model = viewModel()

        assertFalse(model.canAsk)
        model.openAsk()
        assertNull(model.ui.value.editor)
    }

    @Test
    fun theThirtiethQuestionCanStillBeAsked() = runTest {
        server.gift.askedBehindTheScenes(29)
        val model = viewModel()

        assertTrue(model.canAsk)
        model.openAsk(); model.edit("Last one?"); model.save()?.join()

        assertEquals(30, model.data.value.value!!.questions.size)
        assertFalse(model.canAsk)
    }

    @Test
    fun aGroupArchivedWhileAskingClosesTheSheetReloadsAndSaysSo() = runTest {
        val model = viewModel()
        model.openAsk(); model.edit("Too late?")
        groupIs("archived")

        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(GiftNotice.Locked, model.ui.value.notice)
        assertEquals(GroupPhase.Archived, model.data.value.value!!.phase)
        assertFalse(model.canAsk)
        assertEquals(emptyList(), server.gift.myQuestions)
    }

    @Test
    fun theLimitReachedOnAnotherDeviceReloadsAndSaysSo() = runTest {
        val model = viewModel()
        model.openAsk(); model.edit("One more?")
        server.gift.askedBehindTheScenes(30)

        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(GiftNotice.LimitReached, model.ui.value.notice)
        assertEquals(30, model.data.value.value!!.questions.size)
        assertFalse(model.canAsk)
    }

    @Test
    fun aServerFieldErrorKeepsTheSheetAndWhatWasTyped() = runTest {
        val model = viewModel()
        model.openAsk(); model.edit("Fine?")
        server.gift.failNextWriteWith(HttpStatusCode.BadRequest, "validation_failed", "Text is not acceptable.", "text")

        model.save()?.join()

        assertEquals("Fine?", model.ui.value.editor!!.text)
        assertEquals("Text is not acceptable.", model.ui.value.editor!!.error)
        assertFalse(model.ui.value.editor!!.saving)
    }

    @Test
    fun aNetworkFailureKeepsTheSheetAndTheTextAndCanBeRetried() = runTest {
        val model = viewModel()
        model.openAsk(); model.edit("Any allergies?")
        server.unreachable = true

        model.save()?.join()

        assertEquals(Strings.SERVER_UNREACHABLE, model.ui.value.editor!!.general)
        assertEquals("Any allergies?", model.ui.value.editor!!.text)
        assertFalse(model.ui.value.editor!!.saving)

        server.unreachable = false
        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(listOf("Any allergies?"), model.texts())
    }

    @Test
    fun typingClearsTheErrorAndDismissingDiscardsTheText() = runTest {
        val model = viewModel()
        model.openAsk(); model.edit(" "); model.save()
        assertNotNull(model.ui.value.editor!!.error)

        model.edit("a")
        assertNull(model.ui.value.editor!!.error)
        model.dismissEditor()
        assertNull(model.ui.value.editor)
        model.openAsk()
        assertEquals("", model.ui.value.editor!!.text)
    }

    @Test
    fun aNoticeCanBeDismissed() = runTest {
        val model = viewModel()
        model.openAsk(); model.edit("x")
        server.gift.askedBehindTheScenes(30)
        model.save()?.join()
        assertNotNull(model.ui.value.notice)

        model.dismissNotice()

        assertNull(model.ui.value.notice)
    }
}
