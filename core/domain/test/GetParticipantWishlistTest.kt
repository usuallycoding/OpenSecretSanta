package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.wishlist.AddWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.GetParticipantWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.ReorderWishlist
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetParticipantWishlistTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val wishlists = InMemoryWishlists(groups)
    private val clock = FakeClock(now = 1_000L)
    private val participantLoader = ParticipantGroupLoader(groups)
    private val read = GetParticipantWishlist(OwnedGroupLoader(groups), wishlists)
    private val add = AddWishlistItem(participantLoader, wishlists, clock)

    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }
    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        for (name in names) users.add(name)
    }

    private fun user(name: String) = users.userNamed(name)

    private fun group(state: GroupState) = groups.seed(admin.username, state, *names.map { u(it) }.toTypedArray()).id

    private fun titles(group: Long, who: String) = read(admin, group, who).map { it.title }

    @Test
    fun theOwnerReadsAListInTheParticipantsOrderInEveryStateAfterDraft() {
        for (state in listOf(GroupState.PENDING, GroupState.ACTIVE, GroupState.ARCHIVED)) {
            val id = group(GroupState.PENDING)
            add(user("bob"), id, "First", "Grey", "https://shop.example")
            add(user("bob"), id, "Second", null, null)
            ReorderWishlist(participantLoader, wishlists)(
                user("bob"), id, wishlists.list(id, u("bob")).map { it.id }.reversed(),
            )
            groups.setState(id, state)

            assertEquals(listOf("Second", "First"), titles(id, "bob"), "state $state")
        }
    }

    @Test
    fun theViewCarriesTimesAndNoId() {
        val id = group(GroupState.PENDING)
        add(user("bob"), id, "Socks", "Grey", "https://shop.example")

        assertEquals(
            "SharedWishlistItem(title=Socks, note=Grey, url=https://shop.example, createdAtMillis=1000, updatedAtMillis=1000)",
            read(admin, id, "bob").single().toString(),
        )
    }

    @Test
    fun anEmptyListIsAnEmptyList() {
        assertEquals(emptyList(), read(admin, group(GroupState.ACTIVE), "alice"))
    }

    @Test
    fun usernameCaseDoesNotMatter() {
        val id = group(GroupState.ACTIVE)
        add(user("bob"), id, "Socks", null, null)

        assertEquals(listOf("Socks"), titles(id, "BOB"))
    }

    @Test
    fun aDraftHasNoWishlists() {
        assertFailsWith<DomainException.InvalidState> { read(admin, group(GroupState.DRAFT), "alice") }
    }

    @Test
    fun aStrangerOrAnInvalidOrUnknownNameIsNotFound() {
        val id = group(GroupState.ACTIVE)
        users.add("erin")

        assertFailsWith<DomainException.NotFound> { read(admin, id, "erin") }
        assertFailsWith<DomainException.NotFound> { read(admin, id, "nobody") }
        assertFailsWith<DomainException.NotFound> { read(admin, id, "x") }
    }

    @Test
    fun onlyTheOwnerMayRead() {
        val id = group(GroupState.ACTIVE)
        val other = users.add("admin-b", admin = true).let { users.userNamed("admin-b") }

        assertFailsWith<DomainException.NotFound> { read(other, id, "alice") }
        assertFailsWith<DomainException.Forbidden> { read(user("alice"), id, "alice") }
        assertFailsWith<DomainException.NotFound> { read(admin, 999, "alice") }
    }

    @Test
    fun aNonParticipatingOwnerAndAParticipatingOwnerBothRead() {
        users.add("owner-p", admin = true)
        val owner = users.userNamed("owner-p")
        val id = groups.seed(owner.username, GroupState.ACTIVE, u("owner-p"), u("alice"), u("bob"), u("carol")).id
        add(user("alice"), id, "Tea", null, null)

        assertEquals(listOf("Tea"), read(owner, id, "alice").map { it.title })
    }
}
