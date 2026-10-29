package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.contract.GroupDetailsRequest
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.MemberGroupDto
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
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

class GroupLifecycleRoutesTest {
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

    private suspend fun HttpClient.getAs(path: String, token: String?) = send<Unit>("GET", path, token)
    private suspend fun HttpClient.postAs(path: String, token: String?) = send<Unit>("POST", path, token)

    private fun error(response: HttpResponse, body: String) = Json.decodeFromString<ErrorResponse>(body)

    private fun setup() {
        fx.addUser("admin-a", admin = true)
        fx.addUser("admin-b", admin = true)
    }

    private fun people(vararg names: String) = names.forEach { fx.addUser(it) }

    private suspend fun HttpClient.draft(vararg participants: String, token: String = fx.token("admin-a")): AdminGroupDto {
        val response = send("POST", ApiRoutes.ADMIN_GROUPS, token, CreateGroupRequest("Party", participants = participants.toList()))
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return Json.decodeFromString(response.bodyAsText())
    }

    private suspend fun HttpClient.change(path: String, token: String = fx.token("admin-a")): GroupChangeResponse {
        val response = postAs(path, token)
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        return Json.decodeFromString(response.bodyAsText())
    }

    private suspend fun HttpClient.assignments(id: Long, token: String = fx.token("admin-a")): List<AssignmentDto> =
        Json.decodeFromString(getAs(ApiRoutes.adminGroupAssignments(id), token).bodyAsText())

