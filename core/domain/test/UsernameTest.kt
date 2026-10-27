package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.PasswordPolicy
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UsernameTest {

    @Test
    fun mixedCaseIsNormalizedToLowercase() {
        assertEquals("alice_b", Username.parse("Alice_B").value)
    }

    @Test
    fun usernamesDifferingOnlyInCaseAreEqual() {
        assertEquals(Username.parse("ALICE"), Username.parse("alice"))
        assertEquals(Username.parse("ALICE").hashCode(), Username.parse("alice").hashCode())
    }

    @Test
    fun allowedCharactersAreAccepted() {
        assertEquals("a.b_c-9", Username.parse("a.b_c-9").value)
    }

    @Test
    fun lengthBoundsAreEnforced() {
        assertEquals("abc", Username.parse("abc").value)
        assertEquals(32, Username.parse("a".repeat(32)).value.length)
        assertFailsWith<DomainException.ValidationFailed> { Username.parse("ab") }
        assertFailsWith<DomainException.ValidationFailed> { Username.parse("a".repeat(33)) }
    }

    @Test
    fun forbiddenCharactersAreRejected() {
        for (bad in listOf("has space", "at@sign", "semi;colon", "tab\tname", "ünïcode")) {
            assertFailsWith<DomainException.ValidationFailed>(bad) { Username.parse(bad) }
        }
    }

    @Test
    fun parseOrNullReturnsNullForInvalid() {
        assertNull(Username.parseOrNull("no"))
    }

    @Test
    fun validationErrorNamesTheField() {
        val error = assertFailsWith<DomainException.ValidationFailed> { Username.parse("x", field = "ADMIN_USERNAME") }
        assertEquals("ADMIN_USERNAME", error.field)
    }
}

class PasswordPolicyTest {

    @Test
    fun acceptsEightCharacters() {
        PasswordPolicy.validateNew("12345678")
    }

    @Test
    fun rejectsShortPassword() {
        assertFailsWith<DomainException.ValidationFailed> { PasswordPolicy.validateNew("1234567") }
    }

    @Test
    fun rejectsPasswordEqualToCurrent() {
        assertFailsWith<DomainException.ValidationFailed> { PasswordPolicy.validateNew("samepassword", current = "samepassword") }
        PasswordPolicy.validateNew("different-one", current = "samepassword")
    }
}
