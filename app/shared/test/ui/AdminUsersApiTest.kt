package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The admin user calls against a server that behaves like the real one (see [FakeAdminUsers]). */
class AdminUsersApiTest {
    private val server = ScriptedServer().also { it.session() }
    private val api get() = server.api
    private val fake get() = server.adminUsers

    private fun <T> success(result: ApiResult<T>): T = (result as ApiResult.Success).value

    private fun failure(result: ApiResult<*>): ApiError = (result as ApiResult.Failure).error

    @Test
    fun theManagedUsersAreListedWithTheAdministratorAndNoOutsiders() = runTest {
        fake.add("alice", fullName = "Alice Anders")
        fake.add("dave", temporary = "Temp-dave-1")
        fake.addOutsider("zed")

        val users = success(api.adminUsers())

        assertEquals(listOf("admin", "alice", "dave"), users.map { it.user.username })
        assertEquals("Alice Anders", users[1].user.fullName)
        assertEquals("GET /admin/users", server.calls.last())
    }

    @Test
    fun aTemporaryPasswordIsReturnedOnlyWhileItIsStillTemporary() = runTest {
        fake.add("dave", temporary = "Temp-dave-1")
        fake.add("alice")

        val dave = success(api.adminUser("dave"))
        val alice = success(api.adminUser("alice"))

        assertEquals("Temp-dave-1", dave.temporaryPassword)
        assertTrue(dave.user.mustChangePassword)
        assertNull(alice.temporaryPassword)
        assertEquals("GET /admin/users/dave", server.calls[server.calls.size - 2])
    }

    @Test
    fun aUserOutsideTheScopeAnswersAsIfTheyDidNotExist() = runTest {
        fake.addOutsider("zed")

        assertEquals(ApiError.Rejected(404, "not_found", "User not found."), failure(api.adminUser("zed")))
        assertEquals(ApiError.Rejected(404, "not_found", "User not found."), failure(api.adminUser("nobody")))
        assertEquals(ApiError.Rejected(404, "not_found", "User not found."), failure(api.resetPassword("zed")))
    }

    @Test
    fun aResetGivesANewTemporaryPasswordAndRequiresAChange() = runTest {
        fake.add("alice")

        val reset = success(api.resetPassword("alice"))
        val after = success(api.adminUser("alice"))

        assertEquals("alice", reset.username)
        assertEquals(reset.temporaryPassword, after.temporaryPassword)
        assertTrue(after.user.mustChangePassword)
        assertEquals("POST /admin/users/alice/reset-password", server.calls[server.calls.size - 2])
    }

    @Test
    fun aQueuedFailureAnswersTheNextResetOnly() = runTest {
        fake.add("alice")
        fake.failNextResetWith(HttpStatusCode.Conflict, "invalid_state", "Nope.")

        assertTrue(api.resetPassword("alice") is ApiResult.Failure)
        assertTrue(api.resetPassword("alice") is ApiResult.Success)
    }
}
