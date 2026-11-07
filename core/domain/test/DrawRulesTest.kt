package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.AddDrawRules
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.DeleteDrawRule
import com.usuallycoding.opensecretsanta.domain.group.DrawRule
import com.usuallycoding.opensecretsanta.domain.group.DrawRules
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.ListDrawRules
import com.usuallycoding.opensecretsanta.domain.group.NewDrawRule
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.RuleKind
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawRulesTest {
    private fun people(count: Int): List<Username> = List(count) { Username.parse("person-${it.toString().padStart(2, '0')}") }

    private fun must(giver: Username, receiver: Username) = DrawRule(0, giver, receiver, RuleKind.MUST)
    private fun never(giver: Username, receiver: Username) = DrawRule(0, giver, receiver, RuleKind.NEVER)
    private fun newMust(giver: Username, receiver: Username) = NewDrawRule(giver, receiver, RuleKind.MUST)
    private fun newNever(giver: Username, receiver: Username) = NewDrawRule(giver, receiver, RuleKind.NEVER)

    private fun assertOneChain(participants: List<Username>, draw: List<Assignment>) {
        assertEquals(participants.toSet(), draw.map { it.santa }.toSet())
        assertEquals(participants.toSet(), draw.map { it.receiver }.toSet())
        assertTrue(draw.none { it.santa == it.receiver })
        val receiverOf = draw.associate { it.santa to it.receiver }
        var current = participants.first()
        val visited = mutableSetOf<Username>()
        do {
            visited += current
            current = receiverOf.getValue(current)
        } while (current != participants.first())
        assertEquals(participants.size, visited.size, "one chain through everybody")
    }

    // --- the planner with rules ---

    @Test
    fun aMustIsAlwaysHonoured() {
        val p = people(6)
        repeat(100) { seed ->
            val draw = AssignmentPlanner(SeededShuffler(seed)).plan(p, listOf(must(p[2], p[4])))
            assertOneChain(p, draw)
            assertEquals(p[4], draw.first { it.santa == p[2] }.receiver)
        }
    }

    @Test
    fun aNeverIsNeverDrawn() {
        val p = people(5)
        repeat(100) { seed ->
            val draw = AssignmentPlanner(SeededShuffler(seed)).plan(p, listOf(never(p[0], p[1]), never(p[3], p[0])))
            assertOneChain(p, draw)
            assertTrue(Assignment(p[0], p[1]) !in draw)
            assertTrue(Assignment(p[3], p[0]) !in draw)
        }
    }

    @Test
    fun mustsAndNeversTogetherAreHonoured() {
        val p = people(8)
        val rules = listOf(must(p[0], p[1]), must(p[1], p[2]), must(p[5], p[6]), never(p[2], p[3]), never(p[4], p[5]), never(p[7], p[0]))
        repeat(100) { seed ->
            val draw = AssignmentPlanner(SeededShuffler(seed)).plan(p, rules)
            assertOneChain(p, draw)
            rules.filter { it.kind == RuleKind.MUST }.forEach { assertTrue(Assignment(it.giver, it.receiver) in draw, it.describe()) }
            rules.filter { it.kind == RuleKind.NEVER }.forEach { assertTrue(Assignment(it.giver, it.receiver) !in draw, it.describe()) }
        }
    }

    @Test
    fun theMustsCanBeTheWholeChain() {
        val p = people(4)
        val rules = listOf(must(p[0], p[1]), must(p[1], p[2]), must(p[2], p[3]), must(p[3], p[0]))
        val draw = AssignmentPlanner(SeededShuffler(1)).plan(p, rules)
        assertOneChain(p, draw)
        assertEquals(setOf(Assignment(p[0], p[1]), Assignment(p[1], p[2]), Assignment(p[2], p[3]), Assignment(p[3], p[0])), draw.toSet())
    }

    @Test
    fun rulesStillLeaveTheDrawRandom() {
        val p = people(7)
        val draws = (1..40).map { AssignmentPlanner(SeededShuffler(it)).plan(p, listOf(must(p[0], p[1]), never(p[2], p[3]))).toSet() }.toSet()
        assertTrue(draws.size > 20, "expected varied draws but got ${draws.size}")
    }

    @Test
    fun rulesNoDrawCanSatisfyAreRefusedByThePlanner() {
        val p = people(4)
        // person-00 may give to nobody
        val rules = listOf(never(p[0], p[1]), never(p[0], p[2]), never(p[0], p[3]))
        assertFailsWith<DomainException.RulesConflict> { AssignmentPlanner(SeededShuffler(1)).plan(p, rules) }
    }

    @Test
    fun noRulesGivesTheOrderOfTheShufflerAsBefore() {
        val p = people(4)
        assertEquals(
            listOf(Assignment(p[0], p[1]), Assignment(p[1], p[2]), Assignment(p[2], p[3]), Assignment(p[3], p[0])),
            AssignmentPlanner(IdentityShuffler()).plan(p, emptyList()),
        )
    }

    @Test
    fun aHopelessSearchGivesUpInsteadOfHanging() {
        val p = people(30)
        // nobody may give to person-00, which only fails once every chain has been laid out
        val rules = p.drop(1).map { never(it, p[0]) }
        assertFailsWith<DomainException.RulesConflict> { AssignmentPlanner(SeededShuffler(1)).plan(p, rules) }
    }

    // --- checking a rule to add ---

    private fun conflict(participants: List<Username>, existing: List<DrawRule>, vararg added: NewDrawRule): String =
        assertFailsWith<DomainException.RulesConflict> { DrawRules.checkAddition(participants, existing, added.toList()) }.message!!

    @Test
    fun aSensibleRuleIsAccepted() {
        val p = people(5)
        DrawRules.checkAddition(p, listOf(must(p[0], p[1])), listOf(newNever(p[2], p[3])))
    }

    @Test
    fun aRuleNeedsParticipantsWhoDiffer() {
        val p = people(4)
        val outsider = Username.parse("outsider")
        assertEquals("giver", assertFailsWith<DomainException.ValidationFailed> { DrawRules.checkAddition(p, emptyList(), listOf(newMust(outsider, p[0]))) }.field)
        assertEquals("receiver", assertFailsWith<DomainException.ValidationFailed> { DrawRules.checkAddition(p, emptyList(), listOf(newMust(p[0], outsider))) }.field)
        assertEquals("receiver", assertFailsWith<DomainException.ValidationFailed> { DrawRules.checkAddition(p, emptyList(), listOf(newMust(p[0], p[0]))) }.field)
    }

    @Test
    fun twoMustsForOneGiverNameBothRules() {
        val p = people(5)
        val message = conflict(p, listOf(must(p[0], p[1])), newMust(p[0], p[2]))
        assertTrue("person-00 must give to person-01" in message && "person-00 must give to person-02" in message, message)
    }

    @Test
    fun twoMustsForOneReceiverAreRefused() {
        val p = people(5)
        conflict(p, listOf(must(p[0], p[2])), newMust(p[1], p[2]))
    }

    @Test
    fun aMustAndANeverOnThePairContradict() {
        val p = people(5)
        conflict(p, listOf(must(p[0], p[1])), newNever(p[0], p[1]))
        conflict(p, listOf(never(p[0], p[1])), newMust(p[0], p[1]))
    }

    @Test
    fun aRepeatedRuleIsRefused() {
        val p = people(5)
        conflict(p, listOf(never(p[0], p[1])), newNever(p[0], p[1]))
    }

    @Test
    fun mustsThatClosetASmallCircleAreRefused() {
        val p = people(4)
        val message = conflict(p, listOf(must(p[0], p[1])), newMust(p[1], p[0]))
        assertTrue("only give to each other" in message, message)
        conflict(p, listOf(must(p[0], p[1]), must(p[1], p[2])), newMust(p[2], p[0]))
    }

    @Test
    fun aCircleOfEverybodyIsAccepted() {
        val p = people(3)
        DrawRules.checkAddition(p, listOf(must(p[0], p[1]), must(p[1], p[2])), listOf(newMust(p[2], p[0])))
    }

    @Test
    fun neversThatLeaveNoChainAreRefused() {
        val p = people(4)
        conflict(p, listOf(never(p[0], p[1]), never(p[0], p[2])), newNever(p[0], p[3]))
        // two people who can only give to the same one
        conflict(p, listOf(never(p[0], p[1]), never(p[0], p[2]), never(p[3], p[1])), newNever(p[3], p[2]))
    }

    @Test
    fun aBothWaysPairIsCheckedAsAWhole() {
        val p = people(4)
        conflict(p, listOf(must(p[0], p[1])), newNever(p[0], p[2]), newMust(p[0], p[3]))
    }

    // --- few possible draws ---

    @Test
    fun noRulesNeverWarn() {
        assertFalse(DrawRules.fewDraws(people(4), emptyList()))
    }

    @Test
    fun plentyOfRoomDoesNotWarn() {
        val p = people(8)
        assertFalse(DrawRules.fewDraws(p, listOf(never(p[0], p[1]))))
    }

    @Test
    fun aSingleDrawLeftWarns() {
        val p = people(4)
        // 0->1 and 1->2 and 2->3 are all that is left: 3->0 closes it
        val rules = listOf(must(p[0], p[1]), must(p[1], p[2]))
        assertTrue(DrawRules.fewDraws(p, rules))
    }

    // --- use cases ---

    private val users = InMemoryUsers()
    private val rules = InMemoryDrawRules()
    private val groups = InMemoryGroups(users, rules)
    private val admin = users.add("admin-a", admin = true).let { users.userNamed("admin-a") }
    private val other = users.add("admin-b", admin = true).let { users.userNamed("admin-b") }
    private val loader = OwnedGroupLoader(groups)
    private val add = AddDrawRules(loader, rules)
    private val list = ListDrawRules(loader, rules)
    private val delete = DeleteDrawRule(loader, rules)

    private fun pending(count: Int = 5) = groups.seed(admin.username, GroupState.PENDING, *people(count).toTypedArray())

    @Test
    fun anOwnerAddsAndListsRulesInPending() {
        val g = pending()
        val stored = add(admin, g.id, "person-00", "person-01", RuleKind.MUST, bothWays = false)
        assertEquals(1, stored.size)
        assertEquals(stored, list(admin, g.id).rules)
    }

    @Test
    fun bothWaysStoresTwoIndependentRules() {
        val g = pending()
        val stored = add(admin, g.id, "person-02", "person-03", RuleKind.NEVER, bothWays = true)
        assertEquals(listOf("person-02" to "person-03", "person-03" to "person-02"), stored.map { it.giver.value to it.receiver.value })
        delete(admin, g.id, stored[0].id)
        assertEquals(listOf(stored[1]), list(admin, g.id).rules)
    }

    @Test
    fun aConflictingBothWaysAddStoresNeitherRule() {
        val g = pending(4)
        add(admin, g.id, "person-00", "person-01", RuleKind.MUST, bothWays = false)
        assertFailsWith<DomainException.RulesConflict> { add(admin, g.id, "person-01", "person-00", RuleKind.MUST, bothWays = true) }
        assertEquals(1, list(admin, g.id).rules.size)
    }

    @Test
    fun draftHasNoRules() {
        val g = groups.seed(admin.username, GroupState.DRAFT, *people(5).toTypedArray())
        assertFailsWith<DomainException.InvalidState> { list(admin, g.id) }
        assertFailsWith<DomainException.InvalidState> { add(admin, g.id, "person-00", "person-01", RuleKind.MUST, false) }
    }

    @Test
    fun rulesAreFrozenOnceTheGroupIsActiveButStillReadable() {
        val g = pending()
        val stored = add(admin, g.id, "person-00", "person-01", RuleKind.MUST, false)
        groups.setState(g.id, GroupState.ACTIVE)
        assertFailsWith<DomainException.InvalidState> { add(admin, g.id, "person-02", "person-03", RuleKind.NEVER, false) }
        assertFailsWith<DomainException.InvalidState> { delete(admin, g.id, stored[0].id) }
        assertEquals(stored, list(admin, g.id).rules)
        assertFalse(list(admin, g.id).fewDraws)
        groups.setState(g.id, GroupState.ARCHIVED)
        assertEquals(stored, list(admin, g.id).rules)
    }

    @Test
    fun onlyTheOwnerManagesRules() {
        val g = pending()
        assertFailsWith<DomainException.NotFound> { list(other, g.id) }
        assertFailsWith<DomainException.NotFound> { add(other, g.id, "person-00", "person-01", RuleKind.MUST, false) }
        val plain = users.add("someone").let { users.userNamed("someone") }
        assertFailsWith<DomainException.Forbidden> { list(plain, g.id) }
    }

    @Test
    fun deletingAnUnknownRuleIsNotFound() {
        val g = pending()
        assertFailsWith<DomainException.NotFound> { delete(admin, g.id, 999) }
    }

    @Test
    fun activationHonoursTheRulesAndIsRefusedWhenTheyCannotBeMet() {
        val g = pending(4)
        val activate = ActivateGroup(loader, groups, AssignmentPlanner(SeededShuffler(3)), rules, GroupViewer(users, FakeDeliveryInfos()))
        add(admin, g.id, "person-00", "person-02", RuleKind.MUST, false)
        activate(admin, g.id)
        assertEquals(u("person-02"), groups.receiverOf(g.id, u("person-00")))

        val blocked = pending(4)
        // person-00 can give to nobody: stored directly, as if the roster had changed under the rules
        rules.add(blocked.id, listOf(newNever(u("person-00"), u("person-01")), newNever(u("person-00"), u("person-02")), newNever(u("person-00"), u("person-03"))))
        assertFailsWith<DomainException.RulesConflict> { activate(admin, blocked.id) }
        assertEquals(GroupState.PENDING, groups.find(blocked.id)!!.state)
        assertEquals(emptyList(), groups.assignments(blocked.id))
    }
}
