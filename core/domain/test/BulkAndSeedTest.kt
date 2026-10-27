package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.CreateMissingUsers
import com.usuallycoding.opensecretsanta.domain.admin.SeedDefaultAdmin
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateMissingUsersTest {
    private val users = InMemoryUsers()
    private val create = CreateMissingUsers(users, FakeHasher(), FakeSecretBox(), SequenceTempPasswords())

    @Test
    fun createsOnlyMissingUsersAndLeavesExistingUntouched() {
        users.add("alice", password = "keep-me-as-is")

        val created = create(listOf("alice", "bob"))

        assertEquals(listOf("bob"), created.map { it.username.value })
        assertEquals("hash:keep-me-as-is", users.credentials(u("alice"))!!.passwordHash)
        val bob = users.find(u("bob"))!!
        assertTrue(bob.mustChangePassword)
        assertEquals(false, bob.isAdmin)
        assertEquals("TempPass0001", FakeSecretBox().decrypt(users.credentials(u("bob"))!!.encryptedTempPassword!!))
        assertEquals("hash:TempPass0001", users.credentials(u("bob"))!!.passwordHash)
    }

    @Test
    fun newUsersHaveOnlyAUsername() {
        create(listOf("bob"))
        assertEquals(com.usuallycoding.opensecretsanta.domain.user.Profile.EMPTY, users.find(u("bob"))!!.profile)
    }

    @Test
    fun invalidEntryCreatesNothing() {
        assertFailsWith<DomainException.ValidationFailed> { create(listOf("bob", "carol", "no")) }
        assertEquals(0, users.count())
    }

    @Test
    fun duplicatesDifferingOnlyInCaseAreCreatedOnce() {
        val created = create(listOf("Dave", "dave", "DAVE"))
        assertEquals(1, created.size)
        assertEquals(1, users.count())
    }

    @Test
    fun emptyListIsANoOp() {
        assertEquals(emptyList(), create(emptyList()))
    }
}

class SeedDefaultAdminTest {
    private val users = InMemoryUsers()
    private val seed = SeedDefaultAdmin(users, FakeHasher())

    @Test
    fun seedsAdminOnEmptyStore() {
        val admin = assertNotNull(seed("Boss", "initial-pass"))

        assertEquals("boss", admin.username.value)
        assertTrue(admin.isAdmin)
        assertTrue(admin.mustChangePassword)
        assertEquals("hash:initial-pass", users.credentials(admin.username)!!.passwordHash)
    }

    @Test
    fun seededPasswordIsNotRetrievable() {
        seed("boss", "initial-pass")
        assertNull(users.credentials(u("boss"))!!.encryptedTempPassword)
    }

    @Test
    fun doesNothingWhenUsersExist() {
        users.add("alice")
        assertNull(seed("boss", "other-password"))
        assertEquals(1, users.count())
        assertNull(users.find(u("boss")))
    }

    @Test
    fun doesNothingWhenUsersExistEvenWithoutPassword() {
        users.add("alice")
        assertNull(seed("boss", null))
    }

    @Test
    fun missingOrBlankPasswordOnEmptyStoreFailsNamingTheVariable() {
        for (password in listOf(null, "", "   ")) {
            val error = assertFailsWith<DomainException.ValidationFailed> { seed("boss", password) }
            assertEquals("ADMIN_PASSWORD", error.field)
        }
        assertEquals(0, users.count())
    }

    @Test
    fun weakPasswordIsRejectedNamingTheVariable() {
        val error = assertFailsWith<DomainException.ValidationFailed> { seed("boss", "short") }
        assertEquals("ADMIN_PASSWORD", error.field)
    }

    @Test
    fun invalidUsernameFailsNamingTheVariable() {
        val error = assertFailsWith<DomainException.ValidationFailed> { seed("a b", "initial-pass") }
        assertEquals("ADMIN_USERNAME", error.field)
    }
}
