package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.AccessPolicy
import com.usuallycoding.opensecretsanta.domain.admin.GroupMembershipRepository
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileTest {

    private fun profile(
        fullName: String? = null,
        email: String? = null,
        phone: String? = null,
        address: String? = null,
        deliveryInfo: String? = null,
    ) = Profile.of(fullName, email, phone, address, deliveryInfo)

    @Test
    fun blankValuesBecomeNull() {
        val result = profile(fullName = "   ", email = "", phone = null, address = "\n", deliveryInfo = " ")
        assertEquals(Profile.EMPTY, result)
    }

    @Test
    fun valuesAreTrimmed() {
        assertEquals("Alice B", profile(fullName = "  Alice B ").fullName)
    }

    @Test
    fun validEmailIsAccepted() {
        assertEquals("a@b.co", profile(email = "a@b.co").email)
    }

    @Test
    fun invalidEmailIsRejected() {
        for (bad in listOf("plain", "a@b", "a b@c.de", "@b.co", "a@@b.co")) {
            val error = assertFailsWith<DomainException.ValidationFailed>(bad) { profile(email = bad) }
            assertEquals("email", error.field)
        }
    }

    @Test
    fun overLongFieldIsRejectedAndNamed() {
        val error = assertFailsWith<DomainException.ValidationFailed> { profile(address = "x".repeat(501)) }
        assertEquals("address", error.field)
        assertEquals("x".repeat(500), profile(address = "x".repeat(500)).address)
    }

    @Test
    fun aLineBreakInsideAnAddressIsKeptAndBlankLinesAroundItAreTrimmed() {
        assertEquals("1 Main Street\nSpringfield", profile(address = "1 Main Street\nSpringfield").address)
        assertEquals("1 Main Street\n\nSpringfield", profile(address = "\n\n  1 Main Street\n\nSpringfield \n").address)
        assertEquals("Ring twice\nleave at the door", profile(deliveryInfo = "Ring twice\nleave at the door").deliveryInfo)
    }

    @Test
    fun clearedFieldIsNull() {
        assertNull(profile(phone = null).phone)
    }
}

class AccessPolicyTest {

    private fun u(name: String) = Username.parse(name)

    private class FakeMemberships(private val owned: Map<Username, Set<Username>>) : GroupMembershipRepository {
        override fun membersOfGroupsOwnedBy(owner: Username): Set<Username> = owned[owner].orEmpty()
        override fun unassignedUsersCreatedBy(admin: Username): Set<Username> = emptySet()
    }

    private val adminA = u("admin-a")
    private val adminB = u("admin-b")
    private val policy = AccessPolicy(
        FakeMemberships(
            mapOf(
                adminA to setOf(u("alice"), u("shared")),
                adminB to setOf(u("bob"), u("shared")),
            ),
        ),
    )

    @Test
    fun adminManagesMembersOfOwnGroups() {
        assertTrue(policy.canManage(adminA, u("alice")))
    }

    @Test
    fun adminDoesNotManageMembersOfOtherAdminsGroups() {
        assertFalse(policy.canManage(adminA, u("bob")))
    }

    @Test
    fun sharedUserIsManagedByBothOwners() {
        assertTrue(policy.canManage(adminA, u("shared")))
        assertTrue(policy.canManage(adminB, u("shared")))
    }

    @Test
    fun adminManagesThemselfEvenWithoutGroups() {
        assertTrue(policy.canManage(u("lonely-admin"), u("lonely-admin")))
    }

    @Test
    fun managedUsernamesIncludeSelfAndMembers() {
        assertEquals(setOf(adminA, u("alice"), u("shared")), policy.managedUsernames(adminA))
    }

    @Test
    fun unknownUserIsNotManaged() {
        assertFalse(policy.canManage(adminA, u("nobody")))
    }
}
