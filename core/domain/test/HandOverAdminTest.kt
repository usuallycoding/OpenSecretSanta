package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.AccessPolicy
import com.usuallycoding.opensecretsanta.domain.admin.HandOverAdmin
import com.usuallycoding.opensecretsanta.domain.auth.AttemptLimiter
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HandOverAdminTest {
    private val users = InMemoryUsers()
    private val attempts = InMemoryAttempts()
    private val clock = FakeClock()
    private val boss = users.add("boss", password = "boss-password", admin = true)
    private val alice = users.add("alice")
    private val bobby = users.add("bobby", mustChange = true, tempPlain = "Temp-1234")
    private val carol = users.add("carol")
    private val stranger = users.add("stranger")
    private val rival = users.add("rival", admin = true)
    private val access = AccessPolicy(FakeMemberships(mapOf(boss to setOf(alice, bobby, carol, rival))))
    private val handOver = HandOverAdmin(users, access, FakeHasher(), AttemptLimiter(attempts, clock))

    private fun actor(name: String = "boss") = users.find(u(name))!!
    private fun admin(name: String) = users.find(u(name))!!.isAdmin

    @Test
    fun theRoleMovesFromTheCallerToThePerson() {
        handOver(actor(), "alice", "boss-password")

        assertTrue(admin("alice"))
        assertFalse(admin("boss"))
    }

    @Test
    fun accountsTheCallerCreatedBelongToTheNewAdministrator() {
        users.setCreator(carol, boss)
        users.setCreator(alice, boss)

        handOver(actor(), "alice", "boss-password")

        assertEquals(alice, users.createdBy(carol))
        assertNull(users.createdBy(alice))
        assertEquals(alice, users.createdBy(boss))
    }

    @Test
    fun theCallerIsRecordedAsCreatedByTheNewAdministratorSoTheyAreManaged() {
        handOver(actor(), "alice", "boss-password")
        assertEquals(alice, users.createdBy(boss))
    }

    @Test
    fun aNonAdministratorCannotHandOver() {
        assertFailsWith<DomainException.Forbidden> { handOver(actor("alice"), "carol", "password1") }
        assertFalse(admin("carol"))
    }

    @Test
    fun aPersonTheCallerDoesNotManageIsNotFound() {
        assertFailsWith<DomainException.NotFound> { handOver(actor(), "stranger", "boss-password") }
        assertFailsWith<DomainException.NotFound> { handOver(actor(), "nobody-here", "boss-password") }
        assertFailsWith<DomainException.NotFound> { handOver(actor(), "x", "boss-password") }
        assertTrue(admin("boss"))
    }

    @Test
    fun oneselfAndAnExistingAdministratorAreRefused() {
        assertFailsWith<DomainException.InvalidState> { handOver(actor(), "boss", "boss-password") }
        assertFailsWith<DomainException.InvalidState> { handOver(actor(), "rival", "boss-password") }
        assertTrue(admin("boss"))
    }

    @Test
    fun aPersonOnATemporaryPasswordCannotTakeOver() {
        assertFailsWith<DomainException.InvalidState> { handOver(actor(), "bobby", "boss-password") }
        assertTrue(admin("boss"))
        assertFalse(admin("bobby"))
    }

    @Test
    fun afterTheyChangedItTheyCanTakeOver() {
        users.replaceCredentials(bobby, "hash:mine-now-1", null, false)
        handOver(actor(), "bobby", "boss-password")
        assertTrue(admin("bobby"))
    }

    @Test
    fun aWrongOrEmptyPasswordIsRefusedNamingThePasswordAndChangesNothing() {
        for (wrong in listOf("nope", "")) {
            val error = assertFailsWith<DomainException.ValidationFailed> { handOver(actor(), "alice", wrong) }
            assertEquals("password", error.field)
        }
        assertTrue(admin("boss"))
        assertFalse(admin("alice"))
    }

    @Test
    fun failedAttemptsLockThePersonOutAsSigningInDoes() {
        repeat(AttemptLimiter.MAX_FAILURES) {
            assertFailsWith<DomainException.ValidationFailed> { handOver(actor(), "alice", "nope") }
        }
        val locked = assertFailsWith<DomainException.TooManyAttempts> { handOver(actor(), "alice", "boss-password") }
        assertTrue(locked.retryAfterSeconds > 0)
        assertFalse(admin("alice"))

        clock.now += AttemptLimiter.LOCK_MILLIS + 1
        handOver(actor(), "alice", "boss-password")
        assertTrue(admin("alice"))
    }

    @Test
    fun aRightPasswordClearsTheFailedAttempts() {
        repeat(2) { assertFailsWith<DomainException.ValidationFailed> { handOver(actor(), "alice", "nope") } }
        handOver(actor(), "alice", "boss-password")
        assertNull(attempts.get(u("boss")))
    }

    @Test
    fun aRepositoryThatRefusesMakesTheHandoverInvalidState() {
        // the person's password was reset, or the role moved, between the check and the move
        users.refuseHandOver = true
        assertFailsWith<DomainException.InvalidState> { handOver(actor(), "alice", "boss-password") }
        assertTrue(admin("boss"))
        assertFalse(admin("alice"))
    }
}
