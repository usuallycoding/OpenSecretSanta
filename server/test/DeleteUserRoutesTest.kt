package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.ManagedUserDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteUserRoutesTest {
    private val fx = TestFixtures()

    private fun ApplicationTestBuilder.startApp() = application { module(fx.config) }

    private suspend fun HttpClient.get(path: String, token: String?): HttpResponse =
        get(path) { if (token != null) header(HttpHeaders.Authorization, "Bearer $token") }

    private suspend fun HttpClient.remove(username: String, token: String?): HttpResponse =
        delete(ApiRoutes.adminUser(username)) { if (token != null) header(HttpHeaders.Authorization, "Bearer $token") }

    private fun creator(name: String, createdBy: String) = fx.sql("UPDATE users SET created_by = '$createdBy' WHERE username = '$name'")

    private fun count(sql: String): Int = fx.database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery(sql).use { it.next(); it.getInt(1) } }
    }

    private fun exists(name: String) = count("SELECT COUNT(*) FROM users WHERE username = '$name'") == 1

    /** admin-a made alice (in A's Active group), erin (in no group) and gina (named only in B's Draft); admin-b made fred (in no group). */
    private fun world() {
        fx.addUser("admin-a", admin = true)
        fx.addUser("admin-b", admin = true)
        listOf("alice", "erin", "gina").forEach { fx.addUser(it); creator(it, "admin-a") }
        fx.addUser("fred"); creator("fred", "admin-b")
        fx.addGroup("admin-a", "alice", state = "active")
        fx.addGroup("admin-b", "gina", state = "draft")
    }

    private suspend fun HttpClient.users(token: String): List<ManagedUserDto> =
        Json.decodeFromString(get(ApiRoutes.ADMIN_USERS, token).bodyAsText())

    @Test
    fun theListHoldsTheCallersOwnUnassignedUsersAndFlagsOnlyThoseAsDeletable() = testApplication {
        startApp()
        world()

        val listed = client.users(fx.token("admin-a")).associate { it.user.username to it.canDelete }

        assertEquals(mapOf("admin-a" to false, "alice" to false, "erin" to true), listed)
    }

    @Test
    fun theDetailSaysWhetherTheUserCanBeDeleted() = testApplication {
        startApp()
        world()

        val erin = Json.decodeFromString<ManagedUserDto>(client.get(ApiRoutes.adminUser("erin"), fx.token("admin-a")).bodyAsText())
        val alice = Json.decodeFromString<ManagedUserDto>(client.get(ApiRoutes.adminUser("alice"), fx.token("admin-a")).bodyAsText())

        assertTrue(erin.canDelete)
        assertFalse(alice.canDelete)
    }

    @Test
    fun anotherAdminsUnassignedUserIsNotFoundForListAndDetailAndDelete() = testApplication {
        startApp()
        world()
        val token = fx.token("admin-a")

        assertFalse(client.users(token).any { it.user.username == "fred" })
        assertEquals(HttpStatusCode.NotFound, client.get(ApiRoutes.adminUser("fred"), token).status)
        assertEquals(HttpStatusCode.NotFound, client.remove("fred", token).status)
        assertTrue(exists("fred"))
    }

    @Test
    fun deletingAnUnassignedUserAnswers204AndRemovesAllOfIt() = testApplication {
        startApp()
        world()
        fx.sql("INSERT INTO push_devices (token, username, platform, created_at) VALUES ('t1', 'erin', 'android', 1)")
        fx.sql("INSERT INTO notification_prefs (username, wishlist) VALUES ('erin', 0)")
        fx.sql("INSERT INTO login_attempts (username, failures) VALUES ('erin', 2)")
        val group = fx.addGroup("admin-a", "alice", state = "pending", name = "Other")
        // what is left of a wishlist when someone was removed from a Pending group
        fx.sql("INSERT INTO wishlist_items (group_id, username, position, title, created_at, updated_at) VALUES ($group, 'erin', 0, 'Socks', 1, 1)")

        val response = client.remove("erin", fx.token("admin-a"))

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertFalse(exists("erin"))
        assertEquals(0, count("SELECT COUNT(*) FROM push_devices WHERE username = 'erin'"))
        assertEquals(0, count("SELECT COUNT(*) FROM notification_prefs WHERE username = 'erin'"))
        assertEquals(0, count("SELECT COUNT(*) FROM login_attempts WHERE username = 'erin'"))
        assertEquals(0, count("SELECT COUNT(*) FROM wishlist_items WHERE username = 'erin'"))
        assertTrue(exists("alice") && exists("fred"))
    }

    @Test
    fun aUserInAGroupAnAdministratorAndTheCallerAreRefusedWith409() = testApplication {
        startApp()
        world()
        val token = fx.token("admin-a")

        assertEquals(HttpStatusCode.Conflict, client.remove("alice", token).status)
        assertEquals(HttpStatusCode.Conflict, client.remove("admin-a", token).status)
        assertEquals(HttpStatusCode.NotFound, client.remove("admin-b", token).status)
        assertTrue(exists("alice") && exists("admin-a") && exists("admin-b"))
    }

    @Test
    fun someoneNamedOnlyInADraftIsNeitherListedNorDeletable() = testApplication {
        startApp()
        world()

        assertEquals(HttpStatusCode.NotFound, client.remove("gina", fx.token("admin-a")).status)
        assertTrue(exists("gina"))
    }

    @Test
    fun aUserWhoJoinedAGroupSinceTheyWereListedIsRefused() = testApplication {
        startApp()
        world()
        val token = fx.token("admin-a")
        assertTrue(client.users(token).first { it.user.username == "erin" }.canDelete)

        fx.addGroup("admin-b", "erin", state = "active")

        assertEquals(HttpStatusCode.NotFound, client.remove("erin", token).status)
        assertTrue(exists("erin"))
    }

    @Test
    fun aNormalUserAndAnAnonymousCallerAreTurnedAway() = testApplication {
        startApp()
        world()

        assertEquals(HttpStatusCode.Forbidden, client.remove("erin", fx.token("alice")).status)
        assertEquals(HttpStatusCode.Unauthorized, client.remove("erin", null).status)
        assertTrue(exists("erin"))
    }

    @Test
    fun aDeletedUsersTokenIsNoLongerAccepted() = testApplication {
        startApp()
        world()
        val token = fx.token("erin")
        assertEquals(HttpStatusCode.OK, client.get(ApiRoutes.ME, token).status)

        client.remove("erin", fx.token("admin-a"))

        assertEquals(HttpStatusCode.Unauthorized, client.get(ApiRoutes.ME, token).status)
        val login = client.post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest("erin", "password1")))
        }
        assertEquals(HttpStatusCode.Unauthorized, login.status)
    }

    @Test
    fun theNameCanBeGivenToAGroupAgainAfterwards() = testApplication {
        startApp()
        world()
        client.remove("erin", fx.token("admin-a"))
        val created = client.post(ApiRoutes.ADMIN_GROUPS) {
            header(HttpHeaders.Authorization, "Bearer ${fx.token("admin-a")}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CreateGroupRequest("Again", participants = listOf("erin"))))
        }
        val group = Json.decodeFromString<AdminGroupDto>(created.bodyAsText())

        val pending = client.post(ApiRoutes.adminGroupPending(group.id)) { header(HttpHeaders.Authorization, "Bearer ${fx.token("admin-a")}") }

        assertEquals(HttpStatusCode.OK, pending.status)
        assertTrue(exists("erin"))
        assertEquals("admin-a", fx.database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT created_by FROM users WHERE username = 'erin'").use { it.next(); it.getString(1) } }
        })
    }

    @Test
    fun theFirstAdministratorHasNoCreator() = testApplication {
        startApp()
        fx.addUser("admin-a", admin = true)

        assertNull(fx.database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT created_by FROM users WHERE username = 'admin-a'").use { it.next(); it.getString(1) } }
        })
    }
}
