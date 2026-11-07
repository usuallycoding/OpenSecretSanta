package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.AnswerQuestion
import com.usuallycoding.opensecretsanta.domain.gift.AskQuestion
import com.usuallycoding.opensecretsanta.domain.gift.GetMyReadMarks
import com.usuallycoding.opensecretsanta.domain.gift.MarkRead
import com.usuallycoding.opensecretsanta.domain.gift.ReadKind
import com.usuallycoding.opensecretsanta.domain.gift.SendLetter
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.wishlist.AddWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.DeleteWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.UpdateWishlistItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReadMarksTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val wishlists = InMemoryWishlists(groups)
    private val questions = InMemoryQuestions(groups)
    private val letters = InMemoryLetters(groups)
    private val marks = InMemoryReadMarks()
    private val clock = FakeClock(now = 1_000L)
    private val loader = ParticipantGroupLoader(groups)

    private val addItem = AddWishlistItem(loader, wishlists, clock)
    private val updateItem = UpdateWishlistItem(loader, wishlists, clock)
    private val deleteItem = DeleteWishlistItem(loader, wishlists)
    private val ask = AskQuestion(loader, questions, clock)
    private val answer = AnswerQuestion(loader, questions, clock)
    private val send = SendLetter(loader, letters, clock)
    private val read = GetMyReadMarks(loader, groups, marks, wishlists, questions, letters)
    private val mark = MarkRead(loader, groups, marks, wishlists, questions, letters, clock)

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
            ActivateGroup(OwnedGroupLoader(groups), groups, AssignmentPlanner(IdentityShuffler()), InMemoryDrawRules(), GroupViewer(users, FakeDeliveryInfos()))(admin, g.id)
        }
        if (state == GroupState.ARCHIVED) ArchiveGroup(OwnedGroupLoader(groups), groups, GroupViewer(users, FakeDeliveryInfos()))(admin, g.id)
        return g.id
    }

    private fun at(millis: Long) {
        clock.now = millis
    }

    @Test
    fun nothingMarkedYetCountsEverythingAndHasNoTimeSeen() {
        val id = group()
        at(2_000)
        repeat(3) { addItem(user("bob"), id, "Item $it", null, null) }
        send(user("bob"), id, "Hi", "x")

        val result = read(user("alice"), id)

        assertEquals(3, result.wishlist.unread)
        assertNull(result.wishlist.seenUpToMillis)
        assertEquals(1, result.letters.unread)
        assertNull(result.letters.seenUpToMillis)
        assertEquals(0, result.answers.unread)
    }

    @Test
    fun anEmptyGroupHasNothingUnread() {
        val result = read(user("alice"), group())

        assertEquals(0, result.wishlist.unread + result.answers.unread + result.letters.unread)
    }

    @Test
    fun newAndEditedItemsCountAndAnItemEditedManyTimesCountsOnce() {
        val id = group()
        at(2_000)
        val old = addItem(user("bob"), id, "Old", null, null)
        mark(user("alice"), id, ReadKind.WISHLIST, 2_500)
        at(3_000)
        addItem(user("bob"), id, "New", null, null)
        updateItem(user("bob"), id, old.id, "Old, edited", null, null)
        at(4_000)
        updateItem(user("bob"), id, old.id, "Old, edited again", null, null)

        assertEquals(2, read(user("alice"), id).wishlist.unread)
    }

    @Test
    fun aDeletedItemCountsForNothing() {
        val id = group()
        at(2_000)
        val item = addItem(user("bob"), id, "Gone", null, null)
        deleteItem(user("bob"), id, item.id)

        assertEquals(0, read(user("alice"), id).wishlist.unread)
    }

    @Test
    fun anAnswerGivenOrChangedAfterTheTimeSeenCounts() {
        val id = group()
        at(2_000)
        ask(user("alice"), id, "One?")
        ask(user("alice"), id, "Two?")
        answer(user("bob"), id, 1, "Yes")
        mark(user("alice"), id, ReadKind.ANSWERS, 2_000)
        at(3_000)
        answer(user("bob"), id, 2, "Maybe")
        answer(user("bob"), id, 1, "Yes, really")

        assertEquals(2, read(user("alice"), id).answers.unread)
    }

    @Test
    fun anUnansweredQuestionDoesNotCount() {
        val id = group()
        at(2_000)
        ask(user("alice"), id, "Unanswered?")

        assertEquals(0, read(user("alice"), id).answers.unread)
    }

    @Test
    fun lettersSentAfterTheTimeSeenCount() {
        val id = group()
        at(2_000)
        send(user("bob"), id, "Before", "x")
        mark(user("alice"), id, ReadKind.LETTERS, 2_000)
        at(3_000)
        send(user("bob"), id, "After one", "x")
        send(user("bob"), id, "After two", "x")

        assertEquals(2, read(user("alice"), id).letters.unread)
    }

    @Test
    fun markingLowersTheCountAndAnotherKindIsUntouched() {
        val id = group()
        at(2_000)
        addItem(user("bob"), id, "Item", null, null)
        send(user("bob"), id, "Letter", "x")
        at(3_000)

        val marked = mark(user("alice"), id, ReadKind.LETTERS, 2_000)

        assertEquals(0, marked.unread)
        assertEquals(2_000L, marked.seenUpToMillis)
        val all = read(user("alice"), id)
        assertEquals(1, all.wishlist.unread)
        assertNull(all.wishlist.seenUpToMillis)
        assertEquals(0, all.letters.unread)
    }

    @Test
    fun aTimeNeverGoesBackward() {
        val id = group()
        at(5_000)
        mark(user("alice"), id, ReadKind.WISHLIST, 4_000)

        val again = mark(user("alice"), id, ReadKind.WISHLIST, 1_000)

        assertEquals(4_000L, again.seenUpToMillis)
    }

    @Test
    fun aTimeInTheFutureBecomesNow() {
        val id = group()
        at(5_000)

        val marked = mark(user("alice"), id, ReadKind.WISHLIST, 9_999_999)

        assertEquals(5_000L, marked.seenUpToMillis)
    }

    @Test
    fun markingTwiceTheSameTimeChangesNothing() {
        val id = group()
        at(5_000)

        val first = mark(user("alice"), id, ReadKind.LETTERS, 3_000)
        val second = mark(user("alice"), id, ReadKind.LETTERS, 3_000)

        assertEquals(first, second)
    }

    @Test
    fun aSantaOnlySeesTheirOwnReceiversThings() {
        val id = group()
        at(2_000)
        send(user("bob"), id, "To alice", "x") // alice is bob's Santa
        send(user("carol"), id, "To bob", "x") // bob is carol's Santa

        assertEquals(1, read(user("alice"), id).letters.unread)
        assertEquals(1, read(user("bob"), id).letters.unread)
        assertEquals(0, read(user("carol"), id).letters.unread)
        mark(user("alice"), id, ReadKind.LETTERS, 2_000)
        assertEquals(1, read(user("bob"), id).letters.unread, "one Santa's mark is not another's")
    }

    @Test
    fun marksWorkInAnArchivedGroupAndNotBeforeTheDraw() {
        val id = group()
        at(2_000)
        send(user("bob"), id, "Hi", "x")
        ArchiveGroup(OwnedGroupLoader(groups), groups, GroupViewer(users, FakeDeliveryInfos()))(admin, id)

        assertEquals(1, read(user("alice"), id).letters.unread)
        assertEquals(0, mark(user("alice"), id, ReadKind.LETTERS, 2_000).unread)

        val pending = group(GroupState.PENDING)
        assertFailsWith<DomainException.InvalidState> { read(user("alice"), pending) }
        assertFailsWith<DomainException.InvalidState> { mark(user("alice"), pending, ReadKind.LETTERS, 1) }
    }

    @Test
    fun aStrangerAndAnUnknownGroupAreNotFound() {
        val id = group()
        users.add("erin")

        assertFailsWith<DomainException.NotFound> { read(user("erin"), id) }
        assertFailsWith<DomainException.NotFound> { mark(user("erin"), id, ReadKind.WISHLIST, 1) }
        assertFailsWith<DomainException.NotFound> { read(user("alice"), 999) }
    }

    @Test
    fun theKindsAreNamedAsTheRoutesNameThem() {
        assertEquals(ReadKind.WISHLIST, ReadKind.of("wishlist"))
        assertEquals(ReadKind.ANSWERS, ReadKind.of("answers"))
        assertEquals(ReadKind.LETTERS, ReadKind.of("letters"))
        assertNull(ReadKind.of("questions"))
        assertNull(ReadKind.of(null))
        assertNull(ReadKind.of("Wishlist"))
    }
}
