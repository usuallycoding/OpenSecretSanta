package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.auth.ChangePassword
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ChangePasswordTest {
    private val users = InMemoryUsers()
    private val alice = users.add("alice", password = "TempPass0001", mustChange = true, tempPlain = "TempPass0001")
    private val change = ChangePassword(users, FakeHasher(), FakeTokens())

    @Test
    fun successReplacesPasswordDiscardsTempAndClearsMustChange() {
        val result = change(alice, "TempPass0001", "my-new-password")

        assertFalse(result.user.mustChangePassword)
        assertNull(users.credentials(alice)!!.encryptedTempPassword)
        assertEquals("hash:my-new-password", users.credentials(alice)!!.passwordHash)
    }

    @Test
    fun tokenVersionIsBumpedAndFreshTokenMatchesIt() {
        val result = change(alice, "TempPass0001", "my-new-password")

        assertEquals(2, result.user.tokenVersion)
        assertEquals("token:alice:2", result.token)
    }

    @Test
    fun wrongCurrentPasswordIsForbiddenAndChangesNothing() {
        assertFailsWith<DomainException.Forbidden> { change(alice, "not-it", "my-new-password") }

        assertEquals("hash:TempPass0001", users.credentials(alice)!!.passwordHash)
        assertEquals(1, users.find(alice)!!.tokenVersion)
    }

    @Test
    fun shortNewPasswordIsRejected() {
        val error = assertFailsWith<DomainException.ValidationFailed> { change(alice, "TempPass0001", "short") }
        assertEquals("newPassword", error.field)
        assertEquals("hash:TempPass0001", users.credentials(alice)!!.passwordHash)
    }

    @Test
    fun unchangedPasswordIsRejected() {
        assertFailsWith<DomainException.ValidationFailed> { change(alice, "TempPass0001", "TempPass0001") }
    }
}
