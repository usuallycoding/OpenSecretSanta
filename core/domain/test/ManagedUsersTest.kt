package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.AccessPolicy
import com.usuallycoding.opensecretsanta.domain.admin.GetManagedUser
import com.usuallycoding.opensecretsanta.domain.admin.ListManagedUsers
import com.usuallycoding.opensecretsanta.domain.admin.ResetPassword
import com.usuallycoding.opensecretsanta.domain.auth.LoginAttempts
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.TempPassword
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManagedUsersTest {
    private val users = InMemoryUsers()
    private val secretBox = FakeSecretBox()
    private val attempts = InMemoryAttempts()
    private val adminA = users.add("admin-a", admin = true)
    private val adminB = users.add("admin-b", admin = true)
    private val alice = users.add("alice", password = "TempPass9001", mustChange = true, tempPlain = "TempPass9001")
    private val bob = users.add("bob", password = "TempPass9002", mustChange = true, tempPlain = "TempPass9002")
    private val carol = users.add("carol")
    private val outsider = users.add("outsider")
    private val memberships = FakeMemberships(
        mapOf(adminA to setOf(alice, carol), adminB to setOf(bob, alice)),
    )
    private val access = AccessPolicy(memberships)

    private fun actor(name: String) = users.find(u(name))!!

    private val list = ListManagedUsers(users, access, secretBox)
    private val get = GetManagedUser(users, access, secretBox)
    private val reset = ResetPassword(users, access, FakeHasher(), secretBox, SequenceTempPasswords(), attempts)

    @Test
    fun nonAdminsAreForbiddenEverywhere() {
        val normal = actor("carol")
        assertFailsWith<DomainException.Forbidden> { list(normal) }
        assertFailsWith<DomainException.Forbidden> { get(normal, "carol") }
        assertFailsWith<DomainException.Forbidden> { reset(normal, "carol") }
    }

    @Test
    fun listIsScopedToOwnGroupsAndSelf() {
        assertEquals(listOf("admin-a", "alice", "carol"), list(actor("admin-a")).map { it.user.username.value })
    }

    @Test
    fun temporaryPasswordIsVisibleWhileItIsTemporary() {
        assertEquals("TempPass9001", get(actor("admin-a"), "alice").temporaryPassword)
    }

    @Test
    fun temporaryPasswordIsHiddenOnceTheUserSetTheirOwn() {
        users.replaceCredentials(alice, "hash:mine-now-1", null, false)
        assertNull(get(actor("admin-a"), "alice").temporaryPassword)
    }

    @Test
    fun usersWithoutAnEncryptedTempPasswordShowNone() {
        users.replaceCredentials(carol, "hash:x", null, mustChangePassword = true)
        assertNull(get(actor("admin-a"), "carol").temporaryPassword)
    }

    @Test
    fun outOfScopeAndUnknownUsersLookNonexistent() {
        val inScope = assertFailsWith<DomainException.NotFound> { get(actor("admin-a"), "bob") }
        val unknown = assertFailsWith<DomainException.NotFound> { get(actor("admin-a"), "nobody-at-all") }
        assertFailsWith<DomainException.NotFound> { get(actor("admin-a"), "x") }
        assertEquals(inScope.message, unknown.message)
        assertFailsWith<DomainException.NotFound> { reset(actor("admin-a"), "outsider") }
    }

    @Test
    fun sharedUserIsVisibleToBothOwners() {
        assertEquals("alice", get(actor("admin-a"), "alice").user.username.value)
        assertEquals("alice", get(actor("admin-b"), "ALICE").user.username.value)
    }

    @Test
    fun adminCanViewThemselves() {
        assertEquals("admin-a", get(actor("admin-a"), "admin-a").user.username.value)
    }

    @Test
    fun resetIssuesNewTemporaryPasswordWithAllSideEffects() {
        attempts.save(alice, LoginAttempts(5, lockedUntilMillis = Long.MAX_VALUE))
        val tokenVersionBefore = users.find(alice)!!.tokenVersion

        val result = reset(actor("admin-a"), "alice")

        val after = users.find(alice)!!
        assertEquals("TempPass0001", result.temporaryPassword)
        assertEquals("hash:TempPass0001", users.credentials(alice)!!.passwordHash)
        assertTrue(after.mustChangePassword)
        assertEquals(tokenVersionBefore + 1, after.tokenVersion)
        assertNull(attempts.get(alice))
        assertEquals("TempPass0001", get(actor("admin-b"), "alice").temporaryPassword)
    }

    @Test
    fun resetOfUserOutsideScopeChangesNothing() {
        assertFailsWith<DomainException.NotFound> { reset(actor("admin-a"), "bob") }
        assertEquals("hash:TempPass9002", users.credentials(bob)!!.passwordHash)
        assertEquals(1, users.find(bob)!!.tokenVersion)
    }

    @Test
    fun tempPasswordAlphabetExcludesLookAlikes() {
        for (c in listOf('0', 'O', '1', 'l', 'I')) assertFalse(c in TempPassword.ALPHABET)
        assertEquals(12, TempPassword.LENGTH)
    }
}
