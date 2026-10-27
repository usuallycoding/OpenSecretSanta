package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.GetMyReceiver
import com.usuallycoding.opensecretsanta.domain.gift.GetMySanta
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.Profile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GiftVisibilityTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val loader = ParticipantGroupLoader(groups)
    private val receiverOf = GetMyReceiver(loader, groups, users)
    private val santaOf = GetMySanta(loader, groups, users)

    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }

    // identity shuffle: alice -> bob -> carol -> dave -> alice
    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        for (name in names) users.add(name)
        users.updateProfile(u("bob"), Profile("Bob Builder", "bob@example.com", "555-0102", "2 Side St", "Leave at the back door"))
        users.updateProfile(u("alice"), Profile(fullName = "Alice Archer", address = "1 Main St"))
    }

    private fun user(name: String) = users.userNamed(name)

    private fun group(state: GroupState, vararg participants: String) =
        groups.seed(admin.username, state, *participants.map { u(it) }.toTypedArray())

    private fun activeGroup(): Long {
        val g = group(GroupState.PENDING, *names.toTypedArray())
        ActivateGroup(OwnedGroupLoader(groups), groups, AssignmentPlanner(IdentityShuffler()), GroupViewer(users))(admin, g.id)
        return g.id
    }

    private fun archive(id: Long) = ArchiveGroup(OwnedGroupLoader(groups), groups, GroupViewer(users))(admin, id)

    // --- receiver ---

    @Test
    fun aSantaSeesTheirReceiversDetails() {
        val id = activeGroup()

        val details = receiverOf(user("alice"), id)

        assertEquals("bob", details.username.value)
        assertEquals("Bob Builder", details.fullName)
        assertEquals("2 Side St", details.address)
        assertEquals("555-0102", details.phone)
        assertEquals("Leave at the back door", details.deliveryInfo)
    }

    @Test
    fun everyoneSeesOnlyTheirOwnReceiver() {
        val id = activeGroup()

        assertEquals(listOf("bob", "carol", "dave", "alice"), names.map { receiverOf(user(it), id).username.value })
    }

    @Test
    fun detailsAreLive() {
        val id = activeGroup()
        users.updateProfile(u("bob"), Profile(fullName = "Bob Builder", address = "99 New Road"))

        assertEquals("99 New Road", receiverOf(user("alice"), id).address)
    }

    @Test
    fun unsetFieldsAreNull() {
        val id = activeGroup()

        val details = receiverOf(user("dave"), id) // dave's receiver is alice: no phone, no delivery info

        assertEquals("alice", details.username.value)
        assertEquals("Alice Archer", details.fullName)
        assertNull(details.phone)
        assertNull(details.deliveryInfo)
    }

    @Test
    fun theDetailsHaveNoEmailAndNoCredentials() {
        val fields = receiverOf(user("alice"), activeGroup()).toString()

        assertFalse(fields.contains("bob@example.com"))
        assertFalse(fields.contains("hash", ignoreCase = true))
    }

    @Test
    fun theReceiverIsStillReadableAfterArchive() {
        val id = activeGroup()
        archive(id)

        assertEquals("bob", receiverOf(user("alice"), id).username.value)
    }

    @Test
    fun thereIsNoReceiverBeforeActivation() {
        val pending = group(GroupState.PENDING, *names.toTypedArray())

        assertFailsWith<DomainException.InvalidState> { receiverOf(user("alice"), pending.id) }
    }

    @Test
    fun draftsStrangersAndUnknownGroupsAreNotFound() {
        val draft = group(GroupState.DRAFT, *names.toTypedArray())
        val active = activeGroup()
        users.add("outsider")

        assertFailsWith<DomainException.NotFound> { receiverOf(user("alice"), draft.id) }
        assertFailsWith<DomainException.NotFound> { receiverOf(user("outsider"), active) }
        assertFailsWith<DomainException.NotFound> { receiverOf(user("alice"), 9999) }
        assertFailsWith<DomainException.NotFound> { receiverOf(admin, active) } // the owner, not a participant
    }

    // --- Santa reveal ---

    @Test
    fun theSantaIsHiddenWhileActive() {
        val id = activeGroup()

        for (name in names) {
            val error = assertFailsWith<DomainException.InvalidState> { santaOf(user(name), id) }
            for (other in names) assertFalse(error.message.orEmpty().contains(other), "message names $other: ${error.message}")
        }
    }

    @Test
    fun afterArchiveTheSantaIsRevealedWithUsernameAndFullNameOnly() {
        val id = activeGroup()
        archive(id)

        val reveal = santaOf(user("bob"), id) // bob's Santa is alice

        assertEquals("alice", reveal.username.value)
        assertEquals("Alice Archer", reveal.fullName)
        assertEquals("SantaReveal(username=alice, fullName=Alice Archer)", reveal.toString())
    }

    @Test
    fun everyoneLearnsTheirOwnSantaOnly() {
        val id = activeGroup()
        archive(id)

        assertEquals(listOf("dave", "alice", "bob", "carol"), names.map { santaOf(user(it), id).username.value })
    }

    @Test
    fun theRevealIsRefusedBeforeActivationAndToStrangers() {
        val pending = group(GroupState.PENDING, *names.toTypedArray())
        val archived = activeGroup().also { archive(it) }
        users.add("outsider")

        assertFailsWith<DomainException.InvalidState> { santaOf(user("alice"), pending.id) }
        assertFailsWith<DomainException.NotFound> { santaOf(user("outsider"), archived) }
    }
}
