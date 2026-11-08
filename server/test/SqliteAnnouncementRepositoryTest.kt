package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteAnnouncementRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SqliteAnnouncementRepositoryTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val groups = SqliteGroupRepository(database)
    private val announcements = SqliteAnnouncementRepository(database)

    private fun u(name: String) = Username.parse(name)

    private val names = listOf("alice", "bob", "carol")

    init {
        SqliteUserRepository(database).insertAll((names + "boss").map { NewUser(u(it), "h", null, false, it == "boss") })
    }

    private fun group(pending: Boolean = true, activate: Boolean = false, archive: Boolean = false): Long {
        val g = groups.create(u("boss"), GroupDetails("Party"), names.map(::u))
        if (pending) groups.moveToPending(g.id, emptyList())
        if (activate) groups.activate(g.id, names.indices.map { Assignment(u(names[it]), u(names[(it + 1) % names.size])) })
        if (archive) groups.archive(g.id)
        return g.id
    }

    private fun count(): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM announcements").use { it.next(); it.getInt(1) } }
    }

    @Test
    fun announcementsAreNumberedWithoutGapsPerGroup() {
        val first = group()
        val second = group()

        val numbers = listOf(
            announcements.add(first, "a", "b1", 100, 200).number,
            announcements.add(second, "a", "c1", 101, 200).number,
            announcements.add(first, "a", "b2", 102, 200).number,
            announcements.add(first, "a", "b3", 103, 200).number,
        )

        assertEquals(listOf(1, 1, 2, 3), numbers)
        assertEquals(listOf("b1", "b2", "b3"), announcements.list(first).map { it.body })
    }

    @Test
    fun lineBreaksAndTheTimeAreKept() {
        val id = group()

        announcements.add(id, "Schedule change", "one\ntwo\nthree", 123_456, 200)

        val kept = announcements.list(id).single()
        assertEquals("Schedule change", kept.title)
        assertEquals("one\ntwo\nthree", kept.body)
        assertEquals(123_456, kept.sentAtMillis)
    }

    @Test
    fun theLimitIsEnforcedPerGroupAndLeavesNothingBehind() {
        val id = group()
        repeat(3) { announcements.add(id, "t", "x$it", it.toLong(), 3) }

        assertFailsWith<DomainException.InvalidState> { announcements.add(id, "t", "too many", 9, 3) }

        assertEquals(3, count())
        announcements.add(group(), "t", "another group is separate", 10, 3)
    }

    @Test
    fun announcementsAreWrittenOnlyWhilePendingOrActive() {
        announcements.add(group(), "t", "pending", 1, 200)
        announcements.add(group(activate = true), "t", "active", 2, 200)
        assertFailsWith<DomainException.InvalidState> { announcements.add(group(pending = false), "t", "draft", 3, 200) }
        assertFailsWith<DomainException.InvalidState> { announcements.add(group(activate = true, archive = true), "t", "archived", 4, 200) }
        assertFailsWith<DomainException.NotFound> { announcements.add(9999, "t", "missing", 5, 200) }

        assertEquals(2, count())
    }

    @Test
    fun anArchivedGroupsAnnouncementsAreStillReadable() {
        val id = group(activate = true)
        announcements.add(id, "Kept", "x", 1, 200)
        groups.archive(id)

        assertEquals(listOf("Kept"), announcements.list(id).map { it.title })
    }

    @Test
    fun announcementsGoWithTheGroup() {
        val id = group()
        announcements.add(id, "Gone soon", "x", 1, 200)
        assertEquals(1, count())

        groups.delete(id)

        assertEquals(0, count())
    }
}
