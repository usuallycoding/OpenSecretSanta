package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.admin.NewAccountPlanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NewAccountPlannerTest {
    private val planner = NewAccountPlanner(FakeHasher(), FakeSecretBox(), SequenceTempPasswords())

    @Test
    fun plansOnlyMissingAccountsAndStoresNothing() {
        val planned = planner.plan(listOf(u("alice"), u("bob"), u("carol")), existing = setOf(u("alice")))

        assertEquals(listOf("bob", "carol"), planned.map { it.username.value })
    }

    @Test
    fun plannedAccountsMustChangeTheirTemporaryPasswordAndAreNotAdmins() {
        val planned = planner.plan(listOf(u("bob")), existing = emptySet()).single()

        assertTrue(planned.newUser.mustChangePassword)
        assertEquals(false, planned.newUser.isAdmin)
        assertEquals("TempPass0001", planned.temporaryPassword)
        assertEquals("hash:TempPass0001", planned.newUser.passwordHash)
        assertEquals("TempPass0001", FakeSecretBox().decrypt(assertNotNull(planned.newUser.encryptedTempPassword)))
    }

    @Test
    fun repeatedUsernamesProduceOneAccount() {
        val planned = planner.plan(listOf(u("Dave"), u("dave"), u("DAVE")), existing = emptySet())

        assertEquals(1, planned.size)
    }

    @Test
    fun eachAccountGetsItsOwnTemporaryPassword() {
        val planned = planner.plan(listOf(u("bob"), u("carol")), existing = emptySet())

        assertEquals(listOf("TempPass0001", "TempPass0002"), planned.map { it.temporaryPassword })
    }

    @Test
    fun nothingToDoWhenEveryoneHasAnAccount() {
        assertEquals(emptyList(), planner.plan(listOf(u("alice")), existing = setOf(u("alice"))))
        assertEquals(emptyList(), planner.plan(emptyList(), existing = emptySet()))
    }
}
