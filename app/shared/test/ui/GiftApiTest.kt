package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.ReceiverDto
import com.usuallycoding.opensecretsanta.contract.SantaRevealDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** The receiver, question and reveal calls against a server that behaves like the real one (see [FakeGift]). */
class GiftApiTest {
    private val server = ScriptedServer().also { it.session() }
    private val api get() = server.api

    private fun <T> success(result: ApiResult<T>): T = (result as ApiResult.Success).value

    @Test
    fun theReceiverComesBackWithTheirPostalDetails() = runTest {
        val receiver = success(api.receiver(1))

        assertEquals(ReceiverDto("bob", "Bob Builder", "1 Main Street", "555-0100", "Ring twice"), receiver)
        assertEquals(listOf("GET /groups/1/receiver"), server.calls)
    }

    @Test
    fun theReceiverIsNotAvailableBeforeTheDraw() = runTest {
        server.gift.state = "pending"

        val result = api.receiver(1)

        assertEquals(ApiResult.Failure(ApiError.Rejected(409, "invalid_state", "Questions start once the group is active.")), result)
    }

    @Test
    fun aQuestionIsAskedWithTheTextAsBodyAndComesBackNumbered() = runTest {
        val first = success(api.askQuestion(1, "Any allergies?"))
        val second = success(api.askQuestion(1, "  Favourite colour?  "))

        assertEquals(1, first.number)
        assertEquals(2, second.number)
        assertEquals("Favourite colour?", second.text)
        assertEquals("POST /groups/1/receiver/questions", server.calls.last())
        assertEquals(AskQuestionRequest("  Favourite colour?  "), Json.decodeFromString(server.bodies.last()))
    }

    @Test
    fun theQuestionsIAskedComeBackWithTheirAnswers() = runTest {
        api.askQuestion(1, "One?")
        api.askQuestion(1, "Two?")
        server.gift.receiverAnswers(1, "Yes")

        val questions = success(api.receiverQuestions(1))

        assertEquals(listOf("One?", "Two?"), questions.map { it.text })
        assertEquals(listOf("Yes", null), questions.map { it.answer })
        assertEquals("GET /groups/1/receiver/questions", server.calls.last())
    }

    @Test
    fun theSantasQuestionsComeBackWithNoSignOfWhoAsked() = runTest {
        server.gift.santaAsks("Any allergies?")

        val questions = success(api.santaQuestions(1))

        assertEquals(listOf("Any allergies?"), questions.map { it.text })
        assertEquals("GET /groups/1/santa/questions", server.calls.last())
    }

    @Test
    fun anAnswerIsSetAndThenChangedKeepingTheFirstTime() = runTest {
        server.gift.santaAsks("Any allergies?")

        val first = success(api.answerQuestion(1, 1, "None"))
        val changed = success(api.answerQuestion(1, 1, "Nuts"))

        assertEquals("None", first.answer)
        assertEquals("Nuts", changed.answer)
        assertEquals(first.answeredAt, changed.answeredAt)
        assertEquals("PUT /groups/1/santa/questions/1/answer", server.calls.last())
        assertEquals(AnswerRequest("Nuts"), Json.decodeFromString(server.bodies.last()))
    }

    @Test
    fun aBlankQuestionOrAnswerNamesTheTextField() = runTest {
        server.gift.santaAsks("Any allergies?")

        assertEquals(ApiResult.Failure(ApiError.Validation("text", "Text must be 1 to 1000 characters long.")), api.askQuestion(1, "   "))
        assertEquals(ApiResult.Failure(ApiError.Validation("text", "Text must be 1 to 2000 characters long.")), api.answerQuestion(1, 1, ""))
    }

    @Test
    fun anUnknownQuestionIsNotFound() = runTest {
        assertEquals(ApiResult.Failure(ApiError.Rejected(404, "not_found", "Question not found.")), api.answerQuestion(1, 7, "x"))
    }

    @Test
    fun noMoreThanThirtyQuestionsCanBeAsked() = runTest {
        server.gift.askedBehindTheScenes(30)

        val result = api.askQuestion(1, "One more?")

        assertEquals(ApiResult.Failure(ApiError.Rejected(409, "invalid_state", "You have asked the most questions allowed.")), result)
    }

    @Test
    fun anArchivedGroupKeepsItsThreadsReadableButRefusesChanges() = runTest {
        server.gift.santaAsks("Any allergies?")
        server.gift.state = "archived"

        assertEquals(1, success(api.santaQuestions(1)).size)
        assertEquals(ApiResult.Failure(ApiError.Rejected(409, "invalid_state", "Questions and answers can only be written while the group is active.")), api.askQuestion(1, "x"))
        assertEquals(409, ((api.answerQuestion(1, 1, "x") as ApiResult.Failure).error as ApiError.Rejected).status)
    }

    @Test
    fun theSantaIsRevealedOnlyOnceTheGroupIsArchived() = runTest {
        assertEquals(409, ((api.santa(1) as ApiResult.Failure).error as ApiError.Rejected).status)

        server.gift.state = "archived"

        assertEquals(SantaRevealDto("carol", "Carol Santos"), success(api.santa(1)))
        assertEquals("GET /groups/1/santa", server.calls.last())
    }

    @Test
    fun theQuestionShapeHasNoFieldThatCouldNameAnyone() {
        val descriptor = QuestionDto.serializer().descriptor
        val names = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }.toSet()

        assertEquals(setOf("number", "text", "askedAt", "answer", "answeredAt", "answerUpdatedAt"), names)
    }
}
