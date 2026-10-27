package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistEntry
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteWishlistRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import java.sql.SQLException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SqliteWishlistRepositoryTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val groups = SqliteGroupRepository(database)
    private val wishlists = SqliteWishlistRepository(database)

    private fun u(name: String) = Username.parse(name)

    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        SqliteUserRepository(database).insertAll((names + "boss").map { NewUser(u(it), "h", null, false, it == "boss") })
    }

    private fun group(state: String = "active"): Long {
        val g = groups.create(u("boss"), GroupDetails("Party"), names.map(::u))
        if (state != "draft") groups.moveToPending(g.id, emptyList())
        if (state == "active" || state == "archived") {
            groups.activate(g.id, names.indices.map { Assignment(u(names[it]), u(names[(it + 1) % names.size])) })
        }
        if (state == "archived") groups.archive(g.id)
        return g.id
    }

    private fun entry(title: String, note: String? = null, url: String? = null) = WishlistEntry.of(title, note, url)

    private fun add(group: Long, owner: String, title: String, at: Long = 100, max: Int = 50) =
        wishlists.add(group, u(owner), entry(title), at, max)

    private fun titles(group: Long, owner: String = "alice") = wishlists.list(group, u(owner)).map { it.title }

    private fun positions(group: Long, owner: String = "alice"): List<Int> = database.connection().use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT position FROM wishlist_items WHERE group_id = $group AND username = '$owner' ORDER BY position").use { rows ->
                generateSequence { if (rows.next()) rows.getInt(1) else null }.toList()
            }
        }
    }

    private fun sql(statement: String) = database.connection().use { c -> c.createStatement().use { it.execute(statement) } }

    // --- adding and reading ---

    @Test
    fun itemsAreAppendedAtTheEndAndReadBackInOrder() {
        val g = group()

        val first = add(g, "alice", "One", at = 111)
        add(g, "alice", "Two")
        add(g, "alice", "Three")

        assertEquals(listOf("One", "Two", "Three"), titles(g))
        assertEquals(listOf(0, 1, 2), positions(g))
        assertEquals(111L, first.createdAtMillis)
        assertEquals(111L, first.updatedAtMillis)
    }

    @Test
    fun anItemStoresItsNoteAndLink() {
        val g = group()

        val item = wishlists.add(g, u("alice"), entry("Socks", "Grey", "https://shop.example/socks"), 1, 50)

        assertEquals(item, wishlists.list(g, u("alice")).single())
        assertEquals("Grey", item.note)
        assertEquals("https://shop.example/socks", item.url)
    }

    @Test
    fun listsAreSeparatePerOwnerAndPerGroup() {
        val first = group()
        val second = group()

        add(first, "alice", "Alice in first")
        add(first, "bob", "Bob in first")
        add(second, "alice", "Alice in second")

        assertEquals(listOf("Alice in first"), titles(first, "alice"))
        assertEquals(listOf("Bob in first"), titles(first, "bob"))
        assertEquals(listOf("Alice in second"), titles(second, "alice"))
        assertEquals(listOf(0), positions(second, "alice"))
    }

    @Test
    fun afterADeleteTheNextItemTakesTheNextFreePositionAndOrderHolds() {
        val g = group()
        add(g, "alice", "One")
        val middle = add(g, "alice", "Two")
        add(g, "alice", "Three")

        wishlists.delete(g, u("alice"), middle.id)
        add(g, "alice", "Four")

        assertEquals(listOf("One", "Three", "Four"), titles(g))
        assertEquals(listOf(0, 2, 3), positions(g)) // a harmless gap where "Two" was
    }

    // --- cap ---

    @Test
    fun theCapStopsTheNextItemAndLeavesTheListAlone() {
        val g = group()
        repeat(3) { add(g, "alice", "Item $it", max = 3) }

        assertFailsWith<DomainException.InvalidState> { add(g, "alice", "One too many", max = 3) }

        assertEquals(3, titles(g).size)
        add(g, "bob", "Bob has his own allowance", max = 3)
    }

    @Test
    fun theCapHoldsUnderConcurrentAdds() {
        val g = group()
        val pool = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)

        val outcomes = List(12) { index ->
            pool.submit<Result<Long>> {
                start.await()
                runCatching { wishlists.add(g, u("alice"), entry("Item $index"), index.toLong(), 5).id }
            }
        }
        start.countDown()
        val results = outcomes.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(5, results.count { it.isSuccess })
        assertEquals(7, results.count { it.exceptionOrNull() is DomainException.InvalidState })
        assertEquals(listOf(0, 1, 2, 3, 4), positions(g))
    }

    @Test
    fun concurrentAddsGetDistinctConsecutivePositions() {
        val g = group()
        val pool = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)

        val ids = List(20) { index ->
            pool.submit<Long> {
                start.await()
                wishlists.add(g, u("alice"), entry("Item $index"), index.toLong(), 50).id
            }
        }
        start.countDown()
        ids.forEach { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals((0..19).toList(), positions(g))
        assertEquals(20, titles(g).distinct().size)
    }

    // --- editing and deleting ---

    @Test
    fun anEditKeepsThePlaceAndCreationTimeAndMovesTheLastChangedTime() {
        val g = group()
        add(g, "alice", "One")
        val second = add(g, "alice", "Two", at = 100)
        add(g, "alice", "Three")

        val edited = wishlists.update(g, u("alice"), second.id, entry("Two, better", "note", "https://shop.example"), 900)

        assertEquals(listOf("One", "Two, better", "Three"), titles(g))
        assertEquals(100L, edited.createdAtMillis)
        assertEquals(900L, edited.updatedAtMillis)
        assertEquals("note", edited.note)
    }

    @Test
    fun updateAndDeleteAreScopedToTheOwnerAndTheGroup() {
        val first = group()
        val second = group()
        val alices = add(first, "alice", "Alice's")
        val bobs = add(first, "bob", "Bob's")

        assertFailsWith<DomainException.NotFound> { wishlists.update(first, u("alice"), bobs.id, entry("Hijack"), 1) }
        assertFailsWith<DomainException.NotFound> { wishlists.delete(first, u("alice"), bobs.id) }
        assertFailsWith<DomainException.NotFound> { wishlists.update(second, u("alice"), alices.id, entry("Wrong group"), 1) }
        assertFailsWith<DomainException.NotFound> { wishlists.delete(second, u("alice"), alices.id) }
        assertFailsWith<DomainException.NotFound> { wishlists.delete(first, u("alice"), 99999) }

        assertEquals(listOf("Alice's"), titles(first, "alice"))
        assertEquals(listOf("Bob's"), titles(first, "bob"))
    }

    // --- reordering ---

    @Test
    fun reorderingRenumbersFromZeroInTheNewOrder() {
        val g = group()
        add(g, "alice", "A")
        val b = add(g, "alice", "B")
        val c = add(g, "alice", "C")
        wishlists.delete(g, u("alice"), b.id) // leaves a gap first
        val a = wishlists.list(g, u("alice")).first()

        val result = wishlists.reorder(g, u("alice"), listOf(c.id, a.id))

        assertEquals(listOf("C", "A"), result.map { it.title })
        assertEquals(listOf(0, 1), positions(g))
    }

    @Test
    fun reorderingAFullListNeverBreaksUniqueness() {
        val g = group()
        val ids = (1..50).map { add(g, "alice", "Item $it").id }
        val shuffled = ids.shuffled(java.util.Random(42))

        val result = wishlists.reorder(g, u("alice"), shuffled)

        assertEquals(shuffled, result.map { it.id })
        assertEquals((0..49).toList(), positions(g))
        assertEquals(ids, wishlists.reorder(g, u("alice"), ids).map { it.id })
    }

    @Test
    fun aWrongListOfIdsChangesNothing() {
        val g = group()
        val a = add(g, "alice", "A")
        val b = add(g, "alice", "B")
        val bobs = add(g, "bob", "Bob's")

        for (ids in listOf(listOf(a.id), listOf(a.id, a.id), listOf(a.id, b.id, bobs.id), listOf(a.id, b.id, 99999L), emptyList())) {
            assertEquals("itemIds", assertFailsWith<DomainException.ValidationFailed>(ids.toString()) { wishlists.reorder(g, u("alice"), ids) }.field)
        }
        assertEquals(listOf("A", "B"), titles(g))
        assertEquals(listOf(0, 1), positions(g))
    }

    @Test
    fun aReorderThatFailsPartWayIsRolledBackCompletely() {
        val g = group()
        val a = add(g, "alice", "A")
        val b = add(g, "alice", "B")
        val c = add(g, "alice", "C")
        // abort the update that writes position 1, which happens after the list was parked on negatives
        sql("CREATE TRIGGER fail_midway BEFORE UPDATE OF position ON wishlist_items WHEN NEW.position = 1 BEGIN SELECT RAISE(ABORT, 'boom'); END")

        assertFailsWith<SQLException> { wishlists.reorder(g, u("alice"), listOf(c.id, b.id, a.id)) }

        sql("DROP TRIGGER fail_midway")
        assertEquals(listOf("A", "B", "C"), titles(g))
        assertEquals(listOf(0, 1, 2), positions(g))
    }

    // --- states and participation ---

    @Test
    fun writesAreAllowedInPendingAndActive() {
        for (state in listOf("pending", "active")) {
            val g = group(state)
            val item = add(g, "alice", "One")
            wishlists.update(g, u("alice"), item.id, entry("Edited"), 5)
            wishlists.reorder(g, u("alice"), listOf(item.id))
            wishlists.delete(g, u("alice"), item.id)
            assertEquals(emptyList(), titles(g), state)
        }
    }

    @Test
    fun writesAreRefusedInArchivedAndDraftGroupsButReadingStillWorks() {
        val archived = group("archived")
        val draft = group("draft")

        assertFailsWith<DomainException.InvalidState> { add(archived, "alice", "x") }
        assertFailsWith<DomainException.InvalidState> { add(draft, "alice", "x") }
        assertEquals(emptyList(), titles(archived))
        assertEquals(emptyList(), titles(draft))
    }

    @Test
    fun anArchivedListKeepsWhatWasThereAndRefusesChanges() {
        val g = group()
        val item = add(g, "alice", "Kept")
        groups.archive(g)

        assertFailsWith<DomainException.InvalidState> { wishlists.update(g, u("alice"), item.id, entry("Changed"), 5) }
        assertFailsWith<DomainException.InvalidState> { wishlists.delete(g, u("alice"), item.id) }
        assertFailsWith<DomainException.InvalidState> { wishlists.reorder(g, u("alice"), listOf(item.id)) }
        assertEquals(listOf("Kept"), titles(g))
    }

    @Test
    fun anUnknownGroupOrANonParticipantIsNotFound() {
        val g = group()

        assertFailsWith<DomainException.NotFound> { add(9999, "alice", "x") }
        assertFailsWith<DomainException.NotFound> { add(g, "boss", "owner does not participate") }
    }

    @Test
    fun aRemovedParticipantCannotWriteButTheirItemsReturnWhenTheyAreAddedBack() {
        val g = group("pending")
        add(g, "alice", "Kept while away")
        groups.replaceParticipants(g, listOf("bob", "carol", "dave").map(::u), emptyList())

        assertFailsWith<DomainException.NotFound> { add(g, "alice", "while removed") }
        assertEquals(listOf("Kept while away"), titles(g)) // the rows are still there, hidden by the use cases

        groups.replaceParticipants(g, names.map(::u), emptyList())
        add(g, "alice", "after returning")

        assertEquals(listOf("Kept while away", "after returning"), titles(g))
        assertEquals(listOf(0, 1), positions(g))
    }
}
