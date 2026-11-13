package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.ReceiverDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.domain.gift.QuestionLimits
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
        assertEquals("bob@example.com", receiver.email)
        assertTrue(receiver.hasPostalDetails)
    }

    @Test
    fun aBlankEmailIsLeftOut() = runTest {
        server.gift.receiver = ReceiverDto("bob", email = "  ")

        val receiver = viewModel().data.value.value!!.receiver

        assertNull(receiver.email)
        assertFalse(receiver.hasPostalDetails)
    }

    @Test
    fun anEmailAloneCountsAsPostalDetails() = runTest {
        server.gift.receiver = ReceiverDto("bob", email = "bob@example.com")

        val receiver = viewModel().data.value.value!!.receiver

        assertEquals("bob@example.com", receiver.email)
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
    fun questionsAreShownNewestFirstWithAnswersWhereThereAreAny() = runTest {
        server.gift.askedBehindTheScenes(2)
        server.gift.receiverAnswers(1, "Yes")

        val questions = viewModel().data.value.value!!.questions

        assertEquals(listOf(2, 1), questions.map { it.number })
        assertEquals(listOf(null, "Yes"), questions.map { it.answer })
    }

    @Test
    fun noQuestionsIsAnEmptyListNotAnError() = runTest {
        val state = viewModel().data.value

        assertEquals(emptyList(), state.value!!.questions)
        assertNull(state.error)
    }

    // --- asking --------------------------------------------------------------------------------------

    @Test
    fun anAskedQuestionAppearsFirstAndTheSheetCloses() = runTest {
        server.gift.askedBehindTheScenes(1)
        val model = viewModel()

        model.openAsk()
        assertNotNull(model.ui.value.editor)
        model.edit("  Any allergies?  ")
        model.save()?.join()

        assertEquals(listOf("Any allergies?", "Question 1"), model.texts())
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
    fun atTheQuestionLimitAskingIsUnavailable() = runTest {
        server.gift.askedBehindTheScenes(QuestionLimits.MAX_QUESTIONS_PER_SANTA)
        val model = viewModel()

        assertFalse(model.canAsk)
        model.openAsk()
        assertNull(model.ui.value.editor)
    }

    @Test
    fun theLastQuestionCanStillBeAsked() = runTest {
        server.gift.askedBehindTheScenes(QuestionLimits.MAX_QUESTIONS_PER_SANTA - 1)
        val model = viewModel()

        assertTrue(model.canAsk)
        model.openAsk(); model.edit("Last one?"); model.save()?.join()

        assertEquals(QuestionLimits.MAX_QUESTIONS_PER_SANTA, model.data.value.value!!.questions.size)
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
        server.gift.askedBehindTheScenes(QuestionLimits.MAX_QUESTIONS_PER_SANTA)

        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(GiftNotice.LimitReached, model.ui.value.notice)
        assertEquals(QuestionLimits.MAX_QUESTIONS_PER_SANTA, model.data.value.value!!.questions.size)
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
        server.gift.askedBehindTheScenes(QuestionLimits.MAX_QUESTIONS_PER_SANTA)
        model.save()?.join()
        assertNotNull(model.ui.value.notice)

        model.dismissNotice()

        assertNull(model.ui.value.notice)
    }

    // --- deleting --------------------------------------------------------------------------------

    private fun deletes() = server.calls.count { it.startsWith("DELETE") }

    @Test
    fun anUnansweredQuestionIsDeletedAfterAConfirmationAndTheOthersKeepTheirNumbers() = runTest {
        server.gift.askedBehindTheScenes(3)
        val model = viewModel()

        model.requestDelete(2)
        assertEquals(2, model.ui.value.deleting?.number)
        model.confirmDelete()?.join()

        assertNull(model.ui.value.deleting)
        assertEquals(listOf(3, 1), model.data.value.value!!.questions.map { it.number })
        assertEquals(listOf(1, 3), server.gift.myQuestions.map { it.number })
        assertNull(model.ui.value.notice)
    }

    @Test
    fun cancellingTheConfirmationSendsNothing() = runTest {
        server.gift.askedBehindTheScenes(1)
        val model = viewModel()

        model.requestDelete(1)
        model.cancelDelete()

        assertNull(model.ui.value.deleting)
        assertNull(model.confirmDelete())
        assertEquals(0, deletes())
        assertEquals(1, model.data.value.value!!.questions.size)
    }

    @Test
    fun anAnsweredQuestionCannotBeDeleted() = runTest {
        server.gift.askedBehindTheScenes(2)
        server.gift.receiverAnswers(1, "Green")
        val model = viewModel()

        val answered = model.data.value.value!!.questions.first { it.number == 1 }
        val open = model.data.value.value!!.questions.first { it.number == 2 }
        assertFalse(model.canDelete(answered))
        assertTrue(model.canDelete(open))
        model.requestDelete(1)

        assertNull(model.ui.value.deleting)
    }

    @Test
    fun anArchivedGroupHasNothingToDelete() = runTest {
        server.gift.askedBehindTheScenes(1)
        val model = viewModel("archived")

        assertFalse(model.canDelete(model.data.value.value!!.questions.single()))
        model.requestDelete(1)

        assertNull(model.ui.value.deleting)
    }

    @Test
    fun aQuestionAnsweredMeanwhileStaysReloadsAndSaysWhy() = runTest {
        server.gift.askedBehindTheScenes(1)
        val model = viewModel()
        model.requestDelete(1)
        server.gift.receiverAnswers(1, "Green")

        model.confirmDelete()?.join()

        assertEquals(GiftNotice.QuestionAnswered, model.ui.value.notice)
        assertEquals("Green", model.data.value.value!!.questions.single().answer)
    }

    @Test
    fun aGroupArchivedMeanwhileReloadsAndSaysSo() = runTest {
        server.gift.askedBehindTheScenes(1)
        val model = viewModel()
        model.requestDelete(1)
        groupIs("archived")

        model.confirmDelete()?.join()

        assertEquals(GiftNotice.Locked, model.ui.value.notice)
        assertEquals(GroupPhase.Archived, model.data.value.value!!.phase)
        assertEquals(1, server.gift.myQuestions.size)
    }

    @Test
    fun aQuestionThatIsAlreadyGoneReloadsAndSaysSo() = runTest {
        server.gift.askedBehindTheScenes(2)
        val model = viewModel()
        model.requestDelete(1)
        server.gift.failNextWriteWith(HttpStatusCode.NotFound, "not_found", "Question not found.")

        model.confirmDelete()?.join()

        assertEquals(GiftNotice.QuestionGone, model.ui.value.notice)
    }

    @Test
    fun anUnreachableServerKeepsTheQuestionAndSaysSo() = runTest {
        server.gift.askedBehindTheScenes(1)
        val model = viewModel()
        model.requestDelete(1)
        server.unreachable = true

        model.confirmDelete()?.join()

        assertEquals(GiftNotice.Unreachable, model.ui.value.notice)
        assertEquals(1, model.data.value.value!!.questions.size)
    }

    @Test
    fun deletingFreesAPlaceAtTheLimit() = runTest {
        server.gift.askedBehindTheScenes(QuestionLimits.MAX_QUESTIONS_PER_SANTA)
        val model = viewModel()
        assertFalse(model.canAsk)

        model.requestDelete(5)
        model.confirmDelete()?.join()

        assertTrue(model.canAsk)
    }
}
