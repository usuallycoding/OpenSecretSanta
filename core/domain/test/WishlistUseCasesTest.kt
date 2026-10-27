package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.wishlist.AddWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.DeleteWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.GetMyWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.ReorderWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.UpdateWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WishlistUseCasesTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val wishlists = InMemoryWishlists(groups)
    private val clock = FakeClock(now = 1_000L)
    private val loader = ParticipantGroupLoader(groups)

    private val list = GetMyWishlist(loader, wishlists)
    private val add = AddWishlistItem(loader, wishlists, clock)
    private val update = UpdateWishlistItem(loader, wishlists, clock)
    private val delete = DeleteWishlistItem(loader, wishlists)
    private val reorder = ReorderWishlist(loader, wishlists)

    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }
    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        for (name in names) users.add(name)
    }

    private fun user(name: String) = users.userNamed(name)

    private fun group(state: GroupState = GroupState.ACTIVE) =
        groups.seed(admin.username, state, *names.map { u(it) }.toTypedArray()).id

    private fun titles(group: Long, name: String = "alice") = list(user(name), group).map { it.title }

    private fun addTitle(group: Long, title: String, name: String = "alice") = add(user(name), group, title, null, null)

    // --- empty and independent ---

    @Test
    fun aNewListIsEmpty() {
        assertEquals(emptyList(), list(user("alice"), group()))
    }

    @Test
    fun oneUserKeepsIndependentListsInDifferentGroups() {
        val first = group()
        val second = group()

        addTitle(first, "Socks")
        addTitle(second, "Book")
        addTitle(second, "Mug")

        assertEquals(listOf("Socks"), titles(first))
        assertEquals(listOf("Book", "Mug"), titles(second))
    }

    @Test
    fun listsOfDifferentParticipantsInTheSameGroupAreSeparate() {
        val id = group()
        addTitle(id, "Alice's wish", "alice")
        addTitle(id, "Bob's wish", "bob")

        assertEquals(listOf("Alice's wish"), titles(id, "alice"))
        assertEquals(listOf("Bob's wish"), titles(id, "bob"))
    }

    // --- adding ---

    @Test
    fun newItemsGoToTheEnd() {
        val id = group()
        addTitle(id, "One")
        addTitle(id, "Two")
        addTitle(id, "Three")

        assertEquals(listOf("One", "Two", "Three"), titles(id))
    }

    @Test
    fun anItemRecordsItsValuesAndTimes() {
        val id = group()

        val item = add(user("alice"), id, " Wool socks ", "Grey, size M", "https://shop.example/socks")

        assertEquals("Wool socks", item.title)
        assertEquals("Grey, size M", item.note)
        assertEquals("https://shop.example/socks", item.url)
        assertEquals(1_000L, item.createdAtMillis)
        assertEquals(1_000L, item.updatedAtMillis)
    }

    @Test
    fun invalidInputIsRejectedAndNothingIsStored() {
        val id = group()

        assertEquals("title", assertFailsWith<DomainException.ValidationFailed> { add(user("alice"), id, " ", null, null) }.field)
        assertEquals("url", assertFailsWith<DomainException.ValidationFailed> { add(user("alice"), id, "Socks", null, "javascript:alert(1)") }.field)
        assertEquals(emptyList(), titles(id))
    }

    @Test
    fun aListHoldsAtMostFiftyItems() {
        val id = group()
        repeat(WishlistLimits.MAX_ITEMS) { addTitle(id, "Item $it") }

        assertFailsWith<DomainException.InvalidState> { addTitle(id, "One too many") }

        assertEquals(50, titles(id).size)
        addTitle(id, "Bob has his own list", "bob")
    }

    // --- editing and deleting ---

    @Test
    fun editingKeepsThePlaceAndCreationTimeAndUpdatesTheLastChangedTime() {
        val id = group()
        addTitle(id, "One")
        val second = addTitle(id, "Two")
        addTitle(id, "Three")
        clock.now = 5_000

        val edited = update(user("alice"), id, second.id, "Two, better", "A note", "https://shop.example/two")

        assertEquals(listOf("One", "Two, better", "Three"), titles(id))
        assertEquals(1_000L, edited.createdAtMillis)
        assertEquals(5_000L, edited.updatedAtMillis)
        assertEquals("A note", edited.note)
    }

    @Test
    fun anEditCanClearTheNoteAndLink() {
        val id = group()
        val item = add(user("alice"), id, "One", "note", "https://shop.example")

        val edited = update(user("alice"), id, item.id, "One", null, null)

        assertNull(edited.note)
        assertNull(edited.url)
    }

    @Test
    fun anInvalidEditLeavesTheItemAlone() {
        val id = group()
        val item = add(user("alice"), id, "One", null, null)

        assertFailsWith<DomainException.ValidationFailed> { update(user("alice"), id, item.id, "", null, null) }

        assertEquals(listOf("One"), titles(id))
    }

    @Test
    fun deletingKeepsTheOrderOfTheRest() {
        val id = group()
        addTitle(id, "One")
        val middle = addTitle(id, "Two")
        addTitle(id, "Three")

        delete(user("alice"), id, middle.id)

        assertEquals(listOf("One", "Three"), titles(id))
    }

    // --- ownership ---

    @Test
    fun someoneElsesItemIsNotFound() {
        val id = group()
        val bobs = addTitle(id, "Bob's", "bob")

        assertFailsWith<DomainException.NotFound> { update(user("alice"), id, bobs.id, "Mine now", null, null) }
        assertFailsWith<DomainException.NotFound> { delete(user("alice"), id, bobs.id) }
        assertEquals(listOf("Bob's"), titles(id, "bob"))
    }

    @Test
    fun anItemFromTheSameUsersOtherGroupIsNotFound() {
        val first = group()
        val second = group()
        val inFirst = addTitle(first, "Only in the first")

        assertFailsWith<DomainException.NotFound> { update(user("alice"), second, inFirst.id, "Moved", null, null) }
        assertFailsWith<DomainException.NotFound> { delete(user("alice"), second, inFirst.id) }
        assertEquals(listOf("Only in the first"), titles(first))
    }

    // --- reordering ---

    @Test
    fun aCompleteNewOrderIsApplied() {
        val id = group()
        val a = addTitle(id, "A")
        val b = addTitle(id, "B")
        val c = addTitle(id, "C")

        val result = reorder(user("alice"), id, listOf(c.id, a.id, b.id))

        assertEquals(listOf("C", "A", "B"), result.map { it.title })
        assertEquals(listOf("C", "A", "B"), titles(id))
    }

    @Test
    fun aWrongListOfIdsChangesNothing() {
        val id = group()
        val a = addTitle(id, "A")
        val b = addTitle(id, "B")
        val bobs = addTitle(id, "Bob's", "bob")
        val otherGroup = addTitle(group(), "Elsewhere")

        val attempts = listOf(
            listOf(a.id), // missing one
            listOf(a.id, a.id), // repeated
            listOf(a.id, b.id, a.id),
            listOf(a.id, b.id, bobs.id), // someone else's
            listOf(a.id, otherGroup.id), // from another group
            listOf(a.id, b.id, 9999L), // unknown
            emptyList(), // nothing
        )
        for (ids in attempts) {
            assertEquals("itemIds", assertFailsWith<DomainException.ValidationFailed>(ids.toString()) { reorder(user("alice"), id, ids) }.field)
        }
        assertEquals(listOf("A", "B"), titles(id))
    }

    @Test
    fun anOlderListIsRefusedAfterAnotherDeviceAddedAnItem() {
        val id = group()
        val a = addTitle(id, "A")
        val b = addTitle(id, "B")
        addTitle(id, "Added on the second device")

        assertFailsWith<DomainException.ValidationFailed> { reorder(user("alice"), id, listOf(b.id, a.id)) }

        assertEquals(listOf("A", "B", "Added on the second device"), titles(id))
    }

    @Test
    fun anEmptyListCanBeReorderedWithNoIds() {
        assertEquals(emptyList(), reorder(user("alice"), group(), emptyList()))
    }

    // --- group states ---

    @Test
    fun listsAreEditableWhilePendingAndActive() {
        for (state in listOf(GroupState.PENDING, GroupState.ACTIVE)) {
            val id = group(state)
            val item = addTitle(id, "One")
            update(user("alice"), id, item.id, "One, edited", null, null)
            reorder(user("alice"), id, listOf(item.id))
            delete(user("alice"), id, item.id)
            assertEquals(emptyList(), titles(id), state.toString())
        }
    }

    @Test
    fun anArchivedListIsReadOnlyButReadable() {
        val id = group()
        val item = addTitle(id, "Kept")
        groups.archive(id)

        assertFailsWith<DomainException.InvalidState> { addTitle(id, "New") }
        assertFailsWith<DomainException.InvalidState> { update(user("alice"), id, item.id, "Changed", null, null) }
        assertFailsWith<DomainException.InvalidState> { delete(user("alice"), id, item.id) }
        assertFailsWith<DomainException.InvalidState> { reorder(user("alice"), id, listOf(item.id)) }
        assertEquals(listOf("Kept"), titles(id))
    }

    @Test
    fun draftsStrangersTheNonParticipatingOwnerAndUnknownGroupsAreNotFound() {
        val draft = group(GroupState.DRAFT)
        val active = group()
        users.add("outsider")

        assertFailsWith<DomainException.NotFound> { list(user("alice"), draft) }
        assertFailsWith<DomainException.NotFound> { addTitle(draft, "x") }
        assertFailsWith<DomainException.NotFound> { list(user("outsider"), active) }
        assertFailsWith<DomainException.NotFound> { add(user("outsider"), active, "x", null, null) }
        assertFailsWith<DomainException.NotFound> { list(admin, active) }
        assertFailsWith<DomainException.NotFound> { list(user("alice"), 9999) }
    }
}
