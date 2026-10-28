package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.auth.LoginAttempts
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.auth.SqliteLoginAttemptTracker
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupMembershipRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqliteRepositoriesTest {
    private val path = newTempDbPath()
    private val database = Database(path.toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val users = SqliteUserRepository(database)
    private val memberships = SqliteGroupMembershipRepository(database)

    private fun u(name: String) = Username.parse(name)

    private fun newUser(
        name: String,
        enc: ByteArray? = null,
        mustChange: Boolean = true,
        admin: Boolean = false,
    ) = NewUser(u(name), "hash-$name", enc, mustChange, admin)

    private fun sql(statement: String) = database.connection().use { c -> c.createStatement().use { it.execute(statement) } }

    private fun addGroup(owner: String, vararg participants: String, state: String = "pending"): Int {
        sql("INSERT INTO groups (owner_username, name, state) VALUES ('$owner', 'Group', '$state')")
        val id = database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT MAX(id) FROM groups").use { it.next(); it.getInt(1) } }
        }
        participants.forEachIndexed { index, username ->
            sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, '$username', $index)")
        }
        return id
    }

    // --- users ---

    @Test
    fun insertedUserCanBeReadBackWithDefaults() {
        val enc = byteArrayOf(1, 2, 3, 0, -1)
        users.insertAll(listOf(newUser("alice", enc = enc)))

        val alice = assertNotNull(users.find(u("alice")))
        assertEquals(Profile.EMPTY, alice.profile)
        assertTrue(alice.mustChangePassword)
        assertFalse(alice.isAdmin)
        assertEquals(1, alice.tokenVersion)
        val credentials = assertNotNull(users.credentials(u("alice")))
        assertEquals("hash-alice", credentials.passwordHash)
        assertContentEquals(enc, credentials.encryptedTempPassword)
        assertEquals(1, users.count())
    }

    @Test
    fun unknownUserIsNull() {
        assertNull(users.find(u("nobody")))
        assertNull(users.credentials(u("nobody")))
        assertNull(users.updateProfile(u("nobody"), Profile.EMPTY))
        assertNull(users.replaceCredentials(u("nobody"), "h", null, false))
    }

    @Test
    fun profileUpdateAndClearing() {
        users.insertAll(listOf(newUser("alice")))
        val profile = Profile("Alice B", "a@b.co", "123", "1 Main St", "Ring twice")

        assertEquals(profile, users.updateProfile(u("alice"), profile)!!.profile)
        assertEquals(profile, users.find(u("alice"))!!.profile)
        assertEquals(Profile.EMPTY, users.updateProfile(u("alice"), Profile.EMPTY)!!.profile)
    }

    @Test
    fun replacingCredentialsBumpsTokenVersionAndStoresState() {
        users.insertAll(listOf(newUser("alice", enc = byteArrayOf(9))))

        val updated = users.replaceCredentials(u("alice"), "new-hash", null, mustChangePassword = false)!!

        assertEquals(2, updated.tokenVersion)
        assertFalse(updated.mustChangePassword)
        val credentials = users.credentials(u("alice"))!!
        assertEquals("new-hash", credentials.passwordHash)
        assertNull(credentials.encryptedTempPassword)
        assertEquals(3, users.replaceCredentials(u("alice"), "x", byteArrayOf(1), true)!!.tokenVersion)
    }

    @Test
    fun usernamesAreUniqueIgnoringCase() {
        users.insertAll(listOf(newUser("alice")))
        assertFailsWith<SQLException> { users.insertAll(listOf(newUser("ALICE"))) }
        assertEquals(1, users.count())
    }

    @Test
    fun databaseRefusesNonNormalizedUsernames() {
        assertFailsWith<SQLException> { sql("INSERT INTO users (username, password_hash) VALUES ('Mixed', 'h')") }
    }

    @Test
    fun batchInsertIsAllOrNothing() {
        users.insertAll(listOf(newUser("alice")))

        assertFailsWith<SQLException> { users.insertAll(listOf(newUser("bob"), newUser("carol"), newUser("alice"))) }

        assertEquals(1, users.count())
        assertNull(users.find(u("bob")))
    }

    @Test
    fun findAllAndExistingReturnOnlyMatches() {
        users.insertAll(listOf(newUser("alice"), newUser("bob")))

        assertEquals(setOf("alice", "bob"), users.findAll(listOf(u("alice"), u("bob"), u("ghost"))).map { it.username.value }.toSet())
        assertEquals(setOf(u("alice")), users.existing(listOf(u("alice"), u("ghost"))))
        assertEquals(emptyList(), users.findAll(emptyList()))
        assertEquals(emptySet(), users.existing(emptyList()))
    }

    // --- group membership ---

    @Test
    fun membershipIsScopedToTheOwnersGroups() {
        users.insertAll(listOf("owner-a", "owner-b", "alice", "bob").map { newUser(it) })
        addGroup("owner-a", "alice")
        addGroup("owner-b", "bob")

        assertEquals(setOf(u("alice")), memberships.membersOfGroupsOwnedBy(u("owner-a")))
        assertEquals(setOf(u("bob")), memberships.membersOfGroupsOwnedBy(u("owner-b")))
        assertEquals(emptySet(), memberships.membersOfGroupsOwnedBy(u("alice")))
    }

    @Test
    fun userInSeveralGroupsIsVisibleToEachOwnerAndCountedOnce() {
        users.insertAll(listOf("owner-a", "owner-b", "alice").map { newUser(it) })
        addGroup("owner-a", "alice")
        addGroup("owner-a", "alice")
        addGroup("owner-b", "alice")

        assertEquals(setOf(u("alice")), memberships.membersOfGroupsOwnedBy(u("owner-a")))
        assertEquals(setOf(u("alice")), memberships.membersOfGroupsOwnedBy(u("owner-b")))
    }

    @Test
    fun deletingAGroupRemovesItsParticipants() {
        users.insertAll(listOf("owner-a", "alice").map { newUser(it) })
        val groupId = addGroup("owner-a", "alice")

        sql("DELETE FROM groups WHERE id = $groupId")

        assertEquals(emptySet(), memberships.membersOfGroupsOwnedBy(u("owner-a")))
        assertEquals(0, database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM group_participants").use { it.next(); it.getInt(1) } }
        })
    }

    @Test
    fun participantsOfDraftGroupsAreOutOfScope() {
        users.insertAll(listOf("owner-a", "alice", "bob").map { newUser(it) })
        addGroup("owner-a", "alice", state = "draft")
        addGroup("owner-a", "bob", state = "pending")

        assertEquals(setOf(u("bob")), memberships.membersOfGroupsOwnedBy(u("owner-a")))
    }

    @Test
    fun everyStateAfterDraftPlacesParticipantsInScope() {
        users.insertAll(listOf("owner-a", "p-one", "p-two", "p-three").map { newUser(it) })
        addGroup("owner-a", "p-one", state = "pending")
        addGroup("owner-a", "p-two", state = "active")
        addGroup("owner-a", "p-three", state = "archived")

        assertEquals(setOf(u("p-one"), u("p-two"), u("p-three")), memberships.membersOfGroupsOwnedBy(u("owner-a")))
    }

    @Test
    fun namesWithoutAnAccountAreNeverInScope() {
        users.insertAll(listOf("owner-a", "alice").map { newUser(it) })
        addGroup("owner-a", "alice", "not-registered-yet")

        assertEquals(setOf(u("alice")), memberships.membersOfGroupsOwnedBy(u("owner-a")))
    }

    @Test
    fun removingAParticipantEndsTheScope() {
        users.insertAll(listOf("owner-a", "alice").map { newUser(it) })
        val groupId = addGroup("owner-a", "alice")
        assertEquals(setOf(u("alice")), memberships.membersOfGroupsOwnedBy(u("owner-a")))

        sql("DELETE FROM group_participants WHERE group_id = $groupId AND username = 'alice'")

        assertEquals(emptySet(), memberships.membersOfGroupsOwnedBy(u("owner-a")))
    }

    @Test
    fun aGroupNeedsAnExistingOwner() {
        assertFailsWith<SQLException> { sql("INSERT INTO groups (owner_username, name) VALUES ('ghost', 'x')") }
    }

    // --- login attempts ---

    @Test
    fun loginAttemptsRoundTripAndClear() {
        val tracker = SqliteLoginAttemptTracker(database)
        assertNull(tracker.get(u("ghost-user")))

        tracker.save(u("ghost-user"), LoginAttempts(3, null))
        assertEquals(3, tracker.get(u("ghost-user"))!!.failures)
        assertNull(tracker.get(u("ghost-user"))!!.lockedUntilMillis)

        tracker.save(u("ghost-user"), LoginAttempts(5, 123_456_789_000L))
        assertEquals(123_456_789_000L, tracker.get(u("ghost-user"))!!.lockedUntilMillis)

        tracker.clear(u("ghost-user"))
        assertNull(tracker.get(u("ghost-user")))
    }

    @Test
    fun lockSurvivesANewRepositoryInstanceOnTheSameFile() {
        SqliteLoginAttemptTracker(database).save(u("alice"), LoginAttempts(5, 42L))

        val reopened = SqliteLoginAttemptTracker(Database(path.toString()))

        assertEquals(42L, reopened.get(u("alice"))!!.lockedUntilMillis)
    }
}
