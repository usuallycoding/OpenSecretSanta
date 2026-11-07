package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.HandOverAdminRequest
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.domain.user.Username
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HandOverAdminRoutesTest {
    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp() = application { module(fx.config) }

    private suspend fun HttpClient.handOver(to: String, password: String?, token: String? = fx.token("boss")): HttpResponse =
        post(ApiRoutes.adminUserHandOverAdmin(to)) {
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(HandOverAdminRequest(password ?: "")))
        }

    private suspend fun HttpClient.getAs(path: String, token: String?): HttpResponse =
        get(path) { if (token != null) header(HttpHeaders.Authorization, "Bearer $token") }

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    /** boss owns a group of each state; alice, carol and stranger have their own passwords, bobby a temporary one. */
    private fun seedWorld() {
        fx.addUser("boss", password = "boss-password", admin = true)
        fx.addUser("alice")
        fx.addUser("bobby", password = "TempPass0002", mustChange = true, tempPlain = "TempPass0002")
        fx.addUser("carol")
        fx.addUser("stranger")
        fx.addGroup("boss", "alice", "bobby", "carol", "boss", state = "draft", name = "D")
        fx.addGroup("boss", "alice", "bobby", "carol", "boss", state = "pending", name = "P")
        fx.addGroup("boss", "alice", "bobby", "carol", "boss", state = "active", name = "A")
        fx.addGroup("boss", "alice", "bobby", "carol", "boss", state = "archived", name = "R")
        fx.sql("UPDATE users SET created_by = 'boss' WHERE username IN ('alice', 'carol', 'stranger')")
    }

    private fun scalar(query: String): String = fx.database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery(query).use { it.next(); it.getString(1) } }
    }

    private fun isAdmin(name: String) = scalar("SELECT is_admin FROM users WHERE username = '$name'") == "1"

    @Test
    fun theRoleMovesAndEveryGroupAndCreatedAccountWithIt() = testApplication {
        startApp()
        seedWorld()

        val response = client.handOver("alice", "boss-password")

        assertEquals(HttpStatusCode.NoContent, response.status, response.bodyAsText())
        assertTrue(isAdmin("alice"))
        assertFalse(isAdmin("boss"))
        assertEquals("1", scalar("SELECT COUNT(*) FROM users WHERE is_admin = 1"))
        assertEquals("4", scalar("SELECT COUNT(*) FROM groups WHERE owner_username = 'alice'"))
        assertEquals("0", scalar("SELECT COUNT(*) FROM groups WHERE owner_username = 'boss'"))
        assertEquals("alice", scalar("SELECT created_by FROM users WHERE username = 'carol'"))
        assertEquals("alice", scalar("SELECT created_by FROM users WHERE username = 'stranger'"))
        assertNull(fx.database.connection().use { c -> c.createStatement().use { s -> s.executeQuery("SELECT created_by FROM users WHERE username = 'alice'").use { it.next(); it.getString(1) } } })
        assertEquals("alice", scalar("SELECT created_by FROM users WHERE username = 'boss'"))
    }

    @Test
    fun theOldAdministratorIsRefusedAtOnceAndTheNewOneAdmittedWithTheTokensTheyHad() = testApplication {
        startApp()
        seedWorld()
        val bossToken = fx.token("boss")
        val aliceToken = fx.token("alice")

        client.handOver("alice", "boss-password")

        assertEquals(HttpStatusCode.Forbidden, client.getAs(ApiRoutes.ADMIN_USERS, bossToken).status)
        assertEquals(HttpStatusCode.OK, client.getAs(ApiRoutes.ADMIN_GROUPS, aliceToken).status)
        val managed = client.getAs(ApiRoutes.ADMIN_USERS, aliceToken).bodyAsText()
        assertTrue("boss" in managed && "carol" in managed, managed)
    }

    @Test
    fun theOldAdministratorStaysAParticipantAndNothingElseChanges() = testApplication {
        startApp()
        seedWorld()
        val before = scalar("SELECT group_concat(username || ':' || group_id, ',') FROM group_participants ORDER BY group_id, username")
        val hashes = scalar("SELECT group_concat(password_hash, ',') FROM users ORDER BY username")

        client.handOver("alice", "boss-password")

        assertEquals(before, scalar("SELECT group_concat(username || ':' || group_id, ',') FROM group_participants ORDER BY group_id, username"))
        assertEquals(hashes, scalar("SELECT group_concat(password_hash, ',') FROM users ORDER BY username"))
        val mine = client.getAs(ApiRoutes.GROUPS, fx.token("boss")).bodyAsText()
        assertTrue("\"P\"" in mine || "P" in mine, mine)
        assertEquals("0", scalar("SELECT COUNT(*) FROM assignments"))
    }

    @Test
    fun aWrongPasswordIsRefusedNamingThePasswordAndNothingMoves() = testApplication {
        startApp()
        seedWorld()

        val response = client.handOver("alice", "nope")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("password", error(response.bodyAsText()).field)
        assertTrue(isAdmin("boss"))
        assertFalse(isAdmin("alice"))
        assertEquals("4", scalar("SELECT COUNT(*) FROM groups WHERE owner_username = 'boss'"))
    }

    @Test
    fun tooManyWrongPasswordsLockWithRetryAfterAndARightOneClearsTheCount() = testApplication {
        startApp()
        seedWorld()
        repeat(5) { assertEquals(HttpStatusCode.BadRequest, client.handOver("alice", "nope").status) }

        val locked = client.handOver("alice", "boss-password")

        assertEquals(HttpStatusCode.TooManyRequests, locked.status)
        assertEquals("too_many_attempts", error(locked.bodyAsText()).code)
        assertTrue(locked.headers[HttpHeaders.RetryAfter]!!.toLong() > 0)
        assertFalse(isAdmin("alice"))
    }

    @Test
    fun aSuccessfulHandoverClearsTheFailedAttempts() = testApplication {
        startApp()
        seedWorld()
        repeat(2) { client.handOver("alice", "nope") }

        client.handOver("alice", "boss-password")

        assertEquals("0", scalar("SELECT COUNT(*) FROM login_attempts WHERE username = 'boss'"))
    }

    @Test
    fun refusalsForWhoCanTakeOver() = testApplication {
        startApp()
        seedWorld()

        assertEquals(HttpStatusCode.Forbidden, client.handOver("carol", "password1", token = fx.token("alice")).status)
        assertEquals(HttpStatusCode.Unauthorized, client.handOver("alice", "boss-password", token = null).status)
        val unmanaged = fx.addUser("loner")
        assertEquals(HttpStatusCode.NotFound, client.handOver(unmanaged.value, "boss-password").status)
        assertEquals(HttpStatusCode.NotFound, client.handOver("nobody-here", "boss-password").status)
        for (target in listOf("boss", "bobby")) {
            val refused = client.handOver(target, "boss-password")
            assertEquals(HttpStatusCode.Conflict, refused.status, target)
            assertEquals("invalid_state", error(refused.bodyAsText()).code)
        }
        assertTrue(isAdmin("boss"))
        assertEquals("1", scalar("SELECT COUNT(*) FROM users WHERE is_admin = 1"))
    }

    @Test
    fun someoneAlreadyAnAdministratorCannotBeChosen() = testApplication {
        startApp()
        seedWorld()
        fx.addUser("rival", admin = true)
        fx.sql("INSERT INTO group_participants (group_id, username, position) VALUES (2, 'rival', 9)")

        assertEquals(HttpStatusCode.Conflict, client.handOver("rival", "boss-password").status)
        assertTrue(isAdmin("boss"))
    }

    @Test
    fun aPersonWhoChangedTheirPasswordCanTakeOver() = testApplication {
        startApp()
        seedWorld()
        fx.sql("UPDATE users SET must_change_password = 0, temp_password_enc = NULL WHERE username = 'bobby'")

        assertEquals(HttpStatusCode.NoContent, client.handOver("bobby", "boss-password").status)
        assertTrue(isAdmin("bobby"))
    }

    @Test
    fun theRepositoryChecksAgainAndMovesNothingWhenTheWorldChanged() {
        seedWorld()
        // the person has a temporary password again by the time the move is made
        assertFalse(fx.users.handOverAdmin(Username.parse("boss"), Username.parse("bobby")))
        // the caller is no longer an administrator
        assertFalse(fx.users.handOverAdmin(Username.parse("alice"), Username.parse("carol")))
        assertTrue(isAdmin("boss"))
        assertEquals("4", scalar("SELECT COUNT(*) FROM groups WHERE owner_username = 'boss'"))
        assertEquals("boss", scalar("SELECT created_by FROM users WHERE username = 'carol'"))
        assertEquals("0", scalar("SELECT COUNT(*) FROM users WHERE is_admin = 1 AND username <> 'boss'"))
    }

    @Test
    fun twoHandoversAtOnceSucceedAtMostOnce() {
        seedWorld()
        val start = CountDownLatch(1)
        val results = java.util.Collections.synchronizedList(mutableListOf<Boolean>())
        val threads = listOf("alice", "carol").map { target ->
            thread {
                start.await()
                results += fx.users.handOverAdmin(Username.parse("boss"), Username.parse(target))
            }
        }
        start.countDown()
        threads.forEach { it.join() }

        assertEquals(1, results.count { it })
        assertEquals("1", scalar("SELECT COUNT(*) FROM users WHERE is_admin = 1"))
        assertFalse(isAdmin("boss"))
    }

    @Test
    fun theStartUpSettingsDoNotBringTheOldAdministratorBack() {
        seedWorld()
        fx.users.handOverAdmin(Username.parse("boss"), Username.parse("alice"))

        testApplication {
            application { module(testConfig(dbPath = java.nio.file.Path.of(fx.config.dbPath), adminUsername = "boss", adminPassword = "another-password-1")) }
            client.get(ApiRoutes.HEALTH)
        }

        assertFalse(isAdmin("boss"))
        assertTrue(isAdmin("alice"))
        assertEquals("5", scalar("SELECT COUNT(*) FROM users"))
    }
}
