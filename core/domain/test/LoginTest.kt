package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.auth.Login
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginTest {
    private val users = InMemoryUsers().also { it.add("alice", password = "correct-horse", mustChange = true) }
    private val hasher = FakeHasher()
    private val attempts = InMemoryAttempts()
    private val clock = FakeClock()
    private val login = Login(users, hasher, FakeTokens(), attempts, clock)

    private fun failLogin(username: String = "alice", password: String = "wrong") =
        assertFailsWith<DomainException.InvalidCredentials> { login(username, password) }

    @Test
    fun successReturnsTokenAndMustChangeState() {
        val result = login("alice", "correct-horse")
        assertEquals("token:alice:1", result.token)
        assertTrue(result.user.mustChangePassword)
    }

    @Test
    fun usernameIsMatchedIgnoringCase() {
        assertEquals("alice", login("ALICE", "correct-horse").user.username.value)
    }

    @Test
    fun wrongPasswordAndUnknownUserGiveIdenticalErrors() {
        val wrong = failLogin("alice")
        val unknown = failLogin("nobody-here")
        val malformed = failLogin("x")
        assertEquals(wrong.code, unknown.code)
        assertEquals(wrong.message, unknown.message)
        assertEquals(wrong.message, malformed.message)
    }

    @Test
    fun passwordIsVerifiedEvenForUnknownUsersToEqualizeTiming() {
        failLogin("nobody-here")
        assertEquals(1, hasher.verifyCalls)
    }

    @Test
    fun fifthFailureIsStillInvalidCredentialsAndSixthIsLockedEvenWithCorrectPassword() {
        repeat(5) { failLogin() }
        val locked = assertFailsWith<DomainException.TooManyAttempts> { login("alice", "correct-horse") }
        assertEquals(15 * 60L, locked.retryAfterSeconds)
    }

    @Test
    fun unknownUsernamesAreLockedTheSameWay() {
        repeat(5) { failLogin("ghost-user") }
        assertFailsWith<DomainException.TooManyAttempts> { login("ghost-user", "anything") }
    }

    @Test
    fun lockedAttemptsDoNotCheckThePasswordOrExtendTheLock() {
        repeat(5) { failLogin() }
        val callsBefore = hasher.verifyCalls
        clock.advanceMinutes(5)
        val locked = assertFailsWith<DomainException.TooManyAttempts> { login("alice", "wrong") }
        assertEquals(callsBefore, hasher.verifyCalls)
        assertEquals(10 * 60L, locked.retryAfterSeconds)
    }

    @Test
    fun lockExpiresAfterFifteenMinutes() {
        repeat(5) { failLogin() }
        clock.advanceMinutes(15)
        assertEquals("alice", login("alice", "correct-horse").user.username.value)
    }

    @Test
    fun failureCountRestartsAfterLockExpiry() {
        repeat(5) { failLogin() }
        clock.advanceMinutes(16)
        repeat(4) { failLogin() }
        assertEquals("alice", login("alice", "correct-horse").user.username.value)
    }

    @Test
    fun successBeforeLockResetsTheFailureCount() {
        repeat(4) { failLogin() }
        login("alice", "correct-horse")
        assertNull(attempts.get(u("alice")))
        repeat(4) { failLogin() }
        assertEquals("alice", login("alice", "correct-horse").user.username.value)
    }
}
