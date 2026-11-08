package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.announcement.AnnouncementLimits
import com.usuallycoding.opensecretsanta.domain.announcement.AnnouncementReadMark
import com.usuallycoding.opensecretsanta.domain.announcement.GetMyAnnouncementReads
import com.usuallycoding.opensecretsanta.domain.announcement.MarkAnnouncementsRead
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnnouncementReadsTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val announcements = InMemoryAnnouncements(groups)
    private val clock = FakeClock(now = 5_000L)
    private val loader = ParticipantGroupLoader(groups)
    private val read = GetMyAnnouncementReads(loader, announcements)
    private val mark = MarkAnnouncementsRead(loader, announcements, clock)
    private val owner = users.add("boss", admin = true).let { users.userNamed("boss") }
    private val names = listOf("alice", "bob")

    init {
        for (name in names) users.add(name)
    }

    private fun alice() = users.userNamed("alice")

    private fun bob() = users.userNamed("bob")

    /** The announcements of a seeded group were sent at 1000, 1001, 1002, and so on. */
    private fun group(state: GroupState, announced: Int = 3): Group {
        val g = groups.seed(owner.username, state, *names.map(Username::parse).toTypedArray())
        announcements.seed(g.id, announced)
        return g
    }

    @Test
    fun withNothingMarkedEveryAnnouncementCounts() {
        val g = group(GroupState.ACTIVE)

        assertEquals(AnnouncementReadMark(3, null), read(alice(), g.id))
    }

    @Test
    fun onlyWhatWasSentAfterTheTimeSeenCounts() {
        val g = group(GroupState.ACTIVE)

        assertEquals(AnnouncementReadMark(1, 1_001), mark(alice(), g.id, 1_001))
        assertEquals(AnnouncementReadMark(1, 1_001), read(alice(), g.id))
    }

    @Test
    fun everyoneHasTheirOwnMark() {
        val g = group(GroupState.ACTIVE)

        mark(alice(), g.id, 1_002)

        assertEquals(AnnouncementReadMark(0, 1_002), read(alice(), g.id))
        assertEquals(AnnouncementReadMark(3, null), read(bob(), g.id))
    }

    @Test
    fun itWorksInPendingAndArchivedGroups() {
        for (state in listOf(GroupState.PENDING, GroupState.ARCHIVED)) {
            val g = group(state)
            assertEquals(3, read(alice(), g.id).unread, state.name)
            assertEquals(0, mark(alice(), g.id, 1_002).unread, state.name)
        }
    }

    @Test
    fun aStrangerADraftTheOwnerOutsideTheGroupAndAMissingGroupAreNotFound() {
        users.add("eve")
        val active = group(GroupState.ACTIVE)
        val draft = group(GroupState.DRAFT, announced = 0)
        for (call in listOf<() -> Unit>(
            { read(users.userNamed("eve"), active.id) },
            { mark(users.userNamed("eve"), active.id, 1_000) },
            { read(alice(), draft.id) },
            { mark(alice(), draft.id, 1_000) },
            { read(owner, active.id) },
            { mark(owner, active.id, 1_000) },
            { read(alice(), 9999) },
            { mark(alice(), 9999, 1_000) },
        )) {
            assertFailsWith<DomainException.NotFound> { call() }
        }
    }

    @Test
    fun aMarkNeverGoesBack() {
        val g = group(GroupState.ACTIVE)

        mark(alice(), g.id, 1_002)
        val after = mark(alice(), g.id, 1_000)

        assertEquals(AnnouncementReadMark(0, 1_002), after)
    }

    @Test
    fun aMarkIsNeverLaterThanNow() {
        val g = group(GroupState.ACTIVE)

        val marked = mark(alice(), g.id, 9_999)
        assertEquals(5_000L, marked.seenUpToMillis)

        clock.now = 6_000L
        announcements.add(g.id, "Later", "Sent after the mark", 6_000L, AnnouncementLimits.MAX_PER_GROUP)
        assertEquals(1, read(alice(), g.id).unread)
    }

    @Test
    fun theCountsOfManyGroupsComeTogether() {
        val first = group(GroupState.ACTIVE, announced = 2)
        val second = group(GroupState.PENDING, announced = 3)
        val none = group(GroupState.ACTIVE, announced = 0)
        mark(alice(), second.id, 1_000)

        assertEquals(
            mapOf(first.id to 2, second.id to 2, none.id to 0),
            announcements.unreadCounts(Username.parse("alice"), listOf(first.id, second.id, none.id)),
        )
    }
}
