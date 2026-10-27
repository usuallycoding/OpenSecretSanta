package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.Shuffler
import com.usuallycoding.opensecretsanta.domain.wishlist.AddWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.GetReceiverWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.UpdateWishlistItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetReceiverWishlistTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val wishlists = InMemoryWishlists(groups)
    private val clock = FakeClock(now = 1_000L)
    private val loader = ParticipantGroupLoader(groups)
    private val readReceiverList = GetReceiverWishlist(loader, groups, wishlists)
    private val add = AddWishlistItem(loader, wishlists, clock)
    private val update = UpdateWishlistItem(loader, wishlists, clock)

    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }
    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        for (name in names) users.add(name)
    }

    private fun user(name: String) = users.userNamed(name)

    /** Activates a group with the given shuffler: identity gives alice -> bob -> carol -> dave -> alice. */
    private fun activeGroup(shuffler: Shuffler = IdentityShuffler()): Long {
        val g = groups.seed(admin.username, GroupState.PENDING, *names.map { u(it) }.toTypedArray())
        ActivateGroup(OwnedGroupLoader(groups), groups, AssignmentPlanner(shuffler), GroupViewer(users))(admin, g.id)
        return g.id
    }

    private fun wish(group: Long, who: String, title: String) = add(user(who), group, title, null, null)

    private fun seen(group: Long, who: String) = readReceiverList(user(who), group).map { it.title }

    @Test
    fun aSantaReadsTheirReceiversListInTheReceiversOrder() {
        val id = activeGroup()
        wish(id, "bob", "First wish")
        wish(id, "bob", "Second wish")

        assertEquals(listOf("First wish", "Second wish"), seen(id, "alice")) // alice gives to bob
    }

    @Test
    fun everyoneReadsOnlyTheirOwnReceiversList() {
        val id = activeGroup()
        for (name in names) wish(id, name, "Wish of $name")

        assertEquals(
            listOf("Wish of bob", "Wish of carol", "Wish of dave", "Wish of alice"),
            names.map { seen(id, it).single() },
        )
    }

    @Test
    fun theViewHasNoIdsAndCarriesTimes() {
        val id = activeGroup()
        add(user("bob"), id, "Socks", "Grey", "https://shop.example")
        clock.now = 9_000
        val item = wishlists.list(id, u("bob")).single()
        update(user("bob"), id, item.id, "Socks, wool", "Grey", "https://shop.example")

        val shared = readReceiverList(user("alice"), id).single()

        assertEquals("Socks, wool", shared.title)
        assertEquals("Grey", shared.note)
        assertEquals(1_000L, shared.createdAtMillis)
        assertEquals(9_000L, shared.updatedAtMillis)
        assertEquals("SharedWishlistItem(title=Socks, wool, note=Grey, url=https://shop.example, createdAtMillis=1000, updatedAtMillis=9000)", shared.toString())
    }

    @Test
    fun changesAreSeenStraightAway() {
        val id = activeGroup()
        val item = wish(id, "bob", "Old")
        assertEquals(listOf("Old"), seen(id, "alice"))

        update(user("bob"), id, item.id, "New", null, null)
        wish(id, "bob", "Another")

        assertEquals(listOf("New", "Another"), seen(id, "alice"))
    }

    @Test
    fun anEmptyListIsEmpty() {
        assertEquals(emptyList(), seen(activeGroup(), "alice"))
    }

    @Test
    fun aReceiverWithDifferentListsInTwoGroupsIsReadSeparatelyByEachGroupsSanta() {
        val first = activeGroup(IdentityShuffler()) // alice -> bob
        val second = activeGroup(ReversingShuffler()) // dave -> carol -> bob -> alice -> dave: carol gives to bob
        wish(first, "bob", "Wish in the first group")
        wish(second, "bob", "Wish in the second group")

        assertEquals(listOf("Wish in the first group"), seen(first, "alice"))
        assertEquals(listOf("Wish in the second group"), seen(second, "carol"))
    }

    @Test
    fun theListIsStillReadableAfterArchive() {
        val id = activeGroup()
        wish(id, "bob", "Kept")
        groups.archive(id)

        assertEquals(listOf("Kept"), seen(id, "alice"))
    }

    @Test
    fun thereIsNoReceiverBeforeActivation() {
        val pending = groups.seed(admin.username, GroupState.PENDING, *names.map { u(it) }.toTypedArray())
        add(user("bob"), pending.id, "Prepared early", null, null)

        assertFailsWith<DomainException.InvalidState> { readReceiverList(user("alice"), pending.id) }
    }

    @Test
    fun draftsStrangersTheNonParticipatingOwnerAndUnknownGroupsAreNotFound() {
        val id = activeGroup()
        val draft = groups.seed(admin.username, GroupState.DRAFT, *names.map { u(it) }.toTypedArray())
        users.add("outsider")

        assertFailsWith<DomainException.NotFound> { readReceiverList(user("outsider"), id) }
        assertFailsWith<DomainException.NotFound> { readReceiverList(admin, id) }
        assertFailsWith<DomainException.NotFound> { readReceiverList(user("alice"), draft.id) }
        assertFailsWith<DomainException.NotFound> { readReceiverList(user("alice"), 9999) }
    }

    @Test
    fun readingChangesNothingForTheReceiver() {
        val id = activeGroup()
        wish(id, "bob", "Socks")
        val before = wishlists.list(id, u("bob"))

        repeat(5) { readReceiverList(user("alice"), id) }

        assertEquals(before, wishlists.list(id, u("bob")))
    }
}
