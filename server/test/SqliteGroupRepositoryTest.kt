package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
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

    private fun u(name: String) = Username.parse(name)

    private fun newUser(name: String) = NewUser(u(name), "hash-$name", byteArrayOf(1), mustChangePassword = true, isAdmin = false)

    private val owner = u("boss").also { users.insertAll(listOf(NewUser(it, "h", null, false, true))) }

    private fun draft(vararg participants: String, name: String = "Party") =
        groups.create(owner, GroupDetails(name), participants.map(::u))

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
    fun ownedGroupsAreListedInCreationOrderAndOnlyForTheirOwner() {
        users.insertAll(listOf(NewUser(u("other-admin"), "h", null, false, true)))
        val first = draft(name = "First")
        groups.create(u("other-admin"), GroupDetails("Not mine"), emptyList())
        val second = draft(name = "Second")

        assertEquals(listOf(first.id, second.id), groups.findOwnedBy(owner).map { it.id })
        assertEquals(1, groups.findOwnedBy(u("other-admin")).size)
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
    fun onlyADraftCanBeDeletedAndItsParticipantsGoWithIt() {
        val g = draft("alice", "bob-b")
        groups.deleteDraft(g.id)
        assertNull(groups.find(g.id))
        assertEquals(0, count("group_participants", "group_id = ${g.id}"))

        val pending = draft().also { groups.moveToPending(it.id, emptyList()) }
        assertFailsWith<DomainException.InvalidState> { groups.deleteDraft(pending.id) }
        assertNotNull(groups.find(pending.id))
        assertFailsWith<DomainException.NotFound> { groups.deleteDraft(404) }
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
