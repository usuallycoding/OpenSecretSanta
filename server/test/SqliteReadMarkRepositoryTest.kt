package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.gift.ReadKind
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteReadMarkRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class SqliteReadMarkRepositoryTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val groups = SqliteGroupRepository(database)
    private val marks = SqliteReadMarkRepository(database)

    private fun u(name: String) = Username.parse(name)

    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        SqliteUserRepository(database).insertAll((names + "boss").map { NewUser(u(it), "h", null, false, it == "boss") })
    }

    private fun group(): Long {
        val g = groups.create(u("boss"), GroupDetails("Party"), names.map(::u))
        groups.moveToPending(g.id, emptyList())
        groups.activate(g.id, names.indices.map { Assignment(u(names[it]), u(names[(it + 1) % names.size])) })
        return g.id
    }

    private fun count(): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM read_marks").use { it.next(); it.getInt(1) } }
    }

    @Test
    fun nothingIsMarkedAtFirst() {
        assertEquals(emptyMap(), marks.seenUpTo(group(), u("alice")))
    }

    @Test
    fun aNewTimeIsStoredAndReadBack() {
        val id = group()

        assertEquals(500L, marks.markSeen(id, u("alice"), ReadKind.LETTERS, 500))

        assertEquals(mapOf(ReadKind.LETTERS to 500L), marks.seenUpTo(id, u("alice")))
    }

    @Test
    fun theLaterTimeWinsInEitherOrder() {
        val id = group()
        marks.markSeen(id, u("alice"), ReadKind.WISHLIST, 700)

        assertEquals(700L, marks.markSeen(id, u("alice"), ReadKind.WISHLIST, 300))
        assertEquals(900L, marks.markSeen(id, u("alice"), ReadKind.WISHLIST, 900))
        assertEquals(1, count())
    }

    @Test
    fun kindsAndSantasAreKeptApart() {
        val id = group()
        marks.markSeen(id, u("alice"), ReadKind.WISHLIST, 100)
        marks.markSeen(id, u("alice"), ReadKind.ANSWERS, 200)
        marks.markSeen(id, u("bob"), ReadKind.WISHLIST, 300)

        assertEquals(mapOf(ReadKind.WISHLIST to 100L, ReadKind.ANSWERS to 200L), marks.seenUpTo(id, u("alice")))
        assertEquals(mapOf(ReadKind.WISHLIST to 300L), marks.seenUpTo(id, u("bob")))
    }

    @Test
    fun theSameSantaInAnotherGroupHasTheirOwnMarks() {
        val first = group()
        val second = group()
        marks.markSeen(first, u("alice"), ReadKind.LETTERS, 100)

        assertEquals(emptyMap(), marks.seenUpTo(second, u("alice")))
    }

    @Test
    fun marksGoWithTheGroup() {
        val id = group()
        marks.markSeen(id, u("alice"), ReadKind.LETTERS, 100)
        marks.markSeen(id, u("bob"), ReadKind.WISHLIST, 100)
        assertEquals(2, count())

        groups.delete(id)

        assertEquals(0, count())
    }
}
