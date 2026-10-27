package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.NewAccountPlanner
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.GetMyGroup
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.ListAssignments
import com.usuallycoding.opensecretsanta.domain.group.ListMyGroups
import com.usuallycoding.opensecretsanta.domain.group.MoveToPending
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.UpdateGroupDetails
import com.usuallycoding.opensecretsanta.domain.user.Profile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LifecycleTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val viewer = GroupViewer(users)
    private val loader = OwnedGroupLoader(groups)
    private val adminA = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }
    private val adminB = users.add("admin-b", admin = true).let { users.userNamed("admin-b") }

    private val toPending = MoveToPending(loader, groups, users, NewAccountPlanner(FakeHasher(), FakeSecretBox(), SequenceTempPasswords()), viewer)
    private val activate = ActivateGroup(loader, groups, AssignmentPlanner(IdentityShuffler()), viewer)
    private val archive = ArchiveGroup(loader, groups, viewer)
    private val listAssignments = ListAssignments(loader, groups)
    private val myGroups = ListMyGroups(groups)
    private val myGroup = GetMyGroup(groups)

    private fun group(state: GroupState, vararg participants: String) =
        groups.seed(adminA.username, state, *participants.map { u(it) }.toTypedArray())

    private fun stateOf(id: Long) = groups.find(id)!!.state

    // --- order and ownership ---

    @Test
    fun everyIllegalMoveIsRejectedAndLeavesTheStateAlone() {
        val steps = mapOf(
            GroupState.PENDING to { id: Long -> toPending(adminA, id) },
            GroupState.ACTIVE to { id: Long -> activate(adminA, id) },
            GroupState.ARCHIVED to { id: Long -> archive(adminA, id) },
        )
        for (from in GroupState.entries) {
            for ((target, move) in steps) {
                if (from.canMoveTo(target)) continue
                val group = group(from, "alice", "bob-b", "carol")
                assertFailsWith<DomainException.InvalidState>("$from -> $target") { move(group.id) }
                assertEquals(from, stateOf(group.id), "$from -> $target")
            }
        }
    }

    @Test
    fun repeatingAMoveIsRejected() {
        val group = group(GroupState.DRAFT, "alice", "bob-b", "carol")
        toPending(adminA, group.id)
        assertFailsWith<DomainException.InvalidState> { toPending(adminA, group.id) }
    }

    @Test
    fun onlyTheOwnerMovesAGroup() {
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")
        assertFailsWith<DomainException.NotFound> { activate(adminB, group.id) }
        assertEquals(GroupState.PENDING, stateOf(group.id))
    }

    // --- Draft to Pending ---

    @Test
    fun pendingCreatesAccountsOnlyForPeopleWithoutOne() {
        users.add("alice", password = "alices-own-password")
        val group = group(GroupState.DRAFT, "alice", "bob-b")

        val change = toPending(adminA, group.id)

        assertEquals(GroupState.PENDING, change.view.group.state)
        assertEquals(listOf("bob-b"), change.createdAccounts.map { it.username.value })
        assertEquals("TempPass0001", change.createdAccounts.single().temporaryPassword)
        assertTrue(users.find(u("bob-b"))!!.mustChangePassword)
        assertEquals("hash:alices-own-password", users.credentials(u("alice"))!!.passwordHash)
    }

    @Test
    fun accountsAreNotCreatedBeforePending() {
        group(GroupState.DRAFT, "alice")
        assertNull(users.find(u("alice")))
    }

    @Test
    fun anEmptyDraftBecomesPendingWithoutCreatingAnything() {
        val group = group(GroupState.DRAFT)
        val usersBefore = users.count()

        val change = toPending(adminA, group.id)

        assertEquals(GroupState.PENDING, change.view.group.state)
        assertEquals(usersBefore, users.count())
        assertEquals(emptyList(), change.createdAccounts)
    }

    @Test
    fun aFailureWhileMovingToPendingLeavesStateAndAccountsAsTheyWere() {
        val group = group(GroupState.DRAFT, "alice")
        groups.failNextWith = IllegalStateException("disk full")

        assertFailsWith<IllegalStateException> { toPending(adminA, group.id) }

        assertEquals(GroupState.DRAFT, stateOf(group.id))
        assertNull(users.find(u("alice")))
    }

    // --- Pending to Active ---

    @Test
    fun fewerThanFourParticipantsCannotBeActivated() {
        for (names in listOf(emptyList(), listOf("alice"), listOf("alice", "bob-b"), listOf("alice", "bob-b", "carol"))) {
            val group = group(GroupState.PENDING, *names.toTypedArray())
            val error = assertFailsWith<DomainException.ValidationFailed> { activate(adminA, group.id) }
            assertEquals("participants", error.field)
            assertEquals(GroupState.PENDING, stateOf(group.id))
            assertEquals(emptyList(), groups.assignments(group.id))
        }
    }

    @Test
    fun exactlyFourParticipantsCanBeActivated() {
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")

        assertEquals(GroupState.ACTIVE, activate(adminA, group.id).group.state)
        assertEquals(4, groups.assignments(group.id).size)
    }

    @Test
    fun incompleteProfilesDoNotPreventActivationButAreFlagged() {
        users.add("alice").also { users.updateProfile(it, Profile(fullName = "Alice", address = "1 Main St")) }
        users.add("bob-b")
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")

        val view = activate(adminA, group.id)

        assertEquals(GroupState.ACTIVE, view.group.state)
        assertEquals(listOf(true, false, false, false), view.participants.map { it.profileComplete })
    }

    @Test
    fun activationDrawsOneAssignmentPerParticipant() {
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")

        activate(adminA, group.id)

        val pairs = groups.assignments(group.id)
        assertEquals(4, pairs.size)
        assertEquals(setOf("alice", "bob-b", "carol", "dave"), pairs.map { it.santa.value }.toSet())
        assertEquals(setOf("alice", "bob-b", "carol", "dave"), pairs.map { it.receiver.value }.toSet())
        assertTrue(pairs.none { it.santa == it.receiver })
    }

    @Test
    fun noAssignmentsExistBeforeActivation() {
        for (state in listOf(GroupState.DRAFT, GroupState.PENDING)) {
            val group = group(state, "alice", "bob-b", "carol")
            assertEquals(emptyList(), groups.assignments(group.id), state.toString())
            assertFailsWith<DomainException.InvalidState>(state.toString()) { listAssignments(adminA, group.id) }
        }
    }

    @Test
    fun aFailureWhileActivatingLeavesNoAssignmentsAndStaysPending() {
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")
        groups.failNextWith = IllegalStateException("disk full")

        assertFailsWith<IllegalStateException> { activate(adminA, group.id) }

        assertEquals(GroupState.PENDING, stateOf(group.id))
        assertEquals(emptyList(), groups.assignments(group.id))
    }

    // --- archive and fixed assignments ---

    @Test
    fun archivingKeepsParticipantsAndAssignments() {
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")
        activate(adminA, group.id)
        val before = groups.assignments(group.id)

        val view = archive(adminA, group.id)

        assertEquals(GroupState.ARCHIVED, view.group.state)
        assertEquals(before, groups.assignments(group.id))
        assertEquals(before, listAssignments(adminA, group.id))
        assertEquals(listOf("alice", "bob-b", "carol", "dave"), view.group.participants.map { it.value })
    }

    @Test
    fun editingDetailsNeverRedrawsTheAssignments() {
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")
        activate(adminA, group.id)
        val before = groups.assignments(group.id)

        UpdateGroupDetails(loader, groups, viewer)(adminA, group.id, GroupDetails("New name", location = "Elsewhere"))

        assertEquals(before, groups.assignments(group.id))
    }

    // --- who sees assignments ---

    @Test
    fun ownerListsEveryPair() {
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")
        activate(adminA, group.id)

        assertEquals(4, listAssignments(adminA, group.id).size)
    }

    @Test
    fun otherAdminsGetNotFoundAndNormalUsersGetForbidden() {
        val group = group(GroupState.PENDING, "alice", "bob-b", "carol", "dave")
        activate(adminA, group.id)
        users.add("alice")

        assertFailsWith<DomainException.NotFound> { listAssignments(adminB, group.id) }
        assertFailsWith<DomainException.Forbidden> { listAssignments(users.userNamed("alice"), group.id) }
    }

    // --- participant view ---

    @Test
    fun participantsSeeNonDraftGroupsTheyBelongTo() {
        users.add("alice")
        val draft = group(GroupState.DRAFT, "alice")
        val pending = group(GroupState.PENDING, "alice", "bob-b")
        val active = group(GroupState.ACTIVE, "alice", "bob-b", "carol")
        group(GroupState.PENDING, "someone-else")
        val alice = users.userNamed("alice")

        assertEquals(listOf(pending.id, active.id), myGroups(alice).map { it.id })
        assertEquals(pending.id, myGroup(alice, pending.id).id)
        assertFailsWith<DomainException.NotFound> { myGroup(alice, draft.id) }
    }

    @Test
    fun aGroupYouAreNotInOrThatDoesNotExistIsNotFound() {
        users.add("alice")
        val other = group(GroupState.ACTIVE, "bob-b", "carol", "dave")
        val alice = users.userNamed("alice")

        val notMember = assertFailsWith<DomainException.NotFound> { myGroup(alice, other.id) }
        val missing = assertFailsWith<DomainException.NotFound> { myGroup(alice, 9999) }
        assertEquals(notMember.message, missing.message)
    }
}
