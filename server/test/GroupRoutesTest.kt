package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.contract.GroupDetailsRequest
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.ParticipantsRequest
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupRoutesTest {
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
    private suspend fun HttpClient.deleteAs(path: String, token: String?) = send<Unit>("DELETE", path, token)

    private suspend fun HttpClient.createGroup(token: String, request: CreateGroupRequest = CreateGroupRequest("Party")): AdminGroupDto {
        val response = send("POST", ApiRoutes.ADMIN_GROUPS, token, request)
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return Json.decodeFromString(response.bodyAsText())
    }

    private fun error(response: String) = Json.decodeFromString<ErrorResponse>(response)

    private fun admins() {
        fx.addUser("admin-a", admin = true)
        fx.addUser("admin-b", admin = true)
        fx.addUser("carol")
    }

    private fun groupState(id: Long): String = database("SELECT state FROM groups WHERE id = $id")

    private fun database(query: String): String = fx.database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery(query).use { it.next(); it.getString(1) } }
    }

    // --- create ---

    @Test
    fun creatingAGroupGivesADraftOwnedByTheCaller() = testApplication {
        startApp()
        admins()

        val group = client.createGroup(fx.token("admin-a"), CreateGroupRequest("Office party"))

        assertEquals("draft", group.state)
        assertEquals("admin-a", group.owner)
        assertEquals("Office party", group.name)
        assertEquals(emptyList(), group.participants)
        assertNull(group.description)
        assertNull(group.date)
    }

    @Test
    fun allFieldsAreStoredAndReturned() = testApplication {
        startApp()
        admins()

        val group = client.createGroup(
            fx.token("admin-a"),
            CreateGroupRequest("Party", "Bring snacks", "HQ", "2026-12-24", "18:30", listOf("alice", "newcomer")),
        )

        assertEquals("Bring snacks", group.description)
        assertEquals("HQ", group.location)
        assertEquals("2026-12-24", group.date)
        assertEquals("18:30", group.time)
        assertEquals(listOf("alice", "newcomer"), group.participants.map { it.username })
        assertEquals(listOf(false, false), group.participants.map { it.hasAccount })
    }

    @Test
    fun nonAdminsCannotCreateGroups() = testApplication {
        startApp()
        admins()

        val response = client.send("POST", ApiRoutes.ADMIN_GROUPS, fx.token("carol"), CreateGroupRequest("Party"))

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun invalidInputIsRejectedNamingTheFieldAndSavesNothing() = testApplication {
        startApp()
        admins()
        val token = fx.token("admin-a")
        val cases = mapOf(
            "name" to CreateGroupRequest("   "),
            "name" to CreateGroupRequest("n".repeat(101)),
            "date" to CreateGroupRequest("P", date = "2026-02-30"),
            "time" to CreateGroupRequest("P", time = "24:00"),
            "participants" to CreateGroupRequest("P", participants = listOf("alice", "no")),
        )
        for ((field, request) in cases) {
            val response = client.send("POST", ApiRoutes.ADMIN_GROUPS, token, request)
            assertEquals(HttpStatusCode.BadRequest, response.status, field)
            assertEquals(field, error(response.bodyAsText()).field)
        }
        assertEquals("0", database("SELECT COUNT(*) FROM groups"))
    }

    @Test
    fun aMalformedBodyIsABadRequest() = testApplication {
        startApp()
        admins()

        val response = client.post(ApiRoutes.ADMIN_GROUPS) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("admin-a")}")
            contentType(ContentType.Application.Json)
            setBody("{not json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // --- list, view, owner-only ---

    @Test
    fun listContainsOnlyTheCallersGroups() = testApplication {
        startApp()
        admins()
        client.createGroup(fx.token("admin-a"), CreateGroupRequest("A one"))
        client.createGroup(fx.token("admin-a"), CreateGroupRequest("A two"))
        client.createGroup(fx.token("admin-b"), CreateGroupRequest("B one"))

        val list = Json.decodeFromString<List<AdminGroupDto>>(client.getAs(ApiRoutes.ADMIN_GROUPS, fx.token("admin-a")).bodyAsText())

        assertEquals(listOf("A one", "A two"), list.map { it.name })
    }

    @Test
    fun viewShowsParticipantFlags() = testApplication {
        startApp()
        admins()
        val alice = fx.addUser("alice")
        fx.users.updateProfile(alice, Profile(fullName = "Alice B", address = "1 Main St"))
        fx.addUser("bob")
        val group = client.createGroup(fx.token("admin-a"), CreateGroupRequest("P", participants = listOf("alice", "bob", "carol-new")))

        val view = Json.decodeFromString<AdminGroupDto>(client.getAs(ApiRoutes.adminGroup(group.id), fx.token("admin-a")).bodyAsText())

        val flags = view.participants.associate { it.username to (it.hasAccount to it.profileComplete) }
        assertEquals(true to true, flags["alice"])
        assertEquals(true to false, flags["bob"])
        assertEquals(false to false, flags["carol-new"])
    }

    @Test
    fun otherAdminsAndUnknownOrNonNumericIdsGetNotFoundOnEveryRoute() = testApplication {
        startApp()
        admins()
        val group = client.createGroup(fx.token("admin-a"))
        val other = fx.token("admin-b")

        val attempts = listOf(
            client.getAs(ApiRoutes.adminGroup(group.id), other),
            client.send("PUT", ApiRoutes.adminGroup(group.id), other, GroupDetailsRequest("Hijacked")),
            client.send("PUT", ApiRoutes.adminGroupParticipants(group.id), other, ParticipantsRequest(listOf("mallory"))),
            client.deleteAs(ApiRoutes.adminGroup(group.id), other),
            client.getAs(ApiRoutes.adminGroup(9999), fx.token("admin-a")),
            client.getAs("/admin/groups/not-a-number", fx.token("admin-a")),
        )

        for (response in attempts) assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("not_found", error(attempts.first().bodyAsText()).code)
        assertEquals("draft", groupState(group.id))
        assertEquals("Party", database("SELECT name FROM groups WHERE id = ${group.id}"))
    }

    @Test
    fun nonAdminsAreForbiddenAndAnonymousCallersUnauthorized() = testApplication {
        startApp()
        admins()
        val group = client.createGroup(fx.token("admin-a"))

        assertEquals(HttpStatusCode.Forbidden, client.getAs(ApiRoutes.ADMIN_GROUPS, fx.token("carol")).status)
        assertEquals(HttpStatusCode.Forbidden, client.getAs(ApiRoutes.adminGroup(group.id), fx.token("carol")).status)
        assertEquals(HttpStatusCode.Unauthorized, client.getAs(ApiRoutes.ADMIN_GROUPS, null).status)
    }

    @Test
    fun anAdminWithATemporaryPasswordIsBlockedUntilTheyChangeIt() = testApplication {
        startApp()
        fx.addUser("boss", admin = true, mustChange = true)

        val response = client.getAs(ApiRoutes.ADMIN_GROUPS, fx.token("boss"))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("password_change_required", error(response.bodyAsText()).code)
    }

    // --- edit ---

    @Test
    fun detailsCanBeEditedInActiveButNotArchived() = testApplication {
        startApp()
        admins()
        val active = fx.addGroup("admin-a", "alice", "bob", "carol-x", state = "active")
        val archived = fx.addGroup("admin-a", state = "archived")
        val token = fx.token("admin-a")

        val ok = client.send("PUT", ApiRoutes.adminGroup(active.toLong()), token, GroupDetailsRequest("Group", location = "New place"))
        val refused = client.send("PUT", ApiRoutes.adminGroup(archived.toLong()), token, GroupDetailsRequest("Too late"))

        assertEquals(HttpStatusCode.OK, ok.status)
        assertEquals("New place", Json.decodeFromString<AdminGroupDto>(ok.bodyAsText()).location)
        assertEquals(HttpStatusCode.Conflict, refused.status)
        assertEquals("invalid_state", error(refused.bodyAsText()).code)
        assertEquals("Group", database("SELECT name FROM groups WHERE id = $archived"))
    }

    @Test
    fun invalidEditIsRejectedAndNothingChanges() = testApplication {
        startApp()
        admins()
        val group = client.createGroup(fx.token("admin-a"), CreateGroupRequest("Keep me", location = "HQ"))

        val response = client.send("PUT", ApiRoutes.adminGroup(group.id), fx.token("admin-a"), GroupDetailsRequest("", location = "Elsewhere"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("HQ", database("SELECT location FROM groups WHERE id = ${group.id}"))
    }

    // --- participants ---

    @Test
    fun draftParticipantsAreNamesOnlyAndReplaceThePreviousList() = testApplication {
        startApp()
        admins()
        fx.addUser("alice")
        val group = client.createGroup(fx.token("admin-a"), CreateGroupRequest("P", participants = listOf("old-one")))

        val response = client.send("PUT", ApiRoutes.adminGroupParticipants(group.id), fx.token("admin-a"), ParticipantsRequest(listOf("Alice", "alice", "newcomer")))

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<GroupChangeResponse>(response.bodyAsText())
        assertEquals(listOf("alice", "newcomer"), body.group.participants.map { it.username })
        assertEquals(emptyList(), body.createdAccounts)
        assertNull(fx.users.find(Username.parse("newcomer")))
    }

    @Test
    fun anInvalidParticipantIsRejectedAndTheListIsUnchanged() = testApplication {
        startApp()
        admins()
        val group = client.createGroup(fx.token("admin-a"), CreateGroupRequest("P", participants = listOf("keep-me")))

        val response = client.send("PUT", ApiRoutes.adminGroupParticipants(group.id), fx.token("admin-a"), ParticipantsRequest(listOf("fine-name", "x")))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("participants", error(response.bodyAsText()).field)
        assertEquals("keep-me", database("SELECT username FROM group_participants WHERE group_id = ${group.id}"))
    }

    @Test
    fun inPendingNewPeopleGetAccountsAndTheirTemporaryPasswordIsReturned() = testApplication {
        startApp()
        admins()
        fx.addUser("erin", password = "erins-own-password")
        val id = fx.addGroup("admin-a", "erin", state = "pending")

        val response = client.send("PUT", ApiRoutes.adminGroupParticipants(id.toLong()), fx.token("admin-a"), ParticipantsRequest(listOf("erin", "dave")))

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<GroupChangeResponse>(response.bodyAsText())
        val dave = body.createdAccounts.single()
        assertEquals("dave", dave.username)
        assertEquals(12, dave.temporaryPassword.length)
        assertTrue(fx.users.find(Username.parse("dave"))!!.mustChangePassword)
        val login = client.post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest("dave", dave.temporaryPassword)))
        }
        assertEquals(HttpStatusCode.OK, login.status)
        // an existing user is not touched
        assertEquals(1, fx.users.find(Username.parse("erin"))!!.tokenVersion)
        assertFalse(response.bodyAsText().contains("erins-own-password"))
    }

    @Test
    fun removingSomeoneInPendingKeepsTheirAccount() = testApplication {
        startApp()
        admins()
        fx.addUser("bob")
        val id = fx.addGroup("admin-a", "bob", "carol", state = "pending")

        client.send("PUT", ApiRoutes.adminGroupParticipants(id.toLong()), fx.token("admin-a"), ParticipantsRequest(listOf("carol")))

        assertEquals("carol", database("SELECT group_concat(username) FROM group_participants WHERE group_id = $id"))
        assertNotNull(fx.users.find(Username.parse("bob")))
    }

    @Test
    fun theRosterIsFrozenOnceActive() = testApplication {
        startApp()
        admins()
        val id = fx.addGroup("admin-a", "a-one", "b-two", "c-three", state = "active")

        val response = client.send("PUT", ApiRoutes.adminGroupParticipants(id.toLong()), fx.token("admin-a"), ParticipantsRequest(listOf("a-one")))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("invalid_state", error(response.bodyAsText()).code)
        assertEquals("3", database("SELECT COUNT(*) FROM group_participants WHERE group_id = $id"))
    }

    // --- delete ---

    @Test
    fun aDraftCanBeDeletedAndThenNoLongerExists() = testApplication {
        startApp()
        admins()
        val group = client.createGroup(fx.token("admin-a"), CreateGroupRequest("P", participants = listOf("alice")))

        assertEquals(HttpStatusCode.NoContent, client.deleteAs(ApiRoutes.adminGroup(group.id), fx.token("admin-a")).status)

        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.adminGroup(group.id), fx.token("admin-a")).status)
        assertEquals("0", database("SELECT COUNT(*) FROM group_participants"))
    }

    /** A drawn group of alice and bob (users are created here) with wishlist items, questions and an answer. */
    private fun fillGroup(id: Int) {
        for (name in listOf("alice", "bob")) {
            if (database("SELECT COUNT(*) FROM users WHERE username = '$name'") == "0") fx.addUser(name)
        }
        fx.assign(id, "alice" to "bob", "bob" to "alice")
        fx.sql("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES ($id, 'alice', 0, 'Socks', 1, 1)")
        fx.sql("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES ($id, 'alice', 1, 'Tea', 1, 1)")
        fx.sql("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES ($id, 'bob', 0, 'Mug', 1, 1)")
        fx.sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'alice', 1, 'Colour?', 1)")
        fx.sql("INSERT INTO questions (group_id, santa, number, text, asked_at) VALUES ($id, 'bob', 1, 'Size?', 1)")
        fx.sql("UPDATE questions SET answer = 'Green', answered_at = 2, answer_updated_at = 2 WHERE group_id = $id AND santa = 'alice'")
    }

    private fun rows(table: String, id: Int) = database("SELECT COUNT(*) FROM $table WHERE group_id = $id").toInt()

    @Test
    fun aPendingGroupWithNothingLinkedIsDeletedAtOnce() = testApplication {
        startApp()
        admins()
        val id = fx.addGroup("admin-a", "alice", state = "pending")

        assertEquals(HttpStatusCode.NoContent, client.deleteAs(ApiRoutes.adminGroup(id.toLong()), fx.token("admin-a")).status)

        assertEquals(HttpStatusCode.NotFound, client.getAs(ApiRoutes.adminGroup(id.toLong()), fx.token("admin-a")).status)
    }

    @Test
    fun aPendingGroupWithWishlistItemsNeedsConfirmation() = testApplication {
        startApp()
        admins()
        fx.addUser("alice")
        val id = fx.addGroup("admin-a", "alice", state = "pending")
        fx.sql("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES ($id, 'alice', 0, 'Socks', 1, 1)")

        val response = client.deleteAs(ApiRoutes.adminGroup(id.toLong()), fx.token("admin-a"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = error(response.bodyAsText())
        assertEquals("confirmation_required", body.code)
        assertEquals(DeletionImpactDto(wishlistItems = 1, questions = 0, answers = 0, draw = false), body.impact)
        assertEquals("pending", groupState(id.toLong()))
    }

    @Test
    fun aDrawnGroupAsksForConfirmationWithTheCountsAndKeepsEverything() = testApplication {
        startApp()
        admins()
        for (state in listOf("active", "archived")) {
            val id = fx.addGroup("admin-a", "alice", "bob", state = state)
            fillGroup(id)

            val response = client.deleteAs(ApiRoutes.adminGroup(id.toLong()), fx.token("admin-a"))

            assertEquals(HttpStatusCode.Conflict, response.status, state)
            val body = error(response.bodyAsText())
            assertEquals("confirmation_required", body.code)
            assertEquals(DeletionImpactDto(wishlistItems = 3, questions = 2, answers = 1, draw = true), body.impact)
            assertEquals(state, groupState(id.toLong()))
            assertEquals(3, rows("wishlist_items", id))
            assertEquals(2, rows("questions", id))
        }
    }

    @Test
    fun aConfirmedDeletionRemovesTheGroupAndAllItsData() = testApplication {
        startApp()
        admins()
        for (state in listOf("pending", "active", "archived")) {
            val id = fx.addGroup("admin-a", "alice", "bob", state = state)
            fillGroup(id)

            val response = client.deleteAs(ApiRoutes.adminGroup(id.toLong()) + "?confirm=true", fx.token("admin-a"))

            assertEquals(HttpStatusCode.NoContent, response.status, state)
            for (table in listOf("groups", "group_participants", "assignments", "wishlist_items", "questions")) {
                val where = if (table == "groups") "id" else "group_id"
                assertEquals("0", database("SELECT COUNT(*) FROM $table WHERE $where = $id"), "$state $table")
            }
        }
    }

    @Test
    fun aDeletionConfirmedWithAnythingButTrueStillAsks() = testApplication {
        startApp()
        admins()
        val id = fx.addGroup("admin-a", "alice", "bob", state = "active")
        fillGroup(id)

        val response = client.deleteAs(ApiRoutes.adminGroup(id.toLong()) + "?confirm=yes", fx.token("admin-a"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("active", groupState(id.toLong()))
    }

    @Test
    fun anotherAdministratorGetsNotFoundWhetherOrNotConfirmed() = testApplication {
        startApp()
        admins()
        val id = fx.addGroup("admin-a", "alice", "bob", state = "active")
        fillGroup(id)

        for (suffix in listOf("", "?confirm=true")) {
            val response = client.deleteAs(ApiRoutes.adminGroup(id.toLong()) + suffix, fx.token("admin-b"))
            assertEquals(HttpStatusCode.NotFound, response.status, suffix)
        }
        assertEquals("active", groupState(id.toLong()))
    }

    @Test
    fun aMissingGroupIsNotFound() = testApplication {
        startApp()
        admins()

        assertEquals(HttpStatusCode.NotFound, client.deleteAs(ApiRoutes.adminGroup(404) + "?confirm=true", fx.token("admin-a")).status)
    }
}
