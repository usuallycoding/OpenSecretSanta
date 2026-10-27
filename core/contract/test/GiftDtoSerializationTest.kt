package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GiftDtoSerializationTest {
    private val json = Json
    private val withDefaults = Json { encodeDefaults = true }

    private inline fun <reified T> roundTrip(value: T) {
        assertEquals(value, json.decodeFromString<T>(json.encodeToString(value)))
        assertEquals(value, json.decodeFromString<T>(withDefaults.encodeToString(value)))
    }

    private inline fun <reified T> keys(value: T): Set<String> =
        (withDefaults.encodeToJsonElement(kotlinx.serialization.serializer<T>(), value) as JsonObject).keys

    private val question = QuestionDto(1, "What size?", "2026-12-01T10:15:30Z", "Medium", "2026-12-01T11:00:00Z", "2026-12-02T09:00:00Z")

    @Test
    fun everyDtoRoundTrips() {
        roundTrip(ReceiverDto("bob", "Bob Builder", "2 Side St", "555-0102", "Back door"))
        roundTrip(ReceiverDto("bob"))
        roundTrip(SantaRevealDto("alice", "Alice Archer"))
        roundTrip(SantaRevealDto("alice"))
        roundTrip(question)
        roundTrip(QuestionDto(2, "Unanswered?", "2026-12-01T10:15:30Z"))
        roundTrip(AskQuestionRequest("Any allergies?"))
        roundTrip(AnswerRequest("None"))
        roundTrip(AdminQuestionThreadDto("alice", "bob", listOf(question, QuestionDto(2, "Unanswered?", "2026-12-01T10:15:30Z"))))
        roundTrip(AdminQuestionThreadDto("alice", "bob", emptyList()))
    }

    @Test
    fun theOwnersThreadNamesBothPeopleAndEveryParticipantViewStillDoesNot() {
        assertEquals(setOf("santa", "receiver", "questions"), keys(AdminQuestionThreadDto("alice", "bob", emptyList())))
        assertFalse("santa" in keys(question))
    }

    @Test
    fun receiverDetailsExposeOnlyWhatIsNeededToPostAGift() {
        assertEquals(setOf("username", "fullName", "address", "phone", "deliveryInfo"), keys(ReceiverDto("bob")))
    }

    @Test
    fun receiverDetailsHaveNoEmailOrCredentialFields() {
        val text = withDefaults.encodeToString(ReceiverDto("bob", "Bob", "addr", "123", "info"))

        for (forbidden in listOf("email", "password", "hash", "isAdmin", "token")) assertFalse(text.contains(forbidden, ignoreCase = true), forbidden)
    }

    @Test
    fun theRevealCarriesOnlyUsernameAndFullName() {
        assertEquals(setOf("username", "fullName"), keys(SantaRevealDto("alice")))
    }

    @Test
    fun aQuestionHasNoKeyThatCouldHoldAnyonesIdentity() {
        assertEquals(
            setOf("number", "text", "askedAt", "answer", "answeredAt", "answerUpdatedAt"),
            keys(question),
        )
        val text = withDefaults.encodeToString(listOf(question, QuestionDto(2, "Unanswered?", "2026-12-01T10:15:30Z")))
        for (forbidden in listOf("santa", "receiver", "from", "user", "asker", "author")) assertFalse(text.contains("\"$forbidden", ignoreCase = true), forbidden)
    }

    @Test
    fun routeBuildersMatchTheirTemplates() {
        assertEquals("/groups/5/receiver", ApiRoutes.groupReceiver(5))
        assertEquals("/groups/5/santa", ApiRoutes.groupSanta(5))
        assertEquals("/groups/5/receiver/questions", ApiRoutes.groupReceiverQuestions(5))
        assertEquals("/groups/5/santa/questions", ApiRoutes.groupSantaQuestions(5))
        assertEquals("/groups/5/santa/questions/3/answer", ApiRoutes.groupSantaQuestionAnswer(5, 3))
        assertEquals(ApiRoutes.GROUP_SANTA_QUESTION_ANSWER.replace("{id}", "5").replace("{number}", "3"), ApiRoutes.groupSantaQuestionAnswer(5, 3))
    }
}
