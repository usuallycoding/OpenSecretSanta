package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.AddDrawRuleRequest
import com.usuallycoding.opensecretsanta.domain.group.RuleKind
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminPhase
import com.usuallycoding.opensecretsanta.ui.admin.DrawRuleItem
import com.usuallycoding.opensecretsanta.ui.admin.DrawRulesViewModel
import com.usuallycoding.opensecretsanta.ui.admin.GroupSheet
import com.usuallycoding.opensecretsanta.ui.admin.RuleForm
import com.usuallycoding.opensecretsanta.ui.admin.inReadingOrder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DrawRulesViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val fake get() = server.adminGroups
    private val four = listOf("alice", "bobby", "carol", "daves")

    private suspend fun rules(id: Long, me: String = "admin") =
        DrawRulesViewModel(id, me, server.api, immediateScope()).also { it.refresh().join() }

    private fun pending(vararg people: String = four.toTypedArray()) = fake.add("Pending", state = "pending", participants = people.toList())

    @Test
    fun rulesAreListedAsSentencesMustsFirstThenByGiver() = runTest {
        val id = pending()
        fake.addRule(id, "daves", "alice", "never")
        fake.addRule(id, "carol", "daves", "must")
        fake.addRule(id, "alice", "bobby", "must")
        fake.addRule(id, "alice", "carol", "never")

        val data = rules(id).data.value.value!!

        assertEquals(
            listOf("alice must give to bobby", "carol must give to daves", "alice must not give to carol", "daves must not give to alice"),
            data.rules.map { it.sentence },
        )
        assertEquals(four, data.participants)
        assertEquals(AdminPhase.Pending, data.phase)
        assertTrue(data.canEdit)
    }

    @Test
    fun theReadingOrderIsAPureFunction() {
        val items = listOf(
            DrawRuleItem(1, "b", "a", RuleKind.NEVER),
            DrawRuleItem(2, "b", "c", RuleKind.MUST),
            DrawRuleItem(3, "a", "c", RuleKind.MUST),
        )
        assertEquals(listOf(3L, 2L, 1L), items.inReadingOrder().map { it.id })
    }

    @Test
    fun theFormNeedsTwoDifferentPeopleToAdd() {
        assertFalse(RuleForm().canAdd)
        assertFalse(RuleForm(giver = "alice").canAdd)
        assertFalse(RuleForm(giver = "alice", receiver = "alice").canAdd)
        assertTrue(RuleForm(giver = "alice", receiver = "bobby").canAdd)
    }

    @Test
    fun addingOnlyHappensWithAValidForm() = runTest {
        val id = pending()
        val model = rules(id)
        assertNull(model.add())
        model.setGiver("alice")
        model.setReceiver("alice")
        assertNull(model.add())
        assertEquals(0, server.calls.count { it.startsWith("POST") })
    }

    @Test
    fun aOneWayRuleIsAddedListedAndTheFormIsReset() = runTest {
        val id = pending()
        val model = rules(id)
        model.setGiver("alice"); model.setKind(RuleKind.MUST); model.setReceiver("bobby")

        model.add()!!.join()

        assertEquals(listOf("alice must give to bobby"), model.data.value.value!!.rules.map { it.sentence })
        assertEquals(RuleForm(), model.ui.value.form)
        assertNull(model.ui.value.message)
        assertFalse(model.ui.value.busy)
        assertTrue(server.bodies.last { it.contains("giver") }.contains("\"bothWays\":false") || !server.bodies.last { it.contains("giver") }.contains("true"))
    }

    @Test
    fun bothWaysAddsTwoRulesInOneRequest() = runTest {
        val id = pending()
        val model = rules(id)
        model.setGiver("carol"); model.setKind(RuleKind.NEVER); model.setReceiver("daves"); model.setBothWays(true)

        model.add()!!.join()

        assertEquals(setOf("carol must not give to daves", "daves must not give to carol"), model.data.value.value!!.rules.map { it.sentence }.toSet())
        assertEquals(1, server.calls.count { it == "POST /admin/groups/$id/rules" })
    }

    @Test
    fun aRefusedRuleSaysWhyAndKeepsTheForm() = runTest {
        val id = pending()
        val model = rules(id)
        model.setGiver("alice"); model.setReceiver("bobby")
        fake.nextRuleConflict = "alice cannot give to two people"

        model.add()!!.join()

        assertEquals("alice cannot give to two people", model.ui.value.message)
        assertEquals(RuleForm(giver = "alice", receiver = "bobby"), model.ui.value.form)
        assertEquals(emptyList(), model.data.value.value!!.rules)
        assertFalse(model.ui.value.busy)
    }

    @Test
    fun changingTheFormClearsTheMessage() = runTest {
        val id = pending()
        val model = rules(id)
        model.setGiver("alice"); model.setReceiver("bobby")
        fake.nextRuleConflict = "no"
        model.add()!!.join()

        model.setReceiver("carol")

        assertNull(model.ui.value.message)
    }

    @Test
    fun aRuleIsDeletedFromTheListAndTheServer() = runTest {
        val id = pending()
        val kept = fake.addRule(id, "alice", "bobby", "must")
        val gone = fake.addRule(id, "carol", "daves", "never")
        val model = rules(id)

        model.delete(model.data.value.value!!.rules.first { it.id == gone.id })!!.join()

        assertEquals(listOf(kept.id), model.data.value.value!!.rules.map { it.id })
        assertEquals(listOf(kept), fake.rulesOf(id))
    }

    @Test
    fun anActiveGroupShowsTheRulesButChangesNothing() = runTest {
        val id = fake.add("Active", state = "active", participants = four)
        fake.addRule(id, "alice", "bobby", "must")
        val model = rules(id)
        val data = model.data.value.value!!

        assertEquals(1, data.rules.size)
        assertFalse(data.canEdit)
        model.setGiver("alice"); model.setReceiver("carol")
        assertNull(model.add())
        assertNull(model.delete(data.rules.single()))
        assertEquals(0, server.calls.count { it.startsWith("POST") || it.startsWith("DELETE") })
    }

    @Test
    fun theFewDrawsWarningComesFromTheServer() = runTest {
        val id = pending()
        assertFalse(rules(id).data.value.value!!.fewDraws)
        fake.fewDraws = true
        assertTrue(rules(id).data.value.value!!.fewDraws)
    }

    @Test
    fun theOwnerWhoTakesPartKnowsIt() = runTest {
        val id = pending("admin", "bobby", "carol", "daves")
        assertTrue(rules(id, me = "admin").ownerTakesPart)
        assertFalse(rules(id, me = "someone").ownerTakesPart)
    }

    @Test
    fun aDraftOrAnUnknownGroupCannotBeRead() = runTest {
        val draft = fake.add("Draft", participants = four)
        assertNull(rules(draft).data.value.value)
        assertNotNull(rules(draft).data.value.error)
        assertNull(rules(999).data.value.value)
    }

    // --- on the group's own page ---

    private suspend fun group(id: Long) = AdminGroupViewModel(id, "admin", server.api, immediateScope()).also { it.refresh().join() }

    @Test
    fun theRulesOpenFromEveryStateButDraft() = runTest {
        assertFalse(group(fake.add("Draft")).canOpenRules)
        for (state in listOf("pending", "active", "archived")) {
            val model = group(fake.add(state, state = state, participants = four))
            assertTrue(model.canOpenRules, state)
        }
    }

    @Test
    fun removingPeopleTellsWhichRulesWentWithThem() = runTest {
        val id = pending()
        fake.addRule(id, "alice", "bobby", "must")
        fake.addRule(id, "alice", "carol", "never")
        val model = group(id)
        model.openParticipants()
        model.removeUsername("bobby")

        model.saveParticipants()!!.join()

        assertEquals(listOf("alice must give to bobby"), model.ui.value.rulesDeleted.map { it.sentence })
        model.dismissRulesDeleted()
        assertEquals(emptyList(), model.ui.value.rulesDeleted)
    }

    @Test
    fun savingPeopleWithoutLosingRulesSaysNothing() = runTest {
        val id = pending()
        fake.addRule(id, "alice", "bobby", "must")
        val model = group(id)
        model.openParticipants()
        model.typeUsername("erins"); model.addUsername()

        model.saveParticipants()!!.join()

        assertEquals(emptyList(), model.ui.value.rulesDeleted)
    }

    @Test
    fun aDrawTheRulesForbidStaysPendingAndSaysWhy() = runTest {
        val id = pending()
        fake.nextDrawConflict = "No draw can satisfy the rules."
        val model = group(id)

        model.ask(com.usuallycoding.opensecretsanta.ui.admin.AdminStep.DrawNames)
        model.confirmStep()!!.join()

        assertEquals("No draw can satisfy the rules.", model.ui.value.drawRefused)
        assertEquals(AdminPhase.Pending, model.data.value.value!!.phase)
        assertEquals(null, model.ui.value.notice)
        assertFalse(model.ui.value.busy)
        model.dismissDrawRefused()
        assertNull(model.ui.value.drawRefused)
    }
}
