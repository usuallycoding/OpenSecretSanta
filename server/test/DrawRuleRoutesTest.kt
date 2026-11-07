package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AddDrawRuleRequest
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.DrawRuleDto
import com.usuallycoding.opensecretsanta.contract.DrawRulesDto
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.contract.ParticipantsRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DrawRuleRoutesTest {
    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp() = application { module(fx.config) }

    private suspend inline fun <reified T> HttpClient.send(method: String, path: String, token: String?, body: T? = null): HttpResponse =
        request(path) {
            this.method = HttpMethod.parse(method)
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(body))
            }
        }

    private fun setup(vararg people: String) {
        fx.addUser("admin-a", admin = true)
        fx.addUser("admin-b", admin = true)
        fx.addUser("plain")
        people.forEach { fx.addUser(it) }
    }

    private val names = arrayOf("alice", "bobby", "carol", "daves", "erins")

    private suspend fun HttpClient.rules(id: Int, token: String = fx.token("admin-a")): DrawRulesDto {
        val response = send<Unit>("GET", ApiRoutes.adminGroupRules(id.toLong()), token)
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        return Json.decodeFromString(response.bodyAsText())
    }

    private suspend fun HttpClient.addRule(
        id: Int,
        giver: String,
        receiver: String,
        kind: String,
        bothWays: Boolean = false,
        token: String = fx.token("admin-a"),
    ) = send("POST", ApiRoutes.adminGroupRules(id.toLong()), token, AddDrawRuleRequest(giver, receiver, kind, bothWays))

    private fun error(body: String) = Json.decodeFromString<ErrorResponse>(body)

    @Test
    fun theOwnerAddsListsAndDeletesRules() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)
        val client = this.client

        val created = client.addRule(id, "alice", "bobby", "must")
        assertEquals(HttpStatusCode.Created, created.status, created.bodyAsText())
        val stored = Json.decodeFromString<List<DrawRuleDto>>(created.bodyAsText()).single()
        assertEquals(listOf(stored), client.rules(id).rules)

        val deleted = client.send<Unit>("DELETE", ApiRoutes.adminGroupRule(id.toLong(), stored.id), fx.token("admin-a"))
        assertEquals(HttpStatusCode.NoContent, deleted.status)
        assertEquals(emptyList(), client.rules(id).rules)
        assertEquals(HttpStatusCode.NotFound, client.send<Unit>("DELETE", ApiRoutes.adminGroupRule(id.toLong(), stored.id), fx.token("admin-a")).status)
    }

    @Test
    fun bothWaysStoresTwoRulesThatAreDeletedOnTheirOwn() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)

        val created = Json.decodeFromString<List<DrawRuleDto>>(client.addRule(id, "carol", "daves", "never", bothWays = true).bodyAsText())

        assertEquals(listOf("carol" to "daves", "daves" to "carol"), created.map { it.giver to it.receiver })
        client.send<Unit>("DELETE", ApiRoutes.adminGroupRule(id.toLong(), created[0].id), fx.token("admin-a"))
        assertEquals(listOf(created[1]), client.rules(id).rules)
    }

    @Test
    fun aConflictingBothWaysAddStoresNeitherRule() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)
        client.addRule(id, "alice", "bobby", "must")

        val response = client.addRule(id, "bobby", "alice", "must", bothWays = true)

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("rules_conflict", error(response.bodyAsText()).code)
        assertEquals(1, client.rules(id).rules.size)
    }

    @Test
    fun conflictsAreAnsweredWithTheRulesInvolved() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)
        client.addRule(id, "alice", "bobby", "must")

        val twoMusts = client.addRule(id, "alice", "carol", "must")
        assertEquals(HttpStatusCode.Conflict, twoMusts.status)
        val body = error(twoMusts.bodyAsText())
        assertEquals("rules_conflict", body.code)
        assertTrue("alice must give to bobby" in body.message && "alice must give to carol" in body.message, body.message)

        assertEquals(HttpStatusCode.Conflict, client.addRule(id, "alice", "bobby", "never").status)
        assertEquals(HttpStatusCode.Conflict, client.addRule(id, "alice", "bobby", "must").status)
        assertEquals(1, client.rules(id).rules.size)
    }

    @Test
    fun aSmallCircleIsRefused() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)
        client.addRule(id, "alice", "bobby", "must")

        val response = client.addRule(id, "bobby", "alice", "must")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("rules_conflict", error(response.bodyAsText()).code)
    }

    @Test
    fun badNamesAndKindsAreRejected() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)

        val stranger = client.addRule(id, "alice", "nobody", "must")
        assertEquals(HttpStatusCode.BadRequest, stranger.status)
        assertEquals("receiver", error(stranger.bodyAsText()).field)
        assertEquals(HttpStatusCode.BadRequest, client.addRule(id, "alice", "alice", "must").status)
        val kind = client.addRule(id, "alice", "bobby", "sometimes")
        assertEquals(HttpStatusCode.BadRequest, kind.status)
        assertEquals("kind", error(kind.bodyAsText()).field)
        assertEquals(emptyList(), client.rules(id).rules)
    }

    @Test
    fun draftHasNoRules() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names, state = "draft")

        assertEquals(HttpStatusCode.Conflict, client.send<Unit>("GET", ApiRoutes.adminGroupRules(id.toLong()), fx.token("admin-a")).status)
        val response = client.addRule(id, "alice", "bobby", "must")
        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("invalid_state", error(response.bodyAsText()).code)
    }

    @Test
    fun rulesAreReadOnlyOnceActiveAndArchived() = testApplication {
        startApp()
        setup(*names)
        for (state in listOf("active", "archived")) {
            val id = fx.addGroup("admin-a", *names, state = state)
            fx.sql("INSERT INTO draw_rules (group_id, giver, receiver, kind) VALUES ($id, 'alice', 'bobby', 'must')")
            val ruleId = client.rules(id).rules.single().id

            assertEquals(HttpStatusCode.Conflict, client.addRule(id, "carol", "daves", "never").status, state)
            assertEquals(HttpStatusCode.Conflict, client.send<Unit>("DELETE", ApiRoutes.adminGroupRule(id.toLong(), ruleId), fx.token("admin-a")).status, state)
            assertEquals(1, client.rules(id).rules.size, state)
        }
    }

    @Test
    fun onlyTheOwnerCanUseThem() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)

        assertEquals(HttpStatusCode.NotFound, client.send<Unit>("GET", ApiRoutes.adminGroupRules(id.toLong()), fx.token("admin-b")).status)
        assertEquals(HttpStatusCode.NotFound, client.addRule(id, "alice", "bobby", "must", token = fx.token("admin-b")).status)
        assertEquals(HttpStatusCode.Forbidden, client.send<Unit>("GET", ApiRoutes.adminGroupRules(id.toLong()), fx.token("plain")).status)
        assertEquals(HttpStatusCode.Forbidden, client.send<Unit>("GET", ApiRoutes.adminGroupRules(id.toLong()), fx.token("alice")).status)
        assertEquals(HttpStatusCode.Unauthorized, client.send<Unit>("GET", ApiRoutes.adminGroupRules(id.toLong()), null).status)
    }

    @Test
    fun fewPossibleDrawsAreReported() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", "alice", "bobby", "carol", "daves")
        assertFalse(client.rules(id).fewDraws)

        client.addRule(id, "alice", "bobby", "must")
        client.addRule(id, "bobby", "carol", "must")
        assertTrue(client.rules(id).fewDraws)

        val big = fx.addGroup("admin-a", *names, "fiona", "gerry", "harry")
        client.addRule(big, "alice", "bobby", "must")
        assertFalse(client.rules(big).fewDraws)
    }

    @Test
    fun removingAParticipantDeletesTheirRulesAndListsThem() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)
        client.addRule(id, "alice", "bobby", "must")
        client.addRule(id, "bobby", "carol", "never")
        client.addRule(id, "alice", "carol", "never")

        val response = client.send("PUT", ApiRoutes.adminGroupParticipants(id.toLong()), fx.token("admin-a"), ParticipantsRequest(listOf("alice", "carol", "daves", "erins")))

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val change = Json.decodeFromString<GroupChangeResponse>(response.bodyAsText())
        assertEquals(listOf("alice" to "bobby", "bobby" to "carol"), change.deletedRules.map { it.giver to it.receiver })
        assertEquals(listOf("alice" to "carol"), client.rules(id).rules.map { it.giver to it.receiver })
    }

    @Test
    fun addingParticipantsListsNoDeletedRules() = testApplication {
        startApp()
        setup(*names, "fiona")
        val id = fx.addGroup("admin-a", *names)
        client.addRule(id, "alice", "bobby", "must")

        val response = client.send("PUT", ApiRoutes.adminGroupParticipants(id.toLong()), fx.token("admin-a"), ParticipantsRequest(names.toList() + "fiona"))

        assertEquals(emptyList(), Json.decodeFromString<GroupChangeResponse>(response.bodyAsText()).deletedRules)
        assertEquals(1, client.rules(id).rules.size)
    }

    @Test
    fun rulesGoWhenTheGroupIsDeleted() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)
        client.addRule(id, "alice", "bobby", "must")

        client.send<Unit>("DELETE", ApiRoutes.adminGroup(id.toLong()) + "?confirm=true", fx.token("admin-a"))

        val left = fx.database.connection().use { c -> c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM draw_rules").use { it.next(); it.getInt(1) } } }
        assertEquals(0, left)
    }

    @Test
    fun activationHonoursTheRules() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)
        client.addRule(id, "alice", "bobby", "must")
        client.addRule(id, "carol", "daves", "never")

        val response = client.send<Unit>("POST", ApiRoutes.ADMIN_GROUP_ACTIVATE.replace("{id}", id.toString()), fx.token("admin-a"))
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val pairs = fx.database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT santa, receiver FROM assignments WHERE group_id = $id").use { rows ->
                    buildList { while (rows.next()) add(rows.getString(1) to rows.getString(2)) }
                }
            }
        }
        assertEquals(5, pairs.size)
        assertTrue("alice" to "bobby" in pairs)
        assertFalse("carol" to "daves" in pairs)
    }

    @Test
    fun activationIsRefusedWhenTheRulesCannotBeMetAndTheGroupStaysPending() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", "alice", "bobby", "carol", "daves")
        // alice may give to nobody: stored directly, as if the roster had changed under the rules
        listOf("bobby", "carol", "daves").forEach {
            fx.sql("INSERT INTO draw_rules (group_id, giver, receiver, kind) VALUES ($id, 'alice', '$it', 'never')")
        }

        val response = client.send<Unit>("POST", ApiRoutes.ADMIN_GROUP_ACTIVATE.replace("{id}", id.toString()), fx.token("admin-a"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("rules_conflict", error(response.bodyAsText()).code)
        val state = fx.database.connection().use { c -> c.createStatement().use { s -> s.executeQuery("SELECT state FROM groups WHERE id = $id").use { it.next(); it.getString(1) } } }
        assertEquals("pending", state)
        val assignments = fx.database.connection().use { c -> c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM assignments WHERE group_id = $id").use { it.next(); it.getInt(1) } } }
        assertEquals(0, assignments)
    }

    @Test
    fun participantsNeverSeeAnythingAboutRules() = testApplication {
        startApp()
        setup(*names)
        val id = fx.addGroup("admin-a", *names)
        client.addRule(id, "alice", "bobby", "must")

        val list = client.send<Unit>("GET", ApiRoutes.GROUPS, fx.token("alice")).bodyAsText()
        val one = client.send<Unit>("GET", ApiRoutes.group(id.toLong()), fx.token("alice")).bodyAsText()

        for (body in listOf(list, one)) {
            assertFalse("rule" in body.lowercase(), body)
            assertFalse("must" in body.lowercase(), body)
        }
        assertNotNull(one)
    }
}
