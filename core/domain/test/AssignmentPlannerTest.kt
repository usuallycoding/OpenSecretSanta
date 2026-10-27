package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.Shuffler
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IdentityShuffler : Shuffler {
    override fun <T> shuffled(items: List<T>): List<T> = items
}

class ReversingShuffler : Shuffler {
    override fun <T> shuffled(items: List<T>): List<T> = items.reversed()
}

class SeededShuffler(seed: Int) : Shuffler {
    private val random = Random(seed)
    override fun <T> shuffled(items: List<T>): List<T> = items.shuffled(random)
}

class AssignmentPlannerTest {

    private fun people(count: Int): List<Username> = List(count) { Username.parse("person-${it.toString().padStart(2, '0')}") }

    /** Follows receivers from the first santa and returns the people visited before coming back. */
    private fun chainFrom(start: Username, assignments: List<Assignment>): List<Username> {
        val receiverOf = assignments.associate { it.santa to it.receiver }
        val visited = mutableListOf<Username>()
        var current = start
        do {
            visited += current
            current = receiverOf.getValue(current)
        } while (current != start)
        return visited
    }

    private fun assertValid(participants: List<Username>, assignments: List<Assignment>, label: String) {
        assertEquals(participants.size, assignments.size, label)
        assertEquals(participants.toSet(), assignments.map { it.santa }.toSet(), "$label: every participant is a santa once")
        assertEquals(participants.toSet(), assignments.map { it.receiver }.toSet(), "$label: every participant is a receiver once")
        assertEquals(participants.size, assignments.map { it.santa }.distinct().size, label)
        assertEquals(participants.size, assignments.map { it.receiver }.distinct().size, label)
        assertTrue(assignments.none { it.santa == it.receiver }, "$label: nobody draws themself")
        val chain = chainFrom(participants.first(), assignments)
        assertEquals(participants.size, chain.size, "$label: one chain through everyone")
        assertEquals(participants.toSet(), chain.toSet(), label)
    }

    @Test
    fun identityOrderGivesToTheNextPersonAndWrapsAround() {
        val p = people(3)

        val result = AssignmentPlanner(IdentityShuffler()).plan(p)

        assertEquals(
            listOf(Assignment(p[0], p[1]), Assignment(p[1], p[2]), Assignment(p[2], p[0])),
            result,
        )
    }

    @Test
    fun everySizeFromThreeToFiftyIsValidWithFixedShufflers() {
        for (size in 3..50) {
            val p = people(size)
            assertValid(p, AssignmentPlanner(IdentityShuffler()).plan(p), "identity $size")
            assertValid(p, AssignmentPlanner(ReversingShuffler()).plan(p), "reversed $size")
        }
    }

    @Test
    fun everySizeIsValidWithManyRandomShuffles() {
        for (size in 3..20) {
            val p = people(size)
            repeat(50) { seed -> assertValid(p, AssignmentPlanner(SeededShuffler(seed * 31 + size)).plan(p), "size $size seed $seed") }
        }
    }

    @Test
    fun differentShufflesGiveDifferentDraws() {
        val p = people(10)
        val draws = (1..20).map { AssignmentPlanner(SeededShuffler(it)).plan(p).toSet() }.toSet()
        assertTrue(draws.size > 15, "expected varied draws but got ${draws.size} distinct")
    }

    @Test
    fun theInputOrderDoesNotDecideTheOutcome() {
        val p = people(6)
        val forward = AssignmentPlanner(SeededShuffler(7)).plan(p)
        val backward = AssignmentPlanner(SeededShuffler(7)).plan(p.reversed())
        assertNotEquals(forward.toSet(), backward.toSet())
    }

    @Test
    fun twoParticipantsStillFormAValidSwap() {
        val p = people(2)
        assertValid(p, AssignmentPlanner(IdentityShuffler()).plan(p), "two")
    }

    @Test
    fun fewerThanTwoParticipantsCannotBeAssigned() {
        assertFailsWith<IllegalArgumentException> { AssignmentPlanner(IdentityShuffler()).plan(people(1)) }
        assertFailsWith<IllegalArgumentException> { AssignmentPlanner(IdentityShuffler()).plan(emptyList()) }
    }

    @Test
    fun duplicateParticipantsAreRejected() {
        val p = people(3)
        assertFailsWith<IllegalArgumentException> { AssignmentPlanner(IdentityShuffler()).plan(p + p.first()) }
    }

    @Test
    fun aShufflerThatLosesSomeoneIsDetected() {
        val dropping = object : Shuffler {
            override fun <T> shuffled(items: List<T>): List<T> = items.drop(1) + items.first()
        }
        val broken = object : Shuffler {
            override fun <T> shuffled(items: List<T>): List<T> = items.take(items.size - 1) + items.first()
        }
        AssignmentPlanner(dropping).plan(people(4))
        assertFailsWith<IllegalStateException> { AssignmentPlanner(broken).plan(people(4)) }
    }
}
