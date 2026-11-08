package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteAnnouncementReadRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteAnnouncementRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqliteAnnouncementReadRepositoryTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val groups = SqliteGroupRepository(database)
    private val announcements = SqliteAnnouncementRepository(database)
    private val reads = SqliteAnnouncementReadRepository(database)

    private fun u(name: String) = Username.parse(name)

    private val names = listOf("alice", "bob")

    init {
        SqliteUserRepository(database).insertAll((names + "boss").map { NewUser(u(it), "h", null, false, it == "boss") })
    }

    /** A Pending group with [count] announcements sent at 100, 101, 102, and so on. */
    private fun group(count: Int): Long {
        val g = groups.create(u("boss"), GroupDetails("Party"), names.map(::u))
        groups.moveToPending(g.id, emptyList())
        repeat(count) { announcements.add(g.id, "t$it", "b", 100L + it, 200) }
        return g.id
    }

    @Test
    fun withNoMarkEveryAnnouncementCountsAndThereIsNoTime() {
        val id = group(3)

        assertEquals(mapOf(id to 3), reads.unreadCounts(u("alice"), listOf(id)))
        assertNull(reads.seenUpTo(id, u("alice")))
    }

    @Test
    fun onlyWhatWasSentAfterTheMarkCounts() {
        val id = group(3)

        assertEquals(101, reads.markSeen(id, u("alice"), 101))

        assertEquals(mapOf(id to 1), reads.unreadCounts(u("alice"), listOf(id)))
        assertEquals(101, reads.seenUpTo(id, u("alice")))
    }

    @Test
    fun theLaterTimeIsKeptWhateverOrderTheMarksCome() {
        val id = group(3)

        reads.markSeen(id, u("alice"), 102)
        assertEquals(102, reads.markSeen(id, u("alice"), 100))

        assertEquals(mapOf(id to 0), reads.unreadCounts(u("alice"), listOf(id)))
    }

    @Test
    fun everyoneHasTheirOwnMark() {
        val id = group(2)

        reads.markSeen(id, u("alice"), 101)

        assertEquals(mapOf(id to 0), reads.unreadCounts(u("alice"), listOf(id)))
        assertEquals(mapOf(id to 2), reads.unreadCounts(u("bob"), listOf(id)))
    }

    @Test
    fun theCountsOfManyGroupsComeInOneAnswerWithAZeroForAGroupWithNone() {
        val first = group(2)
        val second = group(3)
        val none = group(0)
        reads.markSeen(second, u("alice"), 100)

        assertEquals(mapOf(first to 2, second to 2, none to 0), reads.unreadCounts(u("alice"), listOf(first, second, none)))
        assertEquals(emptyMap(), reads.unreadCounts(u("alice"), emptyList()))
    }

    @Test
    fun aRemovedPersonKeepsTheirMark() {
        val id = group(2)
        reads.markSeen(id, u("bob"), 101)

        groups.replaceParticipants(id, listOf(u("alice")), emptyList())
        groups.replaceParticipants(id, listOf(u("alice"), u("bob")), emptyList())

        assertEquals(101, reads.seenUpTo(id, u("bob")))
    }
}
