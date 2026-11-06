package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.LetterLimits
import com.usuallycoding.opensecretsanta.domain.gift.ListLetterBoxes
import com.usuallycoding.opensecretsanta.domain.gift.ListLettersFromMyReceiver
import com.usuallycoding.opensecretsanta.domain.gift.ListMySentLetters
import com.usuallycoding.opensecretsanta.domain.gift.SendLetter
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
import kotlin.test.assertTrue

class LettersTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val letters = InMemoryLetters(groups)
    private val clock = FakeClock(now = 1_000L)
    private val loader = ParticipantGroupLoader(groups)

    private val send = SendLetter(loader, letters, clock)
    private val mine = ListMySentLetters(loader, letters)
    private val fromReceiver = ListLettersFromMyReceiver(loader, letters)
    private val boxes = ListLetterBoxes(OwnedGroupLoader(groups), groups, letters)

    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }

    // identity shuffle: alice gives to bob, bob to carol, carol to dave, dave to alice
    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        for (name in names) users.add(name)
    }

    private fun user(name: String) = users.userNamed(name)

    private fun group(state: GroupState = GroupState.ACTIVE): Long {
        val g = groups.seed(admin.username, GroupState.PENDING, *names.map { u(it) }.toTypedArray())
        if (state == GroupState.ACTIVE || state == GroupState.ARCHIVED) {
            ActivateGroup(OwnedGroupLoader(groups), groups, AssignmentPlanner(IdentityShuffler()), GroupViewer(users, FakeDeliveryInfos()))(admin, g.id)
        }
        if (state == GroupState.ARCHIVED) ArchiveGroup(OwnedGroupLoader(groups), groups, GroupViewer(users, FakeDeliveryInfos()))(admin, g.id)
        return g.id
    }

    @Test
    fun lettersAreNumberedFromOneInOrderAndTrimmed() {
        val id = group()

        val first = send(user("bob"), id, "  Hello  ", "  Thank you  ")
        clock.now = 2_000
        val second = send(user("bob"), id, "Again", "More thanks")

        assertEquals(1, first.number)
        assertEquals(2, second.number)
        assertEquals("Hello", first.title)
        assertEquals("Thank you", first.text)
        assertEquals(1_000L, first.sentAtMillis)
        assertEquals(2_000L, second.sentAtMillis)
    }

    @Test
    fun numberingIsPerReceiverAndPerGroup() {
        val id = group()
        val other = group()

        assertEquals(1, send(user("bob"), id, "t", "x").number)
        assertEquals(1, send(user("carol"), id, "t", "x").number)
        assertEquals(1, send(user("bob"), other, "t", "x").number)
        assertEquals(2, send(user("bob"), id, "t", "x").number)
    }

    @Test
    fun theTitleAndTheTextAreBothRequiredWithTheirLimits() {
        val id = group()

        for ((title, text, field) in listOf(
            Triple("", "text", "title"),
            Triple("  ", "text", "title"),
            Triple("t".repeat(LetterLimits.MAX_TITLE_LENGTH + 1), "text", "title"),
            Triple("title", "", "text"),
            Triple("title", "   ", "text"),
            Triple("title", "x".repeat(LetterLimits.MAX_TEXT_LENGTH + 1), "text"),
        )) {
            val error = assertFailsWith<DomainException.ValidationFailed> { send(user("bob"), id, title, text) }
            assertEquals(field, error.field)
        }
        assertEquals(emptyList(), mine(user("bob"), id))
        send(user("bob"), id, "t".repeat(LetterLimits.MAX_TITLE_LENGTH), "x".repeat(LetterLimits.MAX_TEXT_LENGTH))
    }

    @Test
    fun aReceiverCanSendAtMostThirtyLetters() {
        val id = group()
        repeat(LetterLimits.MAX_LETTERS_PER_RECEIVER) { send(user("bob"), id, "t$it", "x") }

        assertFailsWith<DomainException.InvalidState> { send(user("bob"), id, "one more", "x") }
        assertEquals(LetterLimits.MAX_LETTERS_PER_RECEIVER, mine(user("bob"), id).size)
        send(user("carol"), id, "carol can", "x")
    }

    @Test
    fun lettersAreSentOnlyWhileTheGroupIsActive() {
        assertFailsWith<DomainException.InvalidState> { send(user("bob"), group(GroupState.PENDING), "t", "x") }
        assertFailsWith<DomainException.InvalidState> { send(user("bob"), group(GroupState.ARCHIVED), "t", "x") }
    }

    @Test
    fun aStrangerAndAMissingGroupAreNotFound() {
        val id = group()
        users.add("erin")

        assertFailsWith<DomainException.NotFound> { send(user("erin"), id, "t", "x") }
        assertFailsWith<DomainException.NotFound> { send(user("bob"), 999, "t", "x") }
    }

    @Test
    fun theReceiverReadsTheirOwnLettersAndTheSantaReadsThemToo() {
        val id = group()
        send(user("bob"), id, "One", "x")
        send(user("bob"), id, "Two", "y")
        send(user("carol"), id, "From carol", "z")

        assertEquals(listOf("One", "Two"), mine(user("bob"), id).map { it.title })
        // alice is bob's Santa, bob is carol's: each reads only the letters of their own receiver
        assertEquals(listOf("One", "Two"), fromReceiver(user("alice"), id).map { it.title })
        assertEquals(listOf("From carol"), fromReceiver(user("bob"), id).map { it.title })
        assertEquals(emptyList(), fromReceiver(user("carol"), id))
    }

    @Test
    fun noLetterTypeCarriesAUsername() {
        val id = group()
        send(user("bob"), id, "Hi", "x")

        assertEquals("Letter(number=1, title=Hi, text=x, sentAtMillis=1000)", mine(user("bob"), id).single().toString())
    }

    @Test
    fun lettersAreReadableAfterTheArchiveAndNotBeforeTheDraw() {
        val id = group()
        send(user("bob"), id, "Kept", "x")
        ArchiveGroup(OwnedGroupLoader(groups), groups, GroupViewer(users, FakeDeliveryInfos()))(admin, id)

        assertEquals(listOf("Kept"), mine(user("bob"), id).map { it.title })
        assertEquals(listOf("Kept"), fromReceiver(user("alice"), id).map { it.title })

        val pending = group(GroupState.PENDING)
        assertFailsWith<DomainException.InvalidState> { mine(user("bob"), pending) }
        assertFailsWith<DomainException.InvalidState> { fromReceiver(user("alice"), pending) }
    }

    @Test
    fun theOwnerReadsEveryPairInAssignmentOrderIncludingEmptyOnes() {
        val id = group()
        send(user("bob"), id, "From bob", "x")

        val result = boxes(admin, id)

        assertEquals(listOf("alice" to "bob", "bob" to "carol", "carol" to "dave", "dave" to "alice"), result.map { it.santa.value to it.receiver.value })
        assertEquals(listOf("From bob"), result.first().letters.map { it.title })
        assertTrue(result.drop(1).all { it.letters.isEmpty() })
    }

    @Test
    fun theOwnerReadsAnArchivedGroupButNotOneThatIsNotDrawn() {
        val archived = group(GroupState.ARCHIVED)
        assertEquals(4, boxes(admin, archived).size)

        assertFailsWith<DomainException.InvalidState> { boxes(admin, group(GroupState.PENDING)) }
    }

    @Test
    fun onlyTheOwnerMayReadTheBoxes() {
        val id = group()
        val other = users.add("admin-b", admin = true).let { users.userNamed("admin-b") }

        assertFailsWith<DomainException.NotFound> { boxes(other, id) }
        assertFailsWith<DomainException.Forbidden> { boxes(user("alice"), id) }
        assertFailsWith<DomainException.NotFound> { boxes(admin, 999) }
    }

    @Test
    fun readingAsTheOwnerChangesNothingForTheParticipants() {
        val id = group()
        send(user("bob"), id, "Hi", "x")
        val before = mine(user("bob"), id) to fromReceiver(user("alice"), id)

        repeat(3) { boxes(admin, id) }

        assertEquals(before, mine(user("bob"), id) to fromReceiver(user("alice"), id))
    }
}
