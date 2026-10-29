package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.DeletionImpact
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistEntry
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteQuestionRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteWishlistRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import java.sql.SQLException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqliteGroupRepositoryTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val users = SqliteUserRepository(database)
    private val groups = SqliteGroupRepository(database)
    private val wishlists = SqliteWishlistRepository(database)
    private val questions = SqliteQuestionRepository(database)

    private fun u(name: String) = Username.parse(name)

    private fun newUser(name: String) = NewUser(u(name), "hash-$name", byteArrayOf(1), mustChangePassword = true, isAdmin = false)

    private val owner = u("boss").also { users.insertAll(listOf(NewUser(it, "h", null, false, true))) }

    private fun draft(vararg participants: String, name: String = "Party") =
        groups.create(owner, GroupDetails(name), participants.map(::u))

    /** An Active group of alice, bob, carol and dave drawn in that order (alice gives to bob, and so on). */
    private fun activeGroup(): Long {
        val names = listOf("alice", "bob", "carol", "dave")
        names.filter { users.find(u(it)) == null }.forEach { users.insertAll(listOf(newUser(it))) }
        val g = groups.create(owner, GroupDetails("Party"), names.map(::u))
        groups.moveToPending(g.id, emptyList())
        groups.activate(g.id, ring(*names.toTypedArray()))
        return g.id
    }

    private fun ring(vararg names: String): List<Assignment> =
        names.indices.map { Assignment(u(names[it]), u(names[(it + 1) % names.size])) }

    private fun count(table: String, where: String = "1=1"): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM $table WHERE $where").use { it.next(); it.getInt(1) } }
    }

    // --- create and read ---

    @Test
    fun createdGroupReadsBackInDraftWithItsDetailsAndOrderedParticipants() {
        val details = GroupDetails("Office party", "Bring snacks", "HQ", LocalDate(2026, 12, 24), LocalTime(18, 30))

        val created = groups.create(owner, details, listOf(u("zed"), u("amy"), u("kim")))

        val read = assertNotNull(groups.find(created.id))
        assertEquals(created, read)
        assertEquals(GroupState.DRAFT, read.state)
        assertEquals(owner, read.owner)
        assertEquals(details, read.details)
        assertEquals(listOf("zed", "amy", "kim"), read.participants.map { it.value })
    }

    @Test
    fun minimalGroupHasEmptyOptionalFields() {
        val read = groups.find(draft().id)!!
        assertEquals(GroupDetails("Party"), read.details)
        assertEquals(emptyList(), read.participants)
    }

    @Test
    fun unknownGroupIsNull() {
        assertNull(groups.find(404))
        assertEquals(emptyList(), groups.assignments(404))
    }

    @Test
    fun participantsNeedNoAccount() {
        val created = draft("no-account-yet")
        assertEquals(listOf("no-account-yet"), groups.find(created.id)!!.participants.map { it.value })
        assertNull(users.find(u("no-account-yet")))
    }

    @Test
    fun ownedGroupsAreListedNewestFirstAndOnlyForTheirOwner() {
        users.insertAll(listOf(NewUser(u("other-admin"), "h", null, false, true)))
        val first = draft(name = "First")
        groups.create(u("other-admin"), GroupDetails("Not mine"), emptyList())
        val second = draft(name = "Second")

        assertEquals(listOf(second.id, first.id), groups.findOwnedBy(owner).map { it.id })
        assertEquals(1, groups.findOwnedBy(u("other-admin")).size)
    }

    private fun createdAt(group: Long, time: String) = database.connection().use { c ->
        c.createStatement().use { it.execute("UPDATE groups SET created_at = '$time' WHERE id = $group") }
    }

    @Test
    fun theTimeOfCreationDecidesTheOrderNotTheId() {
        val earlier = draft(name = "Earlier")
        val later = draft(name = "Later")
        createdAt(earlier.id, "2026-02-01 09:00:00")
        createdAt(later.id, "2026-01-01 09:00:00")

        assertEquals(listOf(earlier.id, later.id), groups.findOwnedBy(owner).map { it.id })
    }

    @Test
    fun groupsCreatedInTheSameSecondComeLastCreatedFirst() {
        val a = draft(name = "A")
        val b = draft(name = "B")
        val c = draft(name = "C")
        listOf(a, b, c).forEach { createdAt(it.id, "2026-03-01 12:00:00") }

        assertEquals(listOf(c.id, b.id, a.id), groups.findOwnedBy(owner).map { it.id })
    }

    @Test
    fun groupsParticipantsSeeAreNewestFirstToo() {
        users.insertAll(listOf(newUser("alice")))
        val old = draft("alice", name = "Old").also { groups.moveToPending(it.id, emptyList()) }
        val middle = draft("alice", name = "Middle").also { groups.moveToPending(it.id, emptyList()) }
        val new = draft("alice", name = "New").also { groups.moveToPending(it.id, emptyList()) }
        createdAt(old.id, "2026-01-01 00:00:00")
        createdAt(middle.id, "2026-02-01 00:00:00")
        createdAt(new.id, "2026-03-01 00:00:00")

        assertEquals(listOf(new.id, middle.id, old.id), groups.findVisibleTo(u("alice")).map { it.id })
    }

    @Test
    fun changingAGroupNeverMovesItInTheList() {
        users.insertAll(listOf("alice", "bob", "carol").map(::newUser))
        val oldest = draft("alice", "bob", "carol", name = "Oldest")
        val newest = draft("alice", name = "Newest")
        createdAt(oldest.id, "2026-01-01 00:00:00")
        createdAt(newest.id, "2026-02-01 00:00:00")

        groups.updateDetails(oldest.id, GroupDetails("Oldest, renamed"))
        groups.replaceParticipants(oldest.id, listOf(u("alice"), u("bob"), u("carol")), emptyList())
        groups.moveToPending(oldest.id, emptyList())
        groups.activate(oldest.id, ring("alice", "bob", "carol"))
        groups.archive(oldest.id)

        assertEquals(listOf(newest.id, oldest.id), groups.findOwnedBy(owner).map { it.id })
    }

    @Test
    fun visibleGroupsExcludeDraftsAndStrangers() {
        users.insertAll(listOf(newUser("alice")))
        val inDraft = draft("alice", name = "Draft")
        val pending = draft("alice", name = "Pending").also { groups.moveToPending(it.id, emptyList()) }
        draft("someone-else", name = "Other").also { groups.moveToPending(it.id, emptyList()) }

        val visible = groups.findVisibleTo(u("alice"))

        assertEquals(listOf(pending.id), visible.map { it.id })
        assertTrue(inDraft.id !in visible.map { it.id })
    }

    // --- details and delete ---

    @Test
    fun detailsCanBeUpdatedUntilArchived() {
        val g = draft("a-one", "b-two", "c-three")
        groups.moveToPending(g.id, emptyList())
        groups.activate(g.id, ring("a-one", "b-two", "c-three"))

        val updated = groups.updateDetails(g.id, GroupDetails("Renamed", location = "Elsewhere", date = LocalDate(2027, 1, 2)))

        assertEquals("Renamed", updated.details.name)
        assertEquals(LocalDate(2027, 1, 2), groups.find(g.id)!!.details.date)
        assertEquals(GroupState.ACTIVE, updated.state)

        groups.archive(g.id)
        assertFailsWith<DomainException.InvalidState> { groups.updateDetails(g.id, GroupDetails("Too late")) }
        assertEquals("Renamed", groups.find(g.id)!!.details.name)
    }

    @Test
    fun updatingAMissingGroupIsNotFound() {
        assertFailsWith<DomainException.NotFound> { groups.updateDetails(404, GroupDetails("x")) }
    }

    @Test
    fun aDraftIsDeletedWithItsParticipants() {
        val g = draft("alice", "bob-b")
        groups.delete(g.id)
        assertNull(groups.find(g.id))
        assertEquals(0, count("group_participants", "group_id = ${g.id}"))
        assertFailsWith<DomainException.NotFound> { groups.delete(g.id) }
        assertFailsWith<DomainException.NotFound> { groups.delete(404) }
    }

    @Test
    fun aGroupInAnyStateCanBeDeleted() {
        val pending = draft("alice").also { groups.moveToPending(it.id, emptyList()) }
        groups.delete(pending.id)
        assertNull(groups.find(pending.id))

        for (archive in listOf(false, true)) {
            val id = activeGroup()
            if (archive) groups.archive(id)
            groups.delete(id)
            assertNull(groups.find(id))
        }
    }

    @Test
    fun impactCountsTheDrawWishlistItemsQuestionsAndAnswers() {
        val draft = draft("alice")
        assertEquals(DeletionImpact(0, 0, 0, draw = false), groups.impact(draft.id))
        assertTrue(groups.impact(draft.id).isEmpty)

        val id = activeGroup()
        assertEquals(DeletionImpact(0, 0, 0, draw = true), groups.impact(id))
        assertEquals(false, groups.impact(id).isEmpty)

        wishlists.add(id, u("alice"), WishlistEntry.of("Socks", null, null), 1, 50)
        wishlists.add(id, u("alice"), WishlistEntry.of("Tea", null, null), 2, 50)
        wishlists.add(id, u("bob"), WishlistEntry.of("Mug", null, null), 3, 50)
        questions.ask(id, u("alice"), "Colour?", 4, 30)
        questions.ask(id, u("alice"), "Size?", 5, 30)
        questions.ask(id, u("bob"), "Allergies?", 6, 30)
        questions.answer(id, u("bob"), 1, "Green", 7)

        assertEquals(DeletionImpact(wishlistItems = 3, questions = 3, answers = 1, draw = true), groups.impact(id))
        assertFailsWith<DomainException.NotFound> { groups.impact(404) }
    }

    @Test
    fun aPendingGroupWithOnlyWishlistItemsHasAnImpact() {
        users.insertAll(listOf(newUser("alice")))
        val id = groups.create(owner, GroupDetails("Party"), listOf(u("alice"))).id
        groups.moveToPending(id, emptyList())
        assertTrue(groups.impact(id).isEmpty)

        wishlists.add(id, u("alice"), WishlistEntry.of("Socks", null, null), 1, 50)

        assertEquals(DeletionImpact(1, 0, 0, draw = false), groups.impact(id))
    }

    @Test
    fun deletingAGroupRemovesItsDrawWishlistItemsQuestionsAndAnswersAndNoOthers() {
        val doomed = activeGroup()
        val kept = activeGroup()
        for (id in listOf(doomed, kept)) {
            wishlists.add(id, u("alice"), WishlistEntry.of("Socks", null, null), 1, 50)
            questions.ask(id, u("alice"), "Colour?", 2, 30)
            questions.answer(id, u("bob"), 1, "Green", 3)
        }

        groups.delete(doomed)

        for (table in listOf("group_participants", "assignments", "wishlist_items", "questions")) {
            assertEquals(0, count(table, "group_id = $doomed"), table)
            assertTrue(count(table, "group_id = $kept") > 0, "$table of the other group")
        }
        assertEquals(1, count("questions", "group_id = $kept AND answer IS NOT NULL"))
    }

    // --- participants ---

    @Test
    fun participantsAreReplacedWholesaleKeepingTheNewOrder() {
        val g = draft("old-one", "old-two")

        val updated = groups.replaceParticipants(g.id, listOf(u("zed"), u("amy")), emptyList())

        assertEquals(listOf("zed", "amy"), updated.participants.map { it.value })
        assertEquals(listOf("zed", "amy"), groups.find(g.id)!!.participants.map { it.value })
    }

    @Test
    fun newUsersAreInsertedTogetherWithTheParticipantChange() {
        val g = draft().also { groups.moveToPending(it.id, emptyList()) }

        groups.replaceParticipants(g.id, listOf(u("dave")), listOf(newUser("dave")))

        assertNotNull(users.find(u("dave")))
        assertEquals(listOf("dave"), groups.find(g.id)!!.participants.map { it.value })
    }

    @Test
    fun aFailingUserInsertRollsBackTheParticipantChange() {
        users.insertAll(listOf(newUser("alice")))
        val g = draft("keep-me").also { groups.moveToPending(it.id, emptyList()) }

        assertFailsWith<SQLException> {
            groups.replaceParticipants(g.id, listOf(u("dave"), u("alice")), listOf(newUser("dave"), newUser("alice")))
        }

        assertEquals(listOf("keep-me"), groups.find(g.id)!!.participants.map { it.value })
        assertNull(users.find(u("dave")))
    }

    @Test
    fun theRosterIsFrozenOnceActive() {
        val g = draft("a-one", "b-two", "c-three")
        groups.moveToPending(g.id, emptyList())
        groups.activate(g.id, ring("a-one", "b-two", "c-three"))

        assertFailsWith<DomainException.InvalidState> { groups.replaceParticipants(g.id, listOf(u("a-one")), emptyList()) }

        assertEquals(3, groups.find(g.id)!!.participants.size)
    }

    // --- transitions ---

    @Test
    fun movingToPendingChangesStateAndCreatesTheUsersTogether() {
        val g = draft("alice", "bob-b")

        val pending = groups.moveToPending(g.id, listOf(newUser("alice"), newUser("bob-b")))

        assertEquals(GroupState.PENDING, pending.state)
        assertEquals(setOf(u("alice"), u("bob-b")), users.existing(listOf(u("alice"), u("bob-b"))))
    }

    @Test
    fun aFailingUserInsertLeavesTheGroupInDraftAndCreatesNoUsers() {
        users.insertAll(listOf(newUser("alice")))
        val g = draft("dave", "alice")

        // "alice" already has an account, so inserting her again fails after "dave" was inserted
        assertFailsWith<SQLException> { groups.moveToPending(g.id, listOf(newUser("dave"), newUser("alice"))) }

        assertEquals(GroupState.DRAFT, groups.find(g.id)!!.state)
        assertNull(users.find(u("dave")))
    }

    @Test
    fun aRepeatedMoveIsRejectedAndChangesNothing() {
        val g = draft("alice")
        groups.moveToPending(g.id, listOf(newUser("alice")))

        assertFailsWith<DomainException.InvalidState> { groups.moveToPending(g.id, listOf(newUser("bob-b"))) }

        assertEquals(GroupState.PENDING, groups.find(g.id)!!.state)
        assertNull(users.find(u("bob-b")))
    }

    @Test
    fun movesFromTheWrongStateAreRejectedAndMissingGroupsAreNotFound() {
        val g = draft("a-one", "b-two", "c-three")

        assertFailsWith<DomainException.InvalidState> { groups.activate(g.id, ring("a-one", "b-two", "c-three")) }
        assertFailsWith<DomainException.InvalidState> { groups.archive(g.id) }
        assertEquals(GroupState.DRAFT, groups.find(g.id)!!.state)
        assertFailsWith<DomainException.NotFound> { groups.moveToPending(404, emptyList()) }
        assertFailsWith<DomainException.NotFound> { groups.archive(404) }
    }

    @Test
    fun activationStoresTheAssignmentsInDrawOrder() {
        val g = draft("a-one", "b-two", "c-three", "d-four").also { groups.moveToPending(it.id, emptyList()) }
        val draw = ring("c-three", "a-one", "d-four", "b-two")

        val active = groups.activate(g.id, draw)

        assertEquals(GroupState.ACTIVE, active.state)
        assertEquals(draw, groups.assignments(g.id))
    }

    @Test
    fun activationWithAnAssignmentForTheWrongPeopleIsRejectedAndLeavesNoTrace() {
        val g = draft("a-one", "b-two", "c-three").also { groups.moveToPending(it.id, emptyList()) }

        assertFailsWith<DomainException.InvalidState> { groups.activate(g.id, ring("a-one", "b-two")) }
        assertFailsWith<DomainException.InvalidState> { groups.activate(g.id, ring("a-one", "b-two", "c-three", "d-four")) }

        assertEquals(GroupState.PENDING, groups.find(g.id)!!.state)
        assertEquals(emptyList(), groups.assignments(g.id))
    }

    @Test
    fun aSelfAssignmentIsRefusedByTheDatabaseAndRollsBackTheActivation() {
        val g = draft("a-one", "b-two", "c-three").also { groups.moveToPending(it.id, emptyList()) }
        val broken = listOf(Assignment(u("a-one"), u("a-one")), Assignment(u("b-two"), u("c-three")), Assignment(u("c-three"), u("b-two")))

        assertFailsWith<SQLException> { groups.activate(g.id, broken) }

        assertEquals(GroupState.PENDING, groups.find(g.id)!!.state)
        assertEquals(0, count("assignments"))
    }

    @Test
    fun archivingKeepsParticipantsAndAssignments() {
        val g = draft("a-one", "b-two", "c-three").also { groups.moveToPending(it.id, emptyList()) }
        val draw = ring("a-one", "b-two", "c-three")
        groups.activate(g.id, draw)

        val archived = groups.archive(g.id)

        assertEquals(GroupState.ARCHIVED, archived.state)
        assertEquals(draw, groups.assignments(g.id))
        assertEquals(3, archived.participants.size)
        assertFailsWith<DomainException.InvalidState> { groups.archive(g.id) }
    }

    // --- who gives to whom ---

    @Test
    fun receiverAndSantaLookupsFollowTheDraw() {
        val g = draft("a-one", "b-two", "c-three", "d-four").also { groups.moveToPending(it.id, emptyList()) }
        groups.activate(g.id, ring("c-three", "a-one", "d-four", "b-two"))

        assertEquals(u("a-one"), groups.receiverOf(g.id, u("c-three")))
        assertEquals(u("c-three"), groups.receiverOf(g.id, u("b-two")))
        assertEquals(u("c-three"), groups.santaOf(g.id, u("a-one")))
        assertEquals(u("b-two"), groups.santaOf(g.id, u("c-three")))
    }

    @Test
    fun lookupsAreNullWithoutAnAssignment() {
        val g = draft("a-one", "b-two", "c-three")

        assertNull(groups.receiverOf(g.id, u("a-one")))
        assertNull(groups.santaOf(g.id, u("a-one")))
        assertNull(groups.receiverOf(404, u("a-one")))
        assertNull(groups.santaOf(g.id, u("not-a-member")))
    }

    @Test
    fun lookupsAreScopedToTheirGroup() {
        val first = draft("a-one", "b-two", "c-three", "d-four", name = "First").also { groups.moveToPending(it.id, emptyList()) }
        val second = draft("a-one", "b-two", "c-three", "d-four", name = "Second").also { groups.moveToPending(it.id, emptyList()) }
        groups.activate(first.id, ring("a-one", "b-two", "c-three", "d-four"))
        groups.activate(second.id, ring("d-four", "c-three", "b-two", "a-one"))

        assertEquals(u("b-two"), groups.receiverOf(first.id, u("a-one")))
        assertEquals(u("d-four"), groups.receiverOf(second.id, u("a-one")))
    }

    // --- concurrency ---

    @Test
    fun twoSimultaneousActivationsSucceedExactlyOnce() {
        val g = draft("a-one", "b-two", "c-three", "d-four").also { groups.moveToPending(it.id, emptyList()) }
        val draw = ring("a-one", "b-two", "c-three", "d-four")
        val pool = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)

        val attempts = List(2) {
            pool.submit<Result<Unit>> {
                start.await()
                runCatching { groups.activate(g.id, draw); Unit }
            }
        }
        start.countDown()
        val results = attempts.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(1, results.count { it.isSuccess }, "results: $results")
        assertTrue(results.single { it.isFailure }.exceptionOrNull() is DomainException.InvalidState)
        assertEquals(4, count("assignments", "group_id = ${g.id}"))
        assertEquals(GroupState.ACTIVE, groups.find(g.id)!!.state)
    }
}
