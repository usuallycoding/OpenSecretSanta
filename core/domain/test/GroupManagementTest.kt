package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.CreateGroup
import com.usuallycoding.opensecretsanta.domain.group.DeleteDraftGroup
import com.usuallycoding.opensecretsanta.domain.group.GetOwnedGroup
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.ListOwnedGroups
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.UpdateGroupDetails
import com.usuallycoding.opensecretsanta.domain.user.Profile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupManagementTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val viewer = GroupViewer(users)
    private val loader = OwnedGroupLoader(groups)

    private val adminA = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }
    private val adminB = users.add("admin-b", admin = true).let { users.userNamed("admin-b") }
    private val normal = users.add("carol").let { users.userNamed("carol") }

    private val create = CreateGroup(groups, viewer)
    private val update = UpdateGroupDetails(loader, groups, viewer)
    private val delete = DeleteDraftGroup(loader, groups)
    private val list = ListOwnedGroups(groups, viewer)
    private val get = GetOwnedGroup(loader, viewer)

    private fun draft(owner: com.usuallycoding.opensecretsanta.domain.user.User = adminA) =
        create(owner, GroupDetails("Party"), emptyList()).group.id

    // --- create ---

    @Test
    fun minimalGroupStartsInDraftOwnedByItsCreator() {
        val view = create(adminA, GroupDetails("Party"), emptyList())

        assertEquals(GroupState.DRAFT, view.group.state)
        assertEquals(adminA.username, view.group.owner)
        assertEquals(GroupDetails("Party"), view.group.details)
        assertEquals(emptyList(), view.group.participants)
    }

    @Test
    fun participantsGivenAtCreationAreNormalizedAndDeduplicated() {
        val view = create(adminA, GroupDetails("Party"), listOf("Alice", "bob-b", "alice"))

        assertEquals(listOf("alice", "bob-b"), view.group.participants.map { it.value })
    }

    @Test
    fun invalidParticipantAtCreationCreatesNothing() {
        val error = assertFailsWith<DomainException.ValidationFailed> { create(adminA, GroupDetails("Party"), listOf("alice", "no")) }

        assertEquals("participants", error.field)
        assertEquals(emptyList(), groups.findOwnedBy(adminA.username))
    }

    @Test
    fun nonAdminsCannotCreateGroups() {
        assertFailsWith<DomainException.Forbidden> { create(normal, GroupDetails("Party"), emptyList()) }
    }

    // --- owner-only ---

    @Test
    fun otherAdminsAndUnknownIdsLookNonexistent() {
        val id = draft()

        val other = assertFailsWith<DomainException.NotFound> { get(adminB, id) }
        val unknown = assertFailsWith<DomainException.NotFound> { get(adminA, 9999) }
        assertEquals(other.message, unknown.message)
        assertFailsWith<DomainException.NotFound> { update(adminB, id, GroupDetails("Hacked")) }
        assertFailsWith<DomainException.NotFound> { delete(adminB, id) }
        assertEquals("Party", groups.find(id)!!.details.name)
    }

    @Test
    fun nonAdminsAreForbiddenFromManagement() {
        val id = draft()
        assertFailsWith<DomainException.Forbidden> { get(normal, id) }
        assertFailsWith<DomainException.Forbidden> { list(normal) }
    }

    // --- list and view ---

    @Test
    fun listContainsOnlyOwnGroupsInAnyState() {
        draft(adminA)
        groups.seed(adminA.username, GroupState.ACTIVE, name = "Active one")
        draft(adminB)

        assertEquals(listOf("Party", "Active one"), list(adminA).map { it.group.details.name })
        assertEquals(1, list(adminB).size)
    }

    @Test
    fun viewFlagsAccountsAndProfileCompleteness() {
        users.add("alice").also { users.updateProfile(it, Profile(fullName = "Alice B", address = "1 Main St")) }
        users.add("bob").also { users.updateProfile(it, Profile(fullName = "Bob", phone = "123")) }
        val group = groups.seed(adminA.username, GroupState.PENDING, u("alice"), u("bob"), u("carol-new"))

        val participants = get(adminA, group.id).participants.associateBy { it.username.value }

        assertTrue(participants.getValue("alice").hasAccount && participants.getValue("alice").profileComplete)
        assertTrue(participants.getValue("bob").hasAccount)
        assertFalse(participants.getValue("bob").profileComplete)
        assertFalse(participants.getValue("carol-new").hasAccount)
        assertFalse(participants.getValue("carol-new").profileComplete)
    }

    @Test
    fun profileNeedsBothNameAndAddressToBeComplete() {
        assertFalse(Profile(fullName = "A").isComplete)
        assertFalse(Profile(address = "x").isComplete)
        assertFalse(Profile(phone = "1", deliveryInfo = "d").isComplete)
        assertTrue(Profile(fullName = "A", address = "x").isComplete)
    }

    // --- edit ---

    @Test
    fun detailsCanBeEditedInDraftPendingAndActive() {
        for (state in listOf(GroupState.DRAFT, GroupState.PENDING, GroupState.ACTIVE)) {
            val group = groups.seed(adminA.username, state)
            val view = update(adminA, group.id, GroupDetails("Renamed $state", location = "HQ"))
            assertEquals("HQ", view.group.details.location, state.toString())
            assertEquals(state, view.group.state)
        }
    }

    @Test
    fun anArchivedGroupIsReadOnly() {
        val group = groups.seed(adminA.username, GroupState.ARCHIVED)

        assertFailsWith<DomainException.InvalidState> { update(adminA, group.id, GroupDetails("Nope")) }

        assertEquals("Group", groups.find(group.id)!!.details.name)
    }

    // --- delete ---

    @Test
    fun aDraftCanBeDeleted() {
        val id = draft()
        delete(adminA, id)
        assertNull(groups.find(id))
    }

    @Test
    fun nonDraftGroupsCannotBeDeleted() {
        for (state in listOf(GroupState.PENDING, GroupState.ACTIVE, GroupState.ARCHIVED)) {
            val group = groups.seed(adminA.username, state)
            assertFailsWith<DomainException.InvalidState>(state.toString()) { delete(adminA, group.id) }
            assertEquals(group, groups.find(group.id))
        }
    }
}
