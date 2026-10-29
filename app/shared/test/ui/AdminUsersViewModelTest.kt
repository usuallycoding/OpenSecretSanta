package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.admin.AdminUserViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminUsersViewModel
import com.usuallycoding.opensecretsanta.ui.admin.UserNotice
import com.usuallycoding.opensecretsanta.ui.admin.UsersNotice
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

    // --- searching and deleting ----------------------------------------------------------------

    @Test
    fun theSearchStartsEmptyKeepsWhatWasTypedAndClears() = runTest {
        val vm = list()
        assertEquals("", vm.search)

        vm.setSearch("  Ali ")
        assertEquals("  Ali ", vm.search)

        vm.clearSearch()
        assertEquals("", vm.search)
    }

    @Test
    fun usersWhoCanBeDeletedAreMarkedAsSuch() = runTest {
        fake.add("alice")
        fake.addUnassigned("erin", fullName = "Erin Typo")

        val users = list().state.value.value!!

        assertEquals(listOf(false, false, true), users.map { it.canDelete })
    }

    @Test
    fun theDetailOfADeletableUserOffersNoDeletion() = runTest {
        fake.addUnassigned("erin")

        assertTrue(detail("erin").data.value.value!!.canDelete)
        assertEquals(0, server.calls.count { it.startsWith("DELETE") })
    }

    @Test
    fun onlyAUserTheServerSaidCanBeDeletedCanBeAskedAbout() = runTest {
        fake.add("alice")
        fake.addUnassigned("erin")
        val vm = list()

        vm.askDelete("alice")
        assertNull(vm.ui.value.confirmingDelete)
        vm.askDelete("admin")
        assertNull(vm.ui.value.confirmingDelete)
        vm.askDelete("nobody")
        assertNull(vm.ui.value.confirmingDelete)
        vm.askDelete("erin")
        assertEquals("erin", vm.ui.value.confirmingDelete)
    }

    @Test
    fun nothingIsSentUntilTheDeletionIsConfirmed() = runTest {
        fake.addUnassigned("erin")
        val vm = list()

        vm.askDelete("erin")
        vm.cancelDelete()

        assertNull(vm.ui.value.confirmingDelete)
        assertEquals(0, server.calls.count { it.startsWith("DELETE") })
        assertNull(vm.confirmDelete())
    }

    @Test
    fun aConfirmedDeletionRemovesTheUserAndReloadsTheList() = runTest {
        fake.add("alice")
        fake.addUnassigned("erin")
        fake.addUnassigned("fran")
        val vm = list()
        vm.askDelete("erin")

        vm.confirmDelete()!!.join()

        assertEquals("DELETE /admin/users/erin", server.calls.filter { it.startsWith("DELETE") }.single())
        assertEquals(listOf("admin", "alice", "fran"), vm.state.value.value!!.map { it.username })
        assertNull(vm.ui.value.notice)
        assertFalse(vm.ui.value.busy)
    }

    @Test
    fun aUserWhoJoinedAGroupMeanwhileIsReportedAndShownWithoutTheIcon() = runTest {
        fake.addUnassigned("erin")
        val vm = list()
        fake.joinsAGroup("erin")
        vm.askDelete("erin")

        vm.confirmDelete()!!.join()

        assertEquals(UsersNotice.DeleteRefused, vm.ui.value.notice)
        val erin = vm.state.value.value!!.first { it.username == "erin" }
        assertFalse(erin.canDelete)
        assertFalse(vm.ui.value.busy)
    }

    @Test
    fun aUserWhoLeftTheScopeMeanwhileJustDisappearsFromTheList() = runTest {
        fake.addUnassigned("erin")
        val vm = list()
        fake.leaveScope("erin")
        vm.askDelete("erin")

        vm.confirmDelete()!!.join()

        assertEquals(listOf("admin"), vm.state.value.value!!.map { it.username })
        assertNull(vm.ui.value.notice)
    }

    @Test
    fun anUnreachableServerLeavesTheUserAndSaysSo() = runTest {
        fake.addUnassigned("erin")
        val vm = list()
        vm.askDelete("erin")
        server.unreachable = true

        vm.confirmDelete()!!.join()

        assertEquals(UsersNotice.Unreachable, vm.ui.value.notice)
        server.unreachable = false
        assertTrue(fake.usernames.contains("erin"))
    }

    @Test
    fun anyOtherFailureIsSaidAndKeepsTheUser() = runTest {
        fake.addUnassigned("erin")
        val vm = list()
        fake.failNextDeleteWith(HttpStatusCode.InternalServerError, "internal_error")
        vm.askDelete("erin")

        vm.confirmDelete()!!.join()

        assertEquals(UsersNotice.DeleteFailed, vm.ui.value.notice)
        assertTrue(fake.usernames.contains("erin"))
        vm.dismissNotice()
        assertNull(vm.ui.value.notice)
    }
}
