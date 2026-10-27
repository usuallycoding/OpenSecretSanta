package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.AnswerQuestion
import com.usuallycoding.opensecretsanta.domain.gift.AskQuestion
import com.usuallycoding.opensecretsanta.domain.gift.ListMyQuestions
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionsFromMySanta
import com.usuallycoding.opensecretsanta.domain.gift.QuestionLimits
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class QuestionsTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val questions = InMemoryQuestions(groups)
    private val clock = FakeClock(now = 1_000L)
    private val loader = ParticipantGroupLoader(groups)

    private val ask = AskQuestion(loader, questions, clock)
    private val mine = ListMyQuestions(loader, questions)
    private val fromSanta = ListQuestionsFromMySanta(loader, questions)
    private val answer = AnswerQuestion(loader, questions, clock)

    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }

    // identity shuffle: alice -> bob -> carol -> dave -> alice (alice is bob's Santa, bob is carol's, ...)
    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        for (name in names) users.add(name)
    }

    private fun user(name: String) = users.userNamed(name)

    private fun activeGroup(): Long {
        val g = groups.seed(admin.username, GroupState.PENDING, *names.map { u(it) }.toTypedArray())
        ActivateGroup(OwnedGroupLoader(groups), groups, AssignmentPlanner(IdentityShuffler()), GroupViewer(users))(admin, g.id)
        return g.id
    }

    private fun archive(id: Long) = ArchiveGroup(OwnedGroupLoader(groups), groups, GroupViewer(users))(admin, id)

    // --- asking ---

    @Test
    fun questionsAreNumberedFromOneInOrder() {
        val id = activeGroup()

        val first = ask(user("alice"), id, "What size are you?")
        clock.now = 2_000
        val second = ask(user("alice"), id, "Any allergies?")

        assertEquals(1, first.number)
        assertEquals(2, second.number)
        assertEquals("What size are you?", first.text)
        assertEquals(1_000L, first.askedAtMillis)
        assertEquals(2_000L, second.askedAtMillis)
        assertNull(first.answer)
    }

    @Test
    fun numberingIsPerSanta() {
        val id = activeGroup()

        val fromAlice = ask(user("alice"), id, "Question from alice")
        val fromBob = ask(user("bob"), id, "Question from bob")
        val secondFromAlice = ask(user("alice"), id, "Another from alice")

        assertEquals(listOf(1, 1, 2), listOf(fromAlice.number, fromBob.number, secondFromAlice.number))
    }

    @Test
    fun textIsTrimmedAndLimitedTo1000Characters() {
        val id = activeGroup()

        assertEquals("hello", ask(user("alice"), id, "  hello \n").text)
        assertEquals(1000, ask(user("alice"), id, "q".repeat(QuestionLimits.MAX_QUESTION_LENGTH)).text.length)
        for (bad in listOf("", "   ", "\n\t", "q".repeat(1001))) {
            val error = assertFailsWith<DomainException.ValidationFailed>(bad.take(10)) { ask(user("alice"), id, bad) }
            assertEquals("text", error.field)
        }
        assertEquals(2, mine(user("alice"), id).size)
    }

    @Test
    fun aSantaCanAskThirtyQuestionsButNotThirtyOne() {
        val id = activeGroup()
        repeat(30) { ask(user("alice"), id, "Question ${it + 1}") }

        assertFailsWith<DomainException.InvalidState> { ask(user("alice"), id, "One too many") }

        assertEquals(30, mine(user("alice"), id).size)
        assertEquals(1, ask(user("bob"), id, "Bob is not limited by alice").number) // the cap is per Santa
    }

    // --- states ---

    @Test
    fun questionsAreRefusedUnlessTheGroupIsActive() {
        val pending = groups.seed(admin.username, GroupState.PENDING, *names.map { u(it) }.toTypedArray())
        val id = activeGroup().also { ask(user("alice"), it, "asked while active") }
        archive(id)

        assertFailsWith<DomainException.InvalidState> { ask(user("alice"), pending.id, "too early") }
        assertFailsWith<DomainException.InvalidState> { ask(user("alice"), id, "too late") }
        assertFailsWith<DomainException.InvalidState> { mine(user("alice"), pending.id) }
        assertFailsWith<DomainException.InvalidState> { fromSanta(user("bob"), pending.id) }
    }

    @Test
    fun anArchivedThreadIsStillReadableButNotWritable() {
        val id = activeGroup()
        ask(user("alice"), id, "What do you like?")
        answer(user("bob"), id, 1, "Books")
        archive(id)

        assertEquals("Books", mine(user("alice"), id).single().answer)
        assertEquals("Books", fromSanta(user("bob"), id).single().answer)
        assertFailsWith<DomainException.InvalidState> { answer(user("bob"), id, 1, "Actually socks") }
        assertEquals("Books", mine(user("alice"), id).single().answer)
    }

    // --- answering ---

    @Test
    fun theFirstAnswerRecordsWhenItWasGiven() {
        val id = activeGroup()
        ask(user("alice"), id, "Favourite colour?")
        clock.now = 5_000

        val answered = answer(user("bob"), id, 1, "  Green  ")

        assertEquals("Green", answered.answer)
        assertEquals(5_000L, answered.answeredAtMillis)
        assertEquals(5_000L, answered.answerUpdatedAtMillis)
    }

    @Test
    fun editingAnAnswerKeepsTheOriginalTimeAndUpdatesTheLastChangedTime() {
        val id = activeGroup()
        ask(user("alice"), id, "Favourite colour?")
        clock.now = 5_000
        answer(user("bob"), id, 1, "Green")
        clock.now = 9_000

        val edited = answer(user("bob"), id, 1, "Blue")

        assertEquals("Blue", edited.answer)
        assertEquals(5_000L, edited.answeredAtMillis)
        assertEquals(9_000L, edited.answerUpdatedAtMillis)
        val seenBySanta = mine(user("alice"), id).single()
        assertEquals("Blue", seenBySanta.answer)
        assertEquals(9_000L, seenBySanta.answerUpdatedAtMillis)
    }

    @Test
    fun anAnswerCanBeChangedManyTimes() {
        val id = activeGroup()
        ask(user("alice"), id, "Favourite colour?")

        listOf("a", "b", "c").forEach { answer(user("bob"), id, 1, it) }

        assertEquals("c", fromSanta(user("bob"), id).single().answer)
    }

    @Test
    fun invalidAnswersAreRejectedAndTheOldAnswerSurvives() {
        val id = activeGroup()
        ask(user("alice"), id, "Favourite colour?")
        answer(user("bob"), id, 1, "Green")

        for (bad in listOf("", "  ", "a".repeat(2001))) {
            assertEquals("text", assertFailsWith<DomainException.ValidationFailed> { answer(user("bob"), id, 1, bad) }.field)
        }
        assertEquals("Green", fromSanta(user("bob"), id).single().answer)
        answer(user("bob"), id, 1, "a".repeat(QuestionLimits.MAX_ANSWER_LENGTH))
    }

    @Test
    fun anUnknownQuestionNumberIsNotFound() {
        val id = activeGroup()
        ask(user("alice"), id, "Favourite colour?")

        assertFailsWith<DomainException.NotFound> { answer(user("bob"), id, 2, "Green") }
        assertFailsWith<DomainException.NotFound> { answer(user("bob"), id, 0, "Green") }
    }

    @Test
    fun aReceiverOnlyEverAnswersTheirOwnSantasQuestions() {
        val id = activeGroup()
        ask(user("alice"), id, "Alice asks bob")
        ask(user("bob"), id, "Bob asks carol")

        answer(user("carol"), id, 1, "Carol answers bob")

        assertNull(mine(user("alice"), id).single().answer)
        assertEquals("Carol answers bob", mine(user("bob"), id).single().answer)
        assertFailsWith<DomainException.NotFound> { answer(user("carol"), id, 2, "no such question for carol") }
    }

    // --- the two threads ---

    @Test
    fun theReceiverSeesQuestionsWithoutAnyIdentityOfTheSanta() {
        val id = activeGroup()
        ask(user("alice"), id, "First")
        ask(user("alice"), id, "Second")
        answer(user("bob"), id, 2, "Reply")

        val received = fromSanta(user("bob"), id)

        assertEquals(listOf(1, 2), received.map { it.number })
        assertEquals(listOf("First", "Second"), received.map { it.text })
        assertEquals(listOf(null, "Reply"), received.map { it.answer })
        assertFalse(received.toString().contains("alice"), received.toString())
    }

    @Test
    fun oneParticipantsTwoThreadsNeverMix() {
        val id = activeGroup()
        ask(user("alice"), id, "From alice to bob")
        ask(user("bob"), id, "From bob to carol")

        assertEquals(listOf("From bob to carol"), mine(user("bob"), id).map { it.text })
        assertEquals(listOf("From alice to bob"), fromSanta(user("bob"), id).map { it.text })
    }

    @Test
    fun anEmptyThreadIsAnEmptyList() {
        val id = activeGroup()

        assertEquals(emptyList(), mine(user("alice"), id))
        assertEquals(emptyList(), fromSanta(user("bob"), id))
    }

    // --- who may use it ---

    @Test
    fun strangersDraftsAndTheNonParticipatingOwnerGetNotFound() {
        val id = activeGroup()
        users.add("outsider")
        val draft = groups.seed(admin.username, GroupState.DRAFT, *names.map { u(it) }.toTypedArray())

        assertFailsWith<DomainException.NotFound> { ask(user("outsider"), id, "hi") }
        assertFailsWith<DomainException.NotFound> { mine(user("outsider"), id) }
        assertFailsWith<DomainException.NotFound> { fromSanta(user("outsider"), id) }
        assertFailsWith<DomainException.NotFound> { answer(user("outsider"), id, 1, "hi") }
        assertFailsWith<DomainException.NotFound> { ask(admin, id, "hi") }
        assertFailsWith<DomainException.NotFound> { ask(user("alice"), draft.id, "hi") }
        assertFailsWith<DomainException.NotFound> { ask(user("alice"), 9999, "hi") }
    }
}
