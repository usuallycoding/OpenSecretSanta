package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.AnswerQuestion
import com.usuallycoding.opensecretsanta.domain.gift.AskQuestion
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionThreads
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ListQuestionThreadsTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val questions = InMemoryQuestions(groups)
    private val clock = FakeClock(now = 1_000L)
    private val participantLoader = ParticipantGroupLoader(groups)
    private val ownedLoader = OwnedGroupLoader(groups)

    private val ask = AskQuestion(participantLoader, questions, clock)
    private val answer = AnswerQuestion(participantLoader, questions, clock)
    private val threads = ListQuestionThreads(ownedLoader, groups, questions)

    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }

    // identity shuffle: alice -> bob -> carol -> dave -> alice (the first of each pair is the Santa)
    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        for (name in names) users.add(name)
    }

    private fun user(name: String) = users.userNamed(name)

    private fun activate(owner: User, vararg participants: String): Long {
        val g = groups.seed(owner.username, GroupState.PENDING, *participants.map { u(it) }.toTypedArray())
        ActivateGroup(ownedLoader, groups, AssignmentPlanner(IdentityShuffler()), GroupViewer(users))(owner, g.id)
        return g.id
    }

    private fun activeGroup() = activate(admin, *names.toTypedArray())

    @Test
    fun theOwnerSeesOneThreadPerPairInAssignmentOrderNamingBothPeople() {
        val id = activeGroup()
        ask(user("alice"), id, "Size?")

        val result = threads(admin, id)

        assertEquals(
            listOf("alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice"),
            result.map { it.santa.value to it.receiver.value },
        )
        assertEquals(listOf("Size?"), result.first().questions.map { it.text })
    }

    @Test
    fun aSantaWhoAskedNothingStillHasAnEmptyThread() {
        val id = activeGroup()

        val result = threads(admin, id)

        assertEquals(4, result.size)
        assertEquals(listOf(emptyList(), emptyList(), emptyList(), emptyList()), result.map { it.questions })
    }

    @Test
    fun answersAndTheirTimesAreShown() {
        val id = activeGroup()
        ask(user("alice"), id, "Size?")
        ask(user("alice"), id, "Colour?")
        clock.now = 2_000
        answer(user("bob"), id, 1, "M")
        clock.now = 5_000
        answer(user("bob"), id, 1, "L")

        val thread = threads(admin, id).first().questions

        assertEquals(listOf(1, 2), thread.map { it.number })
        assertEquals("L", thread[0].answer)
        assertEquals(2_000L, thread[0].answeredAtMillis)
        assertEquals(5_000L, thread[0].answerUpdatedAtMillis)
        assertNull(thread[1].answer)
    }

    @Test
    fun stillReadableOnceArchived() {
        val id = activeGroup()
        ask(user("alice"), id, "Size?")
        ArchiveGroup(ownedLoader, groups, GroupViewer(users))(admin, id)

        assertEquals(listOf("Size?"), threads(admin, id).first().questions.map { it.text })
    }

    @Test
    fun anOwnerWhoParticipatesSeesTheirOwnPairs() {
        users.add("owner-p", admin = true)
        val owner = users.userNamed("owner-p")
        val id = activate(owner, "owner-p", "alice", "bob", "carol")
        ask(owner, id, "What do you like?")

        val result = threads(owner, id)

        assertEquals("alice", result.first { it.santa.value == "owner-p" }.receiver.value)
        assertEquals("carol", result.first { it.receiver.value == "owner-p" }.santa.value) // their own Santa, named before the reveal
        assertEquals(4, result.size)
    }

    @Test
    fun notBeforeActivation() {
        for (state in listOf(GroupState.DRAFT, GroupState.PENDING)) {
            val id = groups.seed(admin.username, state, *names.map { u(it) }.toTypedArray()).id
            assertFailsWith<DomainException.InvalidState> { threads(admin, id) }
        }
    }

    @Test
    fun onlyTheOwnerMayRead() {
        val id = activeGroup()
        val other = users.add("admin-b", admin = true).let { users.userNamed("admin-b") }

        assertFailsWith<DomainException.NotFound> { threads(other, id) }
        assertFailsWith<DomainException.Forbidden> { threads(user("alice"), id) }
        assertFailsWith<DomainException.NotFound> { threads(admin, 999) }
    }
}
