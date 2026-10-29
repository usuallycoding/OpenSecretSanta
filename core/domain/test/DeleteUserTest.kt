package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.AccessPolicy
import com.usuallycoding.opensecretsanta.domain.admin.DeleteUser
import com.usuallycoding.opensecretsanta.domain.admin.GetManagedUser
import com.usuallycoding.opensecretsanta.domain.admin.ListManagedUsers
import com.usuallycoding.opensecretsanta.domain.admin.NewAccountPlanner
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteUserTest {
    private val users = InMemoryUsers()
    private val adminA = users.add("admin-a", admin = true)
    private val adminB = users.add("admin-b", admin = true)

    // alice is in a group of A; erin was made by A and is in no group; fred was made by B and is in no group; gina was made by A and then joined a group
    private val alice = users.add("alice")
    private val erin = users.add("erin")
    private val fred = users.add("fred")
    private val gina = users.add("gina")
    private val memberships = FakeMemberships(
        owned = mapOf(adminA to setOf(alice)),
        unassigned = mapOf(adminA to setOf(erin), adminB to setOf(fred)),
    )
    private val access = AccessPolicy(memberships)
    private val secretBox = FakeSecretBox()
    private val list = ListManagedUsers(users, access, secretBox)
    private val get = GetManagedUser(users, access, secretBox)
    private val delete = DeleteUser(users, access)

    private fun actor(name: String) = users.find(u(name))!!

    private fun createdBy(creator: String, vararg names: String) {
        for (name in names) {
            users.setCreator(u(name), u(creator))
        }
    }

    private fun setup() {
        createdBy("admin-a", "erin", "gina")
        createdBy("admin-b", "fred")
    }

    @Test
    fun theListAndTheFlagsIncludeTheCallersOwnUnassignedUsers() {
        setup()

        val listed = list(actor("admin-a")).associate { it.user.username.value to it.canDelete }

        assertEquals(mapOf("admin-a" to false, "alice" to false, "erin" to true), listed)
    }

    @Test
    fun anotherAdminsUnassignedUserIsNotVisibleOrDeletable() {
        setup()

        assertFailsWith<DomainException.NotFound> { get(actor("admin-a"), "fred") }
        assertFailsWith<DomainException.NotFound> { delete(actor("admin-a"), "fred") }
        assertTrue(users.find(u("fred")) != null)
    }

    @Test
    fun theDetailSaysWhetherTheUserCanBeDeleted() {
        setup()

        assertTrue(get(actor("admin-a"), "erin").canDelete)
        assertFalse(get(actor("admin-a"), "alice").canDelete)
        assertFalse(get(actor("admin-a"), "admin-a").canDelete)
    }

    @Test
    fun anUnassignedUserTheCallerCreatedIsDeleted() {
        setup()

        delete(actor("admin-a"), "erin")

        assertNull(users.find(u("erin")))
    }

    @Test
    fun aUserInAGroupIsRefusedAndKept() {
        setup()

        assertFailsWith<DomainException.InvalidState> { delete(actor("admin-a"), "alice") }
        assertTrue(users.find(u("alice")) != null)
    }

    @Test
    fun anAdministratorAndTheCallerAreRefused() {
        setup()

        assertFailsWith<DomainException.InvalidState> { delete(actor("admin-a"), "admin-a") }
        assertTrue(users.find(u("admin-a")) != null)
        assertFailsWith<DomainException.NotFound> { delete(actor("admin-a"), "admin-b") }
        assertTrue(users.find(u("admin-b")) != null)
    }

    @Test
    fun aUserWhoJoinedAGroupMeanwhileIsRefusedByTheRepositoryToo() {
        setup()
        // the policy still says erin is deletable (it was read just before), but a group named her since
        val stale = DeleteUser(users, AccessPolicy(FakeMemberships(unassigned = mapOf(adminA to setOf(gina)))))
        users.refuseDeleting(u("gina"))

        assertFailsWith<DomainException.InvalidState> { stale(actor("admin-a"), "gina") }
        assertTrue(users.find(u("gina")) != null)
    }

    @Test
    fun aNormalUserMayNotDelete() {
        setup()

        assertFailsWith<DomainException.Forbidden> { delete(actor("alice"), "erin") }
    }

    @Test
    fun anUnknownOrInvalidUsernameIsNotFound() {
        setup()

        assertFailsWith<DomainException.NotFound> { delete(actor("admin-a"), "nobody") }
        assertFailsWith<DomainException.NotFound> { delete(actor("admin-a"), "Not A Name!") }
    }

    @Test
    fun theNameCanBeUsedAgainAfterwards() {
        setup()
        delete(actor("admin-a"), "erin")

        val planned = NewAccountPlanner(FakeHasher(), FakeSecretBox(), SequenceTempPasswords()).plan(listOf(u("erin")), existing = users.existing(listOf(u("erin"))), createdBy = adminB)

        assertEquals(listOf("erin"), planned.map { it.username.value })
        assertEquals(adminB, planned.single().newUser.createdBy)
    }

    @Test
    fun accountsPlannedForAGroupRecordTheAdministratorThatMadeThem() {
        val planned = NewAccountPlanner(FakeHasher(), FakeSecretBox(), SequenceTempPasswords()).plan(listOf(u("newcomer")), existing = emptySet(), createdBy = adminA)

        assertEquals(adminA, planned.single().newUser.createdBy)
        assertNull(NewAccountPlanner(FakeHasher(), FakeSecretBox(), SequenceTempPasswords()).plan(listOf(u("xyz")), existing = emptySet()).single().newUser.createdBy)
    }
}
