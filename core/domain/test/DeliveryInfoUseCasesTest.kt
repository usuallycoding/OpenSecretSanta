package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfo
import com.usuallycoding.opensecretsanta.domain.delivery.GetMyDeliveryInfo
import com.usuallycoding.opensecretsanta.domain.delivery.UpdateMyDeliveryInfo
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DeliveryInfoUseCasesTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val infos = FakeDeliveryInfos()
    private val loader = ParticipantGroupLoader(groups)
    private val get = GetMyDeliveryInfo(loader, infos)
    private val update = UpdateMyDeliveryInfo(loader, infos)
    private val boss = users.add("boss", admin = true)

    private val alice = users.add("alice").let { users.userNamed("alice") }
    private val bob = users.add("bob").let { users.userNamed("bob") }

    private fun group(state: GroupState, vararg people: String) =
        groups.seed(boss, state, *people.map { u(it) }.toTypedArray())

    @Test
    fun aParticipantStartsWithEmptyDetails() {
        val g = group(GroupState.PENDING, "alice", "bob")

        assertEquals(DeliveryInfo.EMPTY, get(alice, g.id))
    }

    @Test
    fun theDetailsAreSavedAndReadBack() {
        val g = group(GroupState.PENDING, "alice", "bob")
        val info = DeliveryInfo.of("Alice B", "a@b.co", "123", "1 Main St", "Ring twice")

        update(alice, g.id, info)

        assertEquals(info, get(alice, g.id))
    }

    @Test
    fun aFieldCanBeCleared() {
        val g = group(GroupState.PENDING, "alice", "bob")
        update(alice, g.id, DeliveryInfo.of("Alice", null, "123", null, null))

        update(alice, g.id, DeliveryInfo.of("Alice", null, "  ", null, null))

        assertNull(get(alice, g.id).phone)
    }

    @Test
    fun theyCanBeChangedWhileActive() {
        val g = group(GroupState.ACTIVE, "alice", "bob")

        update(alice, g.id, DeliveryInfo(address = "2 Side St"))

        assertEquals("2 Side St", get(alice, g.id).address)
    }

    @Test
    fun anArchivedGroupIsReadableButNotChangeable() {
        val g = group(GroupState.ARCHIVED, "alice", "bob")
        infos.save(g.id, u("alice"), DeliveryInfo(address = "Kept"))

        assertFailsWith<DomainException.InvalidState> { update(alice, g.id, DeliveryInfo(address = "Too late")) }

        assertEquals("Kept", get(alice, g.id).address)
    }

    @Test
    fun oneGroupDoesNotTouchAnother() {
        val first = group(GroupState.PENDING, "alice", "bob")
        val second = group(GroupState.PENDING, "alice", "bob")

        update(alice, first.id, DeliveryInfo(address = "Home"))
        update(alice, second.id, DeliveryInfo(address = "Office"))

        assertEquals("Home", get(alice, first.id).address)
        assertEquals("Office", get(alice, second.id).address)
    }

    @Test
    fun onlyTheCallersOwnDetailsChange() {
        val g = group(GroupState.PENDING, "alice", "bob")

        update(alice, g.id, DeliveryInfo(fullName = "Alice only"))

        assertNull(get(bob, g.id).fullName)
    }

    @Test
    fun aDraftAStrangerAndAMissingGroupAreNotFound() {
        val draft = group(GroupState.DRAFT, "alice", "bob")
        val other = group(GroupState.PENDING, "bob")

        assertFailsWith<DomainException.NotFound> { get(alice, draft.id) }
        assertFailsWith<DomainException.NotFound> { update(alice, draft.id, DeliveryInfo.EMPTY) }
        assertFailsWith<DomainException.NotFound> { get(alice, other.id) }
        assertFailsWith<DomainException.NotFound> { update(alice, 9999, DeliveryInfo.EMPTY) }
    }
}
