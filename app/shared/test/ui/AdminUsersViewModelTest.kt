package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.admin.AdminUserViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminUsersViewModel
import com.usuallycoding.opensecretsanta.ui.admin.UserNotice
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminUsersViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val fake get() = server.adminUsers

    private suspend fun list() = AdminUsersViewModel(server.api, immediateScope()).also { it.refresh().join() }

    private suspend fun detail(username: String, me: String = "admin") =
        AdminUserViewModel(username, me, server.api, immediateScope()).also { it.refresh().join() }

    private fun resets() = server.calls.count { it.startsWith("POST") && it.endsWith("/reset-password") }

    // --- the list ------------------------------------------------------------------------------

    @Test
    fun theManagedUsersAreListedInOrderWithTheirMarks() = runTest {
        fake.add("alice", fullName = "Alice Anders")
        fake.add("dave", temporary = "Temp-dave-1")
        fake.addOutsider("zed")

        val users = list().state.value.value!!

        assertEquals(listOf("admin", "alice", "dave"), users.map { it.username })
        assertEquals(listOf(false, false, true), users.map { it.mustChangePassword })
        assertEquals("Alice Anders", users[1].fullName)
        assertNull(users[0].fullName)
    }

    @Test
    fun onlyTheAdministratorIsListedWhenNobodyElseIsManaged() = runTest {
        assertEquals(listOf("admin"), list().state.value.value!!.map { it.username })
    }

    @Test
    fun anUnreachableServerCanBeRetriedAndAFailedRefreshKeepsTheList() = runTest {
        fake.add("alice")
        server.unreachable = true
        val model = AdminUsersViewModel(server.api, immediateScope()).also { it.refresh().join() }
        assertEquals(ApiError.Network, model.state.value.error)
        assertNull(model.state.value.value)

        server.unreachable = false
        model.refresh().join()
        server.unreachable = true
        model.refresh().join()

        assertEquals(listOf("admin", "alice"), model.state.value.value!!.map { it.username })
        assertEquals(ApiError.Network, model.state.value.error)
    }

    // --- the detail ------------------------------------------------------------------------------

    @Test
    fun fieldsTheUserHasNotSetAreNull() = runTest {
        fake.add("alice", fullName = "Alice Anders").apply { address = "1 Main Street"; phone = "  " }

        val user = detail("alice").data.value.value!!

        assertEquals("Alice Anders", user.fullName)
        assertEquals("1 Main Street", user.address)
        assertNull(user.phone)
        assertNull(user.email)
        assertNull(user.deliveryInfo)
    }

    @Test
    fun theTemporaryPasswordIsShownWhileItIsTemporaryAndHiddenAfterAChange() = runTest {
        fake.add("dave", temporary = "Temp-dave-1")
        val model = detail("dave")

        assertEquals("Temp-dave-1", model.data.value.value!!.temporaryPassword)
        assertTrue(model.data.value.value!!.mustChangePassword)

        fake.changesOwnPassword("dave")
        model.refresh().join()

        assertNull(model.data.value.value!!.temporaryPassword)
        assertFalse(model.data.value.value!!.mustChangePassword)
    }

    @Test
    fun aUserOutsideTheScopeIsReportedAsGone() = runTest {
        fake.addOutsider("zed")

        assertTrue(detail("zed").data.value.isGone())
    }

    // --- resetting ---------------------------------------------------------------------------------

    @Test
    fun aResetNeedsAConfirmationAndNothingIsSentBeforeIt() = runTest {
        fake.add("alice")
        val model = detail("alice")

        model.askReset()
        assertTrue(model.ui.value.confirming)
        assertEquals(0, resets())
        model.cancelReset()

        assertFalse(model.ui.value.confirming)
        assertNull(model.confirmReset())
        assertEquals(0, resets())
    }

    @Test
    fun aConfirmedResetShowsTheNewPasswordOnceAndTheDetailNowHasToChangeIt() = runTest {
        fake.add("alice")
        val model = detail("alice")

        model.askReset()
        model.confirmReset()?.join()

        val created = model.ui.value.created!!
        assertEquals("alice", created.username)
        assertEquals("NewTemp-alice-1", created.temporaryPassword)
        assertTrue(model.data.value.value!!.mustChangePassword)
        assertEquals("NewTemp-alice-1", model.data.value.value!!.temporaryPassword)
        assertFalse(model.ui.value.busy)

        model.dismissCreated()

        assertNull(model.ui.value.created)
    }

    @Test
    fun aSecondResetMakesAnotherPassword() = runTest {
        fake.add("alice")
        val model = detail("alice")
        model.askReset(); model.confirmReset()?.join(); model.dismissCreated()

        model.askReset(); model.confirmReset()?.join()

        assertEquals("NewTemp-alice-2", model.ui.value.created!!.temporaryPassword)
    }

    @Test
    fun theAdministratorsOwnAccountOffersNoReset() = runTest {
        val model = detail("admin", me = "admin")

        assertFalse(model.canReset)
        model.askReset()

        assertFalse(model.ui.value.confirming)
        assertNull(model.confirmReset())
        assertEquals(0, resets())
    }

    @Test
    fun aUserWhoLeftTheScopeMeanwhileIsShownAsGoneAfterTheReset() = runTest {
        fake.add("alice")
        val model = detail("alice")
        model.askReset()
        fake.leaveScope("alice")

        model.confirmReset()?.join()

        assertTrue(model.data.value.isGone())
        assertNull(model.ui.value.created)
        assertFalse(model.ui.value.busy)
    }

    @Test
    fun aNetworkFailureIsANoticeAndChangesNothing() = runTest {
        fake.add("alice")
        val model = detail("alice")
        model.askReset()
        server.unreachable = true

        model.confirmReset()?.join()

        assertEquals(UserNotice.Unreachable, model.ui.value.notice)
        assertNull(model.ui.value.created)
        assertFalse(model.ui.value.busy)
        model.dismissNotice()
        assertNull(model.ui.value.notice)
    }

    @Test
    fun anUnexpectedRefusalIsAFailedNotice() = runTest {
        fake.add("alice")
        val model = detail("alice")
        model.askReset()
        fake.failNextResetWith(HttpStatusCode.InternalServerError, "server_error", "Boom.")

        model.confirmReset()?.join()

        assertEquals(UserNotice.Failed, model.ui.value.notice)
        assertNotNull(model.data.value.value)
    }
}
