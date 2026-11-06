package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteLetterRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SqliteLetterRepositoryTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val groups = SqliteGroupRepository(database)
    private val letters = SqliteLetterRepository(database)

    private fun u(name: String) = Username.parse(name)

    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        SqliteUserRepository(database).insertAll((names + "boss").map { NewUser(u(it), "h", null, false, it == "boss") })
    }

    /** alice gives to bob, bob to carol, carol to dave, dave to alice. */
    private fun group(activate: Boolean = true, archive: Boolean = false): Long {
        val g = groups.create(u("boss"), GroupDetails("Party"), names.map(::u))
        groups.moveToPending(g.id, emptyList())
        if (activate) {
            groups.activate(g.id, names.indices.map { Assignment(u(names[it]), u(names[(it + 1) % names.size])) })
            if (archive) groups.archive(g.id)
        }
        return g.id
    }

    private fun count(where: String = "1=1"): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM letters WHERE $where").use { it.next(); it.getInt(1) } }
    }

    @Test
    fun lettersAreNumberedWithoutGapsPerReceiver() {
        val id = group()

        val numbers = listOf(
            letters.send(id, u("bob"), "t", "b1", 100, 30).number,
            letters.send(id, u("carol"), "t", "c1", 101, 30).number,
            letters.send(id, u("bob"), "t", "b2", 102, 30).number,
            letters.send(id, u("bob"), "t", "b3", 103, 30).number,
        )

        assertEquals(listOf(1, 1, 2, 3), numbers)
        assertEquals(listOf("b1", "b2", "b3"), letters.sentBy(id, u("bob")).map { it.text })
    }

    @Test
    fun theSameReceiverInAnotherGroupStartsAgainAtOne() {
        val first = group()
        val second = group()

        letters.send(first, u("bob"), "t", "x", 1, 30)
        letters.send(first, u("bob"), "t", "y", 2, 30)

        assertEquals(1, letters.send(second, u("bob"), "t", "z", 3, 30).number)
    }

    @Test
    fun theCapIsEnforcedPerReceiverAndLeavesNothingBehind() {
        val id = group()
        repeat(3) { letters.send(id, u("bob"), "t", "x$it", it.toLong(), 3) }

        assertFailsWith<DomainException.InvalidState> { letters.send(id, u("bob"), "t", "too many", 9, 3) }
        assertEquals(3, count("receiver = 'bob'"))
        letters.send(id, u("carol"), "t", "carol is separate", 10, 3)
    }

    @Test
    fun lettersAreWrittenOnlyWhileTheGroupIsActive() {
        assertFailsWith<DomainException.InvalidState> { letters.send(group(activate = false), u("bob"), "t", "x", 1, 30) }
        assertFailsWith<DomainException.InvalidState> { letters.send(group(archive = true), u("bob"), "t", "x", 1, 30) }
        assertFailsWith<DomainException.NotFound> { letters.send(9999, u("bob"), "t", "x", 1, 30) }
        assertEquals(0, count())
    }

    @Test
    fun aPersonWithoutAnAssignmentCannotSend() {
        val id = group()
        SqliteUserRepository(database).insertAll(listOf(NewUser(u("erin"), "h", null, false, false)))

        assertFailsWith<DomainException.NotFound> { letters.send(id, u("erin"), "t", "x", 1, 30) }
    }

    @Test
    fun theSantaReadsTheirReceiversLettersFromTheAssignment() {
        val id = group()
        letters.send(id, u("bob"), "To alice", "x", 1, 30)
        letters.send(id, u("carol"), "To bob", "y", 2, 30)

        assertEquals(listOf("To alice"), letters.sentToSanta(id, u("alice")).map { it.title })
        assertEquals(listOf("To bob"), letters.sentToSanta(id, u("bob")).map { it.title })
        assertEquals(emptyList(), letters.sentToSanta(id, u("carol")))
    }

    @Test
    fun anArchivedGroupsLettersAreStillReadable() {
        val id = group()
        letters.send(id, u("bob"), "Kept", "x", 1, 30)
        groups.archive(id)

        assertEquals(listOf("Kept"), letters.sentBy(id, u("bob")).map { it.title })
        assertEquals(listOf("Kept"), letters.sentToSanta(id, u("alice")).map { it.title })
    }

    @Test
    fun lettersGoWithTheGroup() {
        val id = group()
        letters.send(id, u("bob"), "Gone soon", "x", 1, 30)
        assertEquals(1, count())

        groups.delete(id)

        assertEquals(0, count())
    }
}
