package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.auth.ChangePassword
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.UpdateProfile
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

class UpdateProfileTest {
    private val users = InMemoryUsers()
    private val alice = users.add("alice")
    private val update = UpdateProfile(users)

    @Test
    fun updatesOwnProfile() {
        val profile = Profile.of("Alice B", "a@b.co", "123", "1 Main St", "Ring twice")

        val result = update(alice, requestedUsername = null, profile = profile)

        assertEquals(profile, result.profile)
        assertEquals(profile, users.find(alice)!!.profile)
    }

    @Test
    fun fieldCanBeCleared() {
        update(alice, null, Profile.of("Alice", null, "123", null, null))
        val result = update(alice, null, Profile.of("Alice", null, "  ", null, null))
        assertNull(result.profile.phone)
    }

    @Test
    fun sameUsernameInRequestIsAccepted() {
        update(alice, requestedUsername = "ALICE", profile = Profile.EMPTY)
    }

    @Test
    fun differentUsernameIsRejectedAndNothingChanges() {
        users.updateProfile(alice, Profile(fullName = "Before"))

        val error = assertFailsWith<DomainException.ValidationFailed> {
            update(alice, requestedUsername = "mallory", profile = Profile(fullName = "After"))
        }

        assertEquals("username", error.field)
        assertEquals("Before", users.find(alice)!!.profile.fullName)
    }

    @Test
    fun onlyTheActorsRecordIsTouched() {
        val bob = users.add("bob")
        update(alice, null, Profile(fullName = "Alice only"))
        assertNull(users.find(bob)!!.profile.fullName)
    }
}
