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
import com.usuallycoding.opensecretsanta.server.data.group.SqliteQuestionRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SqliteQuestionRepositoryTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val groups = SqliteGroupRepository(database)
    private val questions = SqliteQuestionRepository(database)

    private fun u(name: String) = Username.parse(name)

    private val names = listOf("alice", "bob", "carol", "dave")

    init {
        SqliteUserRepository(database).insertAll((names + "boss").map { NewUser(u(it), "h", null, false, it == "boss") })
    }

    /** alice -> bob -> carol -> dave -> alice, in the given state. */
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
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM questions WHERE $where").use { it.next(); it.getInt(1) } }
    }

    // --- asking ---

    @Test
    fun questionsAreNumberedWithoutGapsPerSanta() {
        val id = group()

        val numbers = listOf(
            questions.ask(id, u("alice"), "a1", 100, 30).number,
            questions.ask(id, u("bob"), "b1", 101, 30).number,
            questions.ask(id, u("alice"), "a2", 102, 30).number,
            questions.ask(id, u("alice"), "a3", 103, 30).number,
            questions.ask(id, u("bob"), "b2", 104, 30).number,
        )

        assertEquals(listOf(1, 1, 2, 3, 2), numbers)
        assertEquals(listOf("a1", "a2", "a3"), questions.askedBy(id, u("alice")).map { it.text })
    }

    @Test
    fun theSameSantaInAnotherGroupStartsAgainAtOne() {
        val first = group()
        val second = group()

        questions.ask(first, u("alice"), "x", 1, 30)
        questions.ask(first, u("alice"), "y", 2, 30)

        assertEquals(1, questions.ask(second, u("alice"), "z", 3, 30).number)
    }

    @Test
    fun theCapIsEnforcedPerSantaAndLeavesNothingBehind() {
        val id = group()
        repeat(3) { questions.ask(id, u("alice"), "q$it", it.toLong(), 3) }

        assertFailsWith<DomainException.InvalidState> { questions.ask(id, u("alice"), "one too many", 9, 3) }

        assertEquals(3, count("santa = 'alice'"))
        assertEquals(1, questions.ask(id, u("bob"), "bob has his own allowance", 10, 3).number)
    }

    @Test
    fun theStoredQuestionCarriesTheTimeItWasAsked() {
        val id = group()

        val asked = questions.ask(id, u("alice"), "What size?", 123_456, 30)

        assertEquals(AskedShape("What size?", 123_456, null, null, null), asked.shape())
        assertEquals(asked, questions.askedBy(id, u("alice")).single())
    }

    @Test
    fun askingNeedsAnActiveGroupAnAssignmentAndAnExistingGroup() {
        val pending = group(activate = false)
        val archived = group(archive = true)
        val active = group()

        assertFailsWith<DomainException.InvalidState> { questions.ask(pending, u("alice"), "too early", 1, 30) }
        assertFailsWith<DomainException.InvalidState> { questions.ask(archived, u("alice"), "too late", 1, 30) }
        assertFailsWith<DomainException.NotFound> { questions.ask(active, u("boss"), "owner is not a participant", 1, 30) }
        assertFailsWith<DomainException.NotFound> { questions.ask(9999, u("alice"), "no group", 1, 30) }
        assertEquals(0, count())
    }

    // --- answering ---

    @Test
    fun anAnswerReachesOnlyTheQuestionAddressedToTheCaller() {
        val id = group()
        questions.ask(id, u("alice"), "alice asks bob", 1, 30)
        questions.ask(id, u("bob"), "bob asks carol", 2, 30)

        val answered = questions.answer(id, u("carol"), 1, "carol answers bob", 50)

        assertEquals("bob asks carol", answered.text)
        assertEquals("carol answers bob", answered.answer)
        assertNull(questions.askedBy(id, u("alice")).single().answer)
        assertEquals("carol answers bob", questions.askedBy(id, u("bob")).single().answer)
    }

    @Test
    fun theFirstAnswerSetsBothTimesAndAnEditOnlyMovesTheLastChangedTime() {
        val id = group()
        questions.ask(id, u("alice"), "Colour?", 1, 30)

        val first = questions.answer(id, u("bob"), 1, "Green", 1_000)
        val edited = questions.answer(id, u("bob"), 1, "Blue", 9_000)

        assertEquals(1_000L, first.answeredAtMillis)
        assertEquals(1_000L, first.answerUpdatedAtMillis)
        assertEquals("Blue", edited.answer)
        assertEquals(1_000L, edited.answeredAtMillis)
        assertEquals(9_000L, edited.answerUpdatedAtMillis)
        assertEquals(edited.answer, questions.askedBy(id, u("alice")).single().answer)
    }

    @Test
    fun unknownQuestionsAndUnassignedCallersAreNotFound() {
        val id = group()
        questions.ask(id, u("alice"), "Colour?", 1, 30)

        assertFailsWith<DomainException.NotFound> { questions.answer(id, u("bob"), 2, "x", 5) }
        assertFailsWith<DomainException.NotFound> { questions.answer(id, u("boss"), 1, "x", 5) }
        assertFailsWith<DomainException.NotFound> { questions.answer(9999, u("bob"), 1, "x", 5) }
        assertNull(questions.askedBy(id, u("alice")).single().answer)
    }

    @Test
    fun answersNeedAnActiveGroup() {
        val id = group()
        questions.ask(id, u("alice"), "Colour?", 1, 30)
        groups.archive(id)

        assertFailsWith<DomainException.InvalidState> { questions.answer(id, u("bob"), 1, "Green", 5) }

        assertNull(questions.askedBy(id, u("alice")).single().answer)
    }

    // --- reading ---

    @Test
    fun theReceiverSeesOnlyTheQuestionsOfTheirOwnSanta() {
        val id = group()
        questions.ask(id, u("alice"), "for bob 1", 1, 30)
        questions.ask(id, u("alice"), "for bob 2", 2, 30)
        questions.ask(id, u("bob"), "for carol", 3, 30)

        assertEquals(listOf("for bob 1", "for bob 2"), questions.askedTo(id, u("bob")).map { it.text })
        assertEquals(listOf("for carol"), questions.askedTo(id, u("carol")).map { it.text })
        assertEquals(emptyList(), questions.askedTo(id, u("dave")))
        assertEquals(emptyList(), questions.askedTo(id, u("boss")))
    }

    @Test
    fun theThreadStaysReadableAfterArchive() {
        val id = group()
        questions.ask(id, u("alice"), "Colour?", 1, 30)
        questions.answer(id, u("bob"), 1, "Green", 2)
        groups.archive(id)

        assertEquals("Green", questions.askedBy(id, u("alice")).single().answer)
        assertEquals("Green", questions.askedTo(id, u("bob")).single().answer)
    }

    // --- concurrency ---

    @Test
    fun simultaneousQuestionsFromOneSantaGetDistinctConsecutiveNumbers() {
        val id = group()
        val pool = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)

        val results = List(20) { index ->
            pool.submit<Int> {
                start.await()
                questions.ask(id, u("alice"), "q$index", index.toLong(), 30).number
            }
        }
        start.countDown()
        val numbers = results.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals((1..20).toList(), numbers.sorted())
        assertEquals(20, count("santa = 'alice'"))
    }

    @Test
    fun simultaneousQuestionsNeverExceedTheCap() {
        val id = group()
        val pool = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)

        val outcomes = List(12) { index ->
            pool.submit<Result<Int>> {
                start.await()
                runCatching { questions.ask(id, u("alice"), "q$index", index.toLong(), 5).number }
            }
        }
        start.countDown()
        val results = outcomes.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(5, results.count { it.isSuccess })
        assertEquals(7, results.count { it.exceptionOrNull() is DomainException.InvalidState })
        assertEquals(5, count("santa = 'alice'"))
    }

    private data class AskedShape(val text: String, val askedAt: Long, val answer: String?, val answeredAt: Long?, val updatedAt: Long?)

    private fun com.usuallycoding.opensecretsanta.domain.gift.AskedQuestion.shape() =
        AskedShape(text, askedAtMillis, answer, answeredAtMillis, answerUpdatedAtMillis)
}