    private fun database(query: String): String = fx.database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery(query).use { it.next(); it.getString(1) } }
    }

    // --- the whole path ---

    @Test
    fun aGroupWalksFromDraftToArchivedAndEachStepIsReflected() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol", "dave")
        val group = client.draft("alice", "bob", "carol", "dave")
        assertEquals("draft", group.state)

        assertEquals("pending", client.change(ApiRoutes.adminGroupPending(group.id)).group.state)
        assertEquals("active", client.change(ApiRoutes.adminGroupActivate(group.id)).group.state)
        assertEquals("archived", client.change(ApiRoutes.adminGroupArchive(group.id)).group.state)

        assertEquals("archived", Json.decodeFromString<AdminGroupDto>(client.getAs(ApiRoutes.adminGroup(group.id), fx.token("admin-a")).bodyAsText()).state)
    }

    // --- order and ownership ---

    @Test
    fun skippingBackwardsAndRepeatingAreAllConflicts() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol", "dave")
        val token = fx.token("admin-a")
        val group = client.draft("alice", "bob", "carol", "dave")

        for (path in listOf(ApiRoutes.adminGroupActivate(group.id), ApiRoutes.adminGroupArchive(group.id))) {
            val response = client.postAs(path, token)
            assertEquals(HttpStatusCode.Conflict, response.status, path)
            assertEquals("invalid_state", error(response, response.bodyAsText()).code)
        }
        client.change(ApiRoutes.adminGroupPending(group.id))
        assertEquals(HttpStatusCode.Conflict, client.postAs(ApiRoutes.adminGroupPending(group.id), token).status)
        client.change(ApiRoutes.adminGroupActivate(group.id))
        assertEquals(HttpStatusCode.Conflict, client.postAs(ApiRoutes.adminGroupActivate(group.id), token).status)
        assertEquals(HttpStatusCode.Conflict, client.postAs(ApiRoutes.adminGroupPending(group.id), token).status)
        assertEquals("active", database("SELECT state FROM groups WHERE id = ${group.id}"))
        assertEquals("4", database("SELECT COUNT(*) FROM assignments WHERE group_id = ${group.id}"))
    }

    @Test
    fun onlyTheOwnerMovesAGroupAndOthersGetNotFound() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol", "dave")
        val group = client.draft("alice", "bob", "carol", "dave")

        for (path in listOf(ApiRoutes.adminGroupPending(group.id), ApiRoutes.adminGroupActivate(group.id), ApiRoutes.adminGroupArchive(group.id))) {
            assertEquals(HttpStatusCode.NotFound, client.postAs(path, fx.token("admin-b")).status, path)
        }
        assertEquals(HttpStatusCode.NotFound, client.postAs("/admin/groups/nope/pending", fx.token("admin-a")).status)
        assertEquals("draft", database("SELECT state FROM groups WHERE id = ${group.id}"))
    }

    // --- Draft to Pending ---

    @Test
    fun pendingCreatesMissingAccountsAndReturnsTheirTemporaryPasswords() = testApplication {
        startApp()
        setup()
        people("alice")
        val group = client.draft("alice", "newbie")
        assertEquals(null, fx.users.find(Username.parse("newbie")))

        val change = client.change(ApiRoutes.adminGroupPending(group.id))

        val created = change.createdAccounts.single()
        assertEquals("newbie", created.username)
        assertTrue(fx.users.find(Username.parse("newbie"))!!.mustChangePassword)
        val login = client.request(ApiRoutes.LOGIN) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest("newbie", created.temporaryPassword)))
        }
        assertEquals(HttpStatusCode.OK, login.status)
        assertEquals(1, fx.users.find(Username.parse("alice"))!!.tokenVersion)
    }

    @Test
    fun theOwnerGainsScopeOverParticipantsOnlyOnceTheGroupIsPending() = testApplication {
        startApp()
        setup()
        people("alice")
        val group = client.draft("alice")
        val token = fx.token("admin-a")
        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.adminUser("alice"), token).status)

        client.change(ApiRoutes.adminGroupPending(group.id))

        assertEquals(HttpStatusCode.OK, client.getAs(ApiRoutes.adminUser("alice"), token).status)
        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.adminUser("alice"), fx.token("admin-b")).status)
    }

    @Test
    fun participantsSeePendingGroupsButNotDraftsAndNewcomersMustChangeTheirPasswordFirst() = testApplication {
        startApp()
        setup()
        people("alice")
        val group = client.draft("alice", "newbie")
        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.group(group.id), fx.token("alice")).status)
        assertEquals("[]", client.getAs(ApiRoutes.GROUPS, fx.token("alice")).bodyAsText())

        client.change(ApiRoutes.adminGroupPending(group.id))

        val mine = Json.decodeFromString<List<MemberGroupDto>>(client.getAs(ApiRoutes.GROUPS, fx.token("alice")).bodyAsText())
        assertEquals(listOf(group.id), mine.map { it.id })
        assertEquals(listOf("alice", "newbie"), mine.single().participants)
        val gated = client.getAs(ApiRoutes.GROUPS, fx.token("newbie"))
        assertEquals(HttpStatusCode.Forbidden, gated.status)
        assertEquals("password_change_required", error(gated, gated.bodyAsText()).code)
    }

    @Test
    fun anEmptyDraftBecomesPendingWithoutCreatingAccounts() = testApplication {
        startApp()
        setup()
        val group = client.draft()

        val change = client.change(ApiRoutes.adminGroupPending(group.id))

        assertEquals("pending", change.group.state)
        assertEquals(emptyList(), change.createdAccounts)
    }

    // --- Pending to Active ---

    @Test
    fun fewerThanFourParticipantsCannotBeActivated() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol")
        val group = client.draft("alice", "bob", "carol")
        client.change(ApiRoutes.adminGroupPending(group.id))

        val response = client.postAs(ApiRoutes.adminGroupActivate(group.id), fx.token("admin-a"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("participants", error(response, response.bodyAsText()).field)
        assertEquals("pending", database("SELECT state FROM groups WHERE id = ${group.id}"))
        assertEquals("0", database("SELECT COUNT(*) FROM assignments"))
    }

    @Test
    fun anIncompleteProfileDoesNotBlockActivationButIsFlagged() = testApplication {
        startApp()
        setup()
        people("alice", "bob")
        fx.users.updateProfile(Username.parse("alice"), Profile(fullName = "Alice B", address = "1 Main St"))
        val group = client.draft("alice", "bob", "carol-new", "dave-new")
        client.change(ApiRoutes.adminGroupPending(group.id))

        val change = client.change(ApiRoutes.adminGroupActivate(group.id))

        assertEquals("active", change.group.state)
        assertEquals(emptyList(), change.createdAccounts)
        assertEquals(
            mapOf("alice" to true, "bob" to false, "carol-new" to false, "dave-new" to false),
            change.group.participants.associate { it.username to it.profileComplete },
        )
    }

    // --- assignments ---

    @Test
    fun activationDrawsOneValidCycleOverEveryParticipant() = testApplication {
        startApp()
        setup()
        val names = listOf("alice", "bob", "carol", "dave", "erin", "frank")
        people(*names.toTypedArray())
        val group = client.draft(*names.toTypedArray())
        client.change(ApiRoutes.adminGroupPending(group.id))
        client.change(ApiRoutes.adminGroupActivate(group.id))

        val pairs = client.assignments(group.id)

        assertEquals(names.size, pairs.size)
        assertEquals(names.toSet(), pairs.map { it.santa }.toSet())
        assertEquals(names.toSet(), pairs.map { it.receiver }.toSet())
        assertTrue(pairs.none { it.santa == it.receiver })
        val receiverOf = pairs.associate { it.santa to it.receiver }
        val chain = generateSequence("alice") { receiverOf.getValue(it) }.drop(1).takeWhile { it != "alice" }.toList() + "alice"
        assertEquals(names.size, chain.size, "one chain through everyone")
    }

    @Test
    fun noAssignmentsBeforeActivation() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol", "dave")
        val group = client.draft("alice", "bob", "carol", "dave")
        val token = fx.token("admin-a")

        for (step in 0..1) {
            val response = client.getAs(ApiRoutes.adminGroupAssignments(group.id), token)
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals("invalid_state", error(response, response.bodyAsText()).code)
            if (step == 0) client.change(ApiRoutes.adminGroupPending(group.id))
        }
        assertEquals("0", database("SELECT COUNT(*) FROM assignments"))
    }

    @Test
    fun onlyTheOwnerCanListAssignments() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol", "dave")
        val group = client.draft("alice", "bob", "carol", "dave")
        client.change(ApiRoutes.adminGroupPending(group.id))
        client.change(ApiRoutes.adminGroupActivate(group.id))

        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.adminGroupAssignments(group.id), fx.token("admin-b")).status)
        assertEquals(HttpStatusCode.Forbidden, client.getAs(ApiRoutes.adminGroupAssignments(group.id), fx.token("alice")).status)
        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.adminGroupAssignments(group.id), null).status)
        assertEquals(4, client.assignments(group.id).size)
    }

    @Test
    fun participantsNeverSeeAnythingAboutAssignments() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol", "dave")
        val group = client.draft("alice", "bob", "carol", "dave")
        client.change(ApiRoutes.adminGroupPending(group.id))
        client.change(ApiRoutes.adminGroupActivate(group.id))

        for (user in listOf("alice", "bob", "carol", "dave")) {
            for (path in listOf(ApiRoutes.GROUPS, ApiRoutes.group(group.id))) {
                val body = client.getAs(path, fx.token(user)).bodyAsText()
                for (forbidden in listOf("santa", "receiver", "assignment", "gives")) assertFalse(body.contains(forbidden, ignoreCase = true), "$user $path: $body")
            }
        }
        val view = Json.decodeFromString<MemberGroupDto>(client.getAs(ApiRoutes.group(group.id), fx.token("alice")).bodyAsText())
        assertEquals("active", view.state)
        assertEquals(listOf("alice", "bob", "carol", "dave"), view.participants)
    }

    @Test
    fun editingOrArchivingNeverChangesWhoGivesToWhom() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol", "dave")
        val group = client.draft("alice", "bob", "carol", "dave")
        client.change(ApiRoutes.adminGroupPending(group.id))
        client.change(ApiRoutes.adminGroupActivate(group.id))
        val drawn = client.assignments(group.id)

        val edit = client.send("PUT", ApiRoutes.adminGroup(group.id), fx.token("admin-a"), GroupDetailsRequest("Renamed", location = "Elsewhere"))
        assertEquals(HttpStatusCode.OK, edit.status)
        assertEquals(drawn, client.assignments(group.id))

        client.change(ApiRoutes.adminGroupArchive(group.id))
        assertEquals(drawn, client.assignments(group.id))
        assertEquals(HttpStatusCode.Conflict, client.send("PUT", ApiRoutes.adminGroup(group.id), fx.token("admin-a"), GroupDetailsRequest("Too late")).status)
    }

    @Test
    fun peopleAddedInPendingTakePartInTheDraw() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol")
        val group = client.draft("alice", "bob", "carol")
        client.change(ApiRoutes.adminGroupPending(group.id))
        val participants = client.send("PUT", ApiRoutes.adminGroupParticipants(group.id), fx.token("admin-a"), com.usuallycoding.opensecretsanta.contract.ParticipantsRequest(listOf("alice", "bob", "carol", "latecomer")))
        assertEquals(HttpStatusCode.OK, participants.status)
        assertNotNull(fx.users.find(Username.parse("latecomer")))

        client.change(ApiRoutes.adminGroupActivate(group.id))

        assertEquals(setOf("alice", "bob", "carol", "latecomer"), client.assignments(group.id).map { it.santa }.toSet())
    }
    // --- newest first ---

    private suspend fun HttpClient.ownedIds(): List<Long> =
        Json.decodeFromString<List<AdminGroupDto>>(getAs(ApiRoutes.ADMIN_GROUPS, fx.token("admin-a")).bodyAsText()).map { it.id }

    private suspend fun HttpClient.visibleIds(user: String): List<Long> =
        Json.decodeFromString<List<MemberGroupDto>>(getAs(ApiRoutes.GROUPS, fx.token(user)).bodyAsText()).map { it.id }

    @Test
    fun aGroupMadeLaterIsFirstInTheAdministratorsList() = testApplication {
        startApp()
        setup()
        people("alice")
        val first = client.draft("alice")
        val second = client.draft("alice") // as a duplicate of the first would be

        assertEquals(listOf(second.id, first.id), client.ownedIds())
    }

    @Test
    fun participantsSeeTheNewestGroupFirstAndOnlyTheCreationTimeDecides() = testApplication {
        startApp()
        setup()
        people("alice")
        val old = client.draft("alice")
        val middle = client.draft("alice")
        val new = client.draft("alice")
        for (group in listOf(old, middle, new)) client.change(ApiRoutes.adminGroupPending(group.id))
        fx.sql("UPDATE groups SET created_at = '2026-01-01 00:00:00' WHERE id = ${old.id}")
        fx.sql("UPDATE groups SET created_at = '2026-02-01 00:00:00' WHERE id = ${middle.id}")
        fx.sql("UPDATE groups SET created_at = '2026-03-01 00:00:00' WHERE id = ${new.id}")

        assertEquals(listOf(new.id, middle.id, old.id), client.visibleIds("alice"))
    }

    @Test
    fun editingMovingAndDrawingTheOldestGroupLeavesItLast() = testApplication {
        startApp()
        setup()
        people("alice", "bob", "carol", "dave")
        val oldest = client.draft("alice", "bob", "carol", "dave")
        val newest = client.draft("alice", "bob", "carol", "dave")
        fx.sql("UPDATE groups SET created_at = '2026-01-01 00:00:00' WHERE id = ${oldest.id}")
        fx.sql("UPDATE groups SET created_at = '2026-02-01 00:00:00' WHERE id = ${newest.id}")

        val renamed = client.send("PUT", ApiRoutes.adminGroup(oldest.id), fx.token("admin-a"), GroupDetailsRequest("Renamed"))
        assertEquals(HttpStatusCode.OK, renamed.status, renamed.bodyAsText())
        client.change(ApiRoutes.adminGroupPending(oldest.id))
        client.change(ApiRoutes.adminGroupActivate(oldest.id))

        assertEquals(listOf(newest.id, oldest.id), client.ownedIds())
        client.change(ApiRoutes.adminGroupPending(newest.id))
        assertEquals(listOf(newest.id, oldest.id), client.visibleIds("alice"))
    }

}
