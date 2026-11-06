package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfo
import com.usuallycoding.opensecretsanta.domain.delivery.GetParticipantDeliveryInfo
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetParticipantDeliveryInfoTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val infos = FakeDeliveryInfos()
    private val read = GetParticipantDeliveryInfo(OwnedGroupLoader(groups), infos)

    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }
    private val names = listOf("alice", "bob", "carol", "dave")
    private val bobs = DeliveryInfo("Bob Brown", "bob@example.com", "0700", "1 Main St", "Ring twice")

    init {
        for (name in names) users.add(name)
    }

    private fun group(state: GroupState) = groups.seed(admin.username, state, *names.map { u(it) }.toTypedArray()).id

    @Test
    fun theOwnerReadsEveryFieldTheParticipantSet() {
        val id = group(GroupState.ACTIVE)
        infos.save(id, u("bob"), bobs)

        assertEquals(bobs, read(admin, id, "bob"))
    }

    @Test
    fun aParticipantWhoSavedNothingReadsAsEmptyDetails() {
        assertEquals(DeliveryInfo.EMPTY, read(admin, group(GroupState.ACTIVE), "alice"))
    }

    @Test
    fun itIsReadableInEveryStateAfterDraft() {
        for (state in listOf(GroupState.PENDING, GroupState.ACTIVE, GroupState.ARCHIVED)) {
            val id = group(state)
            infos.save(id, u("bob"), bobs)

            assertEquals(bobs, read(admin, id, "bob"), "state $state")
        }
    }

    @Test
    fun onlyThatGroupsDetailsAreRead() {
        val first = group(GroupState.ACTIVE)
        val second = group(GroupState.ACTIVE)
        infos.save(first, u("bob"), bobs)

        assertEquals(DeliveryInfo.EMPTY, read(admin, second, "bob"))
    }

    @Test
    fun usernameCaseDoesNotMatter() {
        val id = group(GroupState.ACTIVE)
        infos.save(id, u("bob"), bobs)

        assertEquals(bobs, read(admin, id, "BOB"))
    }

    @Test
    fun aDraftHasNoDeliveryInfo() {
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
        assertFailsWith<DomainException.Forbidden> { read(users.userNamed("alice"), id, "alice") }
        assertFailsWith<DomainException.NotFound> { read(admin, 999, "alice") }
    }

    @Test
    fun readingChangesNothing() {
        val id = group(GroupState.ACTIVE)
        infos.save(id, u("bob"), bobs)

        repeat(3) { read(admin, id, "bob") }

        assertEquals(bobs, infos.find(id, u("bob")))
        assertEquals(DeliveryInfo.EMPTY, infos.find(id, u("alice")))
    }
}
