package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.NewAccountPlanner
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.SetParticipants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetParticipantsTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val adminA = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }
    private val adminB = users.add("admin-b", admin = true).let { users.userNamed("admin-b") }
    private val set = SetParticipants(
        OwnedGroupLoader(groups),
        groups,
        users,
        NewAccountPlanner(FakeHasher(), FakeSecretBox(), SequenceTempPasswords()),
        GroupViewer(users),
    )

    private fun names(group: Long) = groups.find(group)!!.participants.map { it.value }

    // --- Draft ---

    @Test
    fun draftStoresNamesWithoutCreatingAccounts() {
        users.add("alice")
        val group = groups.seed(adminA.username, GroupState.DRAFT)

        val change = set(adminA, group.id, listOf("alice", "newcomer"))

        assertEquals(listOf("alice", "newcomer"), names(group.id))
        assertNull(users.find(u("newcomer")))
        assertEquals(emptyList(), change.createdAccounts)
        assertEquals(listOf(true, false), change.view.participants.map { it.hasAccount })
    }

    @Test
    fun theNewListReplacesTheOldOneAndKeepsItsOrder() {
        val group = groups.seed(adminA.username, GroupState.DRAFT, u("old-one"), u("old-two"))

        set(adminA, group.id, listOf("zed", "amy", "kim"))

        assertEquals(listOf("zed", "amy", "kim"), names(group.id))
    }

    @Test
    fun caseDuplicatesAreStoredOnce() {
        val group = groups.seed(adminA.username, GroupState.DRAFT)

        set(adminA, group.id, listOf("Alice", "alice", "ALICE"))

        assertEquals(listOf("alice"), names(group.id))
    }

    @Test
    fun anInvalidNameFailsNamingTheFieldAndLeavesTheListUntouched() {
        val group = groups.seed(adminA.username, GroupState.DRAFT, u("alice"))

        val error = assertFailsWith<DomainException.ValidationFailed> { set(adminA, group.id, listOf("bob-b", "no", "carol")) }

        assertEquals("participants", error.field)
        assertEquals(listOf("alice"), names(group.id))
    }

    // --- Pending ---

    @Test
    fun addingANewPersonInPendingCreatesTheirAccountAndReturnsTheCredentials() {
        users.add("alice")
        val group = groups.seed(adminA.username, GroupState.PENDING, u("alice"))

        val change = set(adminA, group.id, listOf("alice", "dave"))

        val dave = assertNotNull(users.find(u("dave")))
        assertTrue(dave.mustChangePassword)
        assertEquals(listOf("dave"), change.createdAccounts.map { it.username.value })
        assertEquals("TempPass0001", change.createdAccounts.single().temporaryPassword)
        assertEquals("hash:TempPass0001", users.credentials(u("dave"))!!.passwordHash)
        assertEquals(listOf("alice", "dave"), names(group.id))
    }

    @Test
    fun removingAPersonInPendingKeepsTheirAccount() {
        users.add("alice")
        users.add("bob")
        val group = groups.seed(adminA.username, GroupState.PENDING, u("alice"), u("bob"))

        val change = set(adminA, group.id, listOf("alice"))

        assertEquals(listOf("alice"), names(group.id))
        assertNotNull(users.find(u("bob")))
        assertEquals(emptyList(), change.createdAccounts)
    }

    @Test
    fun addingAnExistingUserInPendingLeavesTheirAccountUntouched() {
        users.add("erin", password = "erins-own-password")
        val group = groups.seed(adminA.username, GroupState.PENDING)

        val change = set(adminA, group.id, listOf("erin"))

        assertEquals(emptyList(), change.createdAccounts)
        assertEquals("hash:erins-own-password", users.credentials(u("erin"))!!.passwordHash)
        assertEquals(1, users.find(u("erin"))!!.tokenVersion)
    }

    @Test
    fun anInvalidNameInPendingCreatesNoAccountAtAll() {
        val group = groups.seed(adminA.username, GroupState.PENDING)

        assertFailsWith<DomainException.ValidationFailed> { set(adminA, group.id, listOf("valid-one", "x")) }

        assertNull(users.find(u("valid-one")))
        assertEquals(0, users.count() - 2) // only the two admins exist
    }

    // --- frozen ---

    @Test
    fun theRosterIsFrozenFromActivation() {
        for (state in listOf(GroupState.ACTIVE, GroupState.ARCHIVED)) {
            val group = groups.seed(adminA.username, state, u("alice"), u("bob"), u("carol"))

            assertFailsWith<DomainException.InvalidState>(state.toString()) { set(adminA, group.id, listOf("alice")) }

            assertEquals(listOf("alice", "bob", "carol"), names(group.id))
        }
    }

    // --- access ---

    @Test
    fun onlyTheOwnerCanChangeParticipants() {
        val group = groups.seed(adminA.username, GroupState.DRAFT, u("alice"))

        assertFailsWith<DomainException.NotFound> { set(adminB, group.id, listOf("mallory")) }

        assertEquals(listOf("alice"), names(group.id))
    }

    @Test
    fun aFailedStoreLeavesNoAccountsBehind() {
        val group = groups.seed(adminA.username, GroupState.PENDING)
        groups.failNextWith = IllegalStateException("disk full")

        assertFailsWith<IllegalStateException> { set(adminA, group.id, listOf("dave")) }

        assertNull(users.find(u("dave")))
        assertEquals(emptyList(), names(group.id))
    }
    // --- who is told ---

    @Test
    fun inDraftNobodyIsReportedAsAdded() {
        val group = groups.seed(adminA.username, GroupState.DRAFT, u("alice"))

        assertEquals(emptyList(), set(adminA, group.id, listOf("alice", "bob-b")).added)
    }

    @Test
    fun inPendingOnlyThePeopleNotThereBeforeAreReportedAsAdded() {
        users.add("alice")
        val group = groups.seed(adminA.username, GroupState.PENDING, u("alice"), u("bob-b"))

        val change = set(adminA, group.id, listOf("alice", "carol", "dave"))

        assertEquals(listOf(u("carol"), u("dave")), change.added)
    }

    @Test
    fun aPendingListWithNobodyNewReportsNoOneAdded() {
        val group = groups.seed(adminA.username, GroupState.PENDING, u("alice"), u("bob-b"))

        assertEquals(emptyList(), set(adminA, group.id, listOf("bob-b", "alice")).added)
    }

}
