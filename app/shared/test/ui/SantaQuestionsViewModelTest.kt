package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.gift.GiftNotice
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsViewModel
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

class SantaQuestionsViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private fun groupIs(state: String) {
        server.gift.state = state
        server.groupAnswer = { HttpStatusCode.OK to """{"id":1,"name":"Office party","state":"$state","participants":["alice","bob","carol","dave"]}""" }
    }

    private suspend fun viewModel(state: String = "active"): SantaQuestionsViewModel {
        groupIs(state)
        return SantaQuestionsViewModel(1, server.api, immediateScope()).also { it.refresh().join() }
    }

    private fun SantaQuestionsViewModel.answers() = data.value.value!!.questions.map { it.answer }

    private fun writes() = server.calls.count { it.startsWith("PUT") }

    @Test
    fun questionsAreShownInOrderWithThePersonsOwnAnswers() = runTest {
        server.gift.santaAsks("Any allergies?")
        server.gift.santaAsks("Favourite colour?")
        val model = viewModel()
        model.openAnswer(2); model.edit("Green"); model.save()?.join()

        val questions = model.data.value.value!!.questions

        assertEquals(listOf(1, 2), questions.map { it.number })
        assertEquals(listOf("Any allergies?", "Favourite colour?"), questions.map { it.text })
        assertEquals(listOf(null, "Green"), model.answers())
    }

    @Test
    fun nothingAskedIsAnEmptyListNotAnError() = runTest {
        val state = viewModel().data.value

        assertEquals(emptyList(), state.value!!.questions)
        assertNull(state.error)
    }

    @Test
    fun anAnswerIsSavedAndTheSheetCloses() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel()

        model.openAnswer(1)
        assertNotNull(model.ui.value.editor)
        assertEquals("", model.ui.value.editor!!.text)
        model.edit("  None  ")
        model.save()?.join()

        assertEquals(listOf("None"), model.answers())
        assertNull(model.ui.value.editor)
        assertEquals("None", server.gift.santaQuestions.single().answer)
    }

    @Test
    fun theSheetForAnAnsweredQuestionHoldsTheCurrentAnswerAndSavingReplacesIt() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel()
        model.openAnswer(1); model.edit("None"); model.save()?.join()

        model.openAnswer(1)
        assertEquals("None", model.ui.value.editor!!.text)
        model.edit("Nuts"); model.save()?.join()

        assertEquals(listOf("Nuts"), model.answers())
        assertEquals(2, writes())
    }

    @Test
    fun aBlankOrTooLongAnswerIsNotSentAndTheSheetSaysWhy() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel()
        model.openAnswer(1)

        model.edit("  ")
        assertNull(model.save())
        assertEquals("Must be 1 to 2000 characters long.", model.ui.value.editor!!.error)

        model.edit("x".repeat(2001))
        assertNull(model.save())
        assertEquals(0, writes())

        model.edit("x".repeat(2000))
        model.save()?.join()
        assertEquals(1, writes())
    }

    @Test
    fun anArchivedGroupShowsTheThreadAndAllowsNothing() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel("archived")

        assertFalse(model.canAnswer)
        model.openAnswer(1)
        assertNull(model.ui.value.editor)
        assertEquals(GroupPhase.Archived, model.data.value.value!!.phase)
        assertEquals(1, model.data.value.value!!.questions.size)
    }

    @Test
    fun aQuestionThatIsNotThereOpensNothing() = runTest {
        val model = viewModel()

        model.openAnswer(5)

        assertNull(model.ui.value.editor)
    }

    @Test
    fun aGroupArchivedWhileAnsweringClosesTheSheetReloadsAndSaysSo() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel()
        model.openAnswer(1); model.edit("None")
        groupIs("archived")

        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(GiftNotice.Locked, model.ui.value.notice)
        assertFalse(model.canAnswer)
        assertEquals(listOf<String?>(null), model.answers())
    }

    @Test
    fun aQuestionThatIsGoneClosesTheSheetReloadsAndSaysSo() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel()
        model.openAnswer(1); model.edit("None")
        server.gift.failNextWriteWith(HttpStatusCode.NotFound, "not_found", "Question not found.")

        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(GiftNotice.QuestionGone, model.ui.value.notice)
    }

    @Test
    fun aServerFieldErrorKeepsTheSheetAndWhatWasTyped() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel()
        model.openAnswer(1); model.edit("None")
        server.gift.failNextWriteWith(HttpStatusCode.BadRequest, "validation_failed", "Text is not acceptable.", "text")

        model.save()?.join()

        assertEquals("None", model.ui.value.editor!!.text)
        assertEquals("Text is not acceptable.", model.ui.value.editor!!.error)
    }

    @Test
    fun aNetworkFailureKeepsTheSheetAndTheTextAndCanBeRetried() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel()
        model.openAnswer(1); model.edit("None")
        server.unreachable = true

        model.save()?.join()

        assertEquals(Strings.SERVER_UNREACHABLE, model.ui.value.editor!!.general)
        assertEquals("None", model.ui.value.editor!!.text)

        server.unreachable = false
        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(listOf("None"), model.answers())
    }

    @Test
    fun aGroupThatIsGoneIsReportedAsGone() = runTest {
        server.groupAnswer = { HttpStatusCode.NotFound to """{"code":"not_found","message":"Group not found."}""" }

        val model = SantaQuestionsViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        assertTrue(model.data.value.isGone())
    }

    @Test
    fun anUnreachableServerCanBeRetried() = runTest {
        server.gift.santaAsks("Any allergies?")
        groupIs("active")
        server.unreachable = true
        val model = SantaQuestionsViewModel(1, server.api, immediateScope()).also { it.refresh().join() }
        assertEquals(ApiError.Network, model.data.value.error)

        server.unreachable = false
        model.refresh().join()

        assertEquals(1, model.data.value.value!!.questions.size)
    }

    @Test
    fun nothingTheScreenHoldsNamesTheSanta() = runTest {
        server.gift.santaAsks("Any allergies?")
        val model = viewModel()
        model.openAnswer(1); model.edit("None"); model.save()?.join()

        val everything = "${model.data.value} ${model.ui.value}".lowercase()

        assertFalse("carol" in everything)
        assertFalse("santos" in everything)
    }
}
