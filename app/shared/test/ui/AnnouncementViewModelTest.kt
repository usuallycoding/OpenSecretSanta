package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.AnnouncementRequest
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.contract.PushSummaryDto
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AnnouncementViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnnouncementViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private fun account(email: Boolean, push: Boolean) {
        server.accountBody = { """{"username":"admin","isAdmin":true,"mustChangePassword":false,"emailAvailable":$email,"pushAvailable":$push}""" }
    }

    private suspend fun viewModel(email: Boolean = true, push: Boolean = true): AnnouncementViewModel {
        account(email, push)
        return AnnouncementViewModel(7, "Office Party", server.api, immediateScope()).also { it.refresh().join() }
    }

    private fun AnnouncementViewModel.fill(title: String = "Schedule change", body: String = "We start at 19:00.") {
        setTitle(title)
        setBody(body)
    }

    // --- the channels ---

    @Test
    fun bothChannelsStartCheckedWhenTheServerHasThem() = runTest {
        val ui = viewModel().ui.value

        assertTrue(ui.ready && ui.email && ui.push)
    }

    @Test
    fun aChannelTheServerLacksIsNotChosenAndCannotBe() = runTest {
        val vm = viewModel(email = false)

        vm.setEmail(true)

        assertFalse(vm.ui.value.email)
        assertTrue(vm.ui.value.push)
    }

    @Test
    fun withNeitherChannelSendIsOff() = runTest {
        val vm = viewModel(email = false, push = false)
        vm.fill()

        assertFalse(vm.canSend)
        vm.ask()
        assertFalse(vm.ui.value.confirming)
    }

    @Test
    fun unchoosingTheOnlyChannelTurnsSendOff() = runTest {
        val vm = viewModel(push = false)
        vm.fill()
        assertTrue(vm.canSend)

        vm.setEmail(false)

        assertFalse(vm.canSend)
    }

    @Test
    fun anAccountThatCannotBeReadLeavesSendOffUntilItIs() = runTest {
        server.unreachable = true
        val vm = AnnouncementViewModel(7, "Office Party", server.api, immediateScope()).also { it.refresh().join() }
        vm.fill()
        assertFalse(vm.ui.value.ready)
        assertFalse(vm.canSend)

        server.unreachable = false
        account(email = true, push = true)
        vm.refresh().join()

        assertTrue(vm.canSend)
    }

    // --- the fields ---

    @Test
    fun sendNeedsATitleAndAMessage() = runTest {
        val vm = viewModel()
        assertFalse(vm.canSend)
        vm.setTitle("Schedule change")
        assertFalse(vm.canSend)
        vm.setBody("  ")
        assertFalse(vm.canSend)
        vm.setBody("Hello")
        assertTrue(vm.canSend)
    }

    @Test
    fun theFieldsStopAtTheirLimitsAndTheTitleStaysOnOneLine() = runTest {
        val vm = viewModel()

        vm.setTitle("a".repeat(150))
        vm.setBody("b".repeat(2500))
        assertEquals(100, vm.title.length)
        assertEquals(2000, vm.body.length)

        vm.setTitle("two\nlines")
        assertEquals("two lines", vm.title)
    }

    @Test
    fun theSubjectReadsTitleColonGroup() = runTest {
        val vm = viewModel()
        vm.setTitle(" Schedule change ")

        assertEquals("Schedule change: Office Party", vm.subject())
    }

    @Test
    fun aMessageOverThreeHundredCharactersWarnsOnlyWhenPushIsChosen() = runTest {
        val vm = viewModel()
        vm.setBody("x".repeat(301))
        assertTrue(vm.pushCutsMessage)

        vm.setPush(false)
        assertFalse(vm.pushCutsMessage)
        vm.setPush(true)
        vm.setBody("x".repeat(300))
        assertFalse(vm.pushCutsMessage)
    }

    // --- confirming and sending ---

    @Test
    fun nothingIsSentUntilItIsConfirmed() = runTest {
        val vm = viewModel()
        vm.fill()

        vm.ask()
        assertTrue(vm.ui.value.confirming)
        assertTrue(server.announcements.requests.isEmpty())

        vm.cancelConfirm()
        assertFalse(vm.ui.value.confirming)
        assertEquals("Schedule change", vm.title) // still filled in
        assertTrue(server.announcements.requests.isEmpty())
    }

    @Test
    fun confirmingSendsTheTrimmedTextAndTheChosenChannelsAndShowsBothSummaries() = runTest {
        val vm = viewModel()
        vm.fill(title = " Schedule change ", body = " We start at 19:00. ")
        vm.ask()

        vm.send().join()

        assertEquals(listOf(AnnouncementRequest("Schedule change", "We start at 19:00.", email = true, push = true)), server.announcements.requests)
        assertEquals("POST /admin/groups/7/announcements", server.calls.last())
        val ui = vm.ui.value
        assertFalse(ui.sending || ui.confirming)
        assertEquals(EmailSummaryDto(4, 0, 0), ui.result!!.emails)
        assertEquals(PushSummaryDto(3, 1), ui.result!!.push)
    }

    @Test
    fun onlyTheChosenChannelIsAskedForAndSummarised() = runTest {
        val vm = viewModel()
        vm.fill()
        vm.setEmail(false)
        vm.ask()

        vm.send().join()

        assertEquals(AnnouncementRequest("Schedule change", "We start at 19:00.", email = false, push = true), server.announcements.requests.single())
        assertNull(vm.ui.value.result!!.emails)
        assertNotNull(vm.ui.value.result!!.push)
    }

    @Test
    fun aRefusedAnnouncementShowsTheServersMessageAndKeepsTheText() = runTest {
        val vm = viewModel()
        vm.fill()
        server.announcements.failNextWith(HttpStatusCode.Conflict, "invalid_state", "Announcements can only be sent to a pending or active group.")
        vm.ask()

        vm.send().join()

        val ui = vm.ui.value
        assertEquals("Announcements can only be sent to a pending or active group.", ui.error)
        assertEquals("Schedule change", vm.title)
        assertEquals("We start at 19:00.", vm.body)
        assertNull(ui.result)
        assertFalse(ui.sending)
    }

    @Test
    fun aServerThatCannotBeReachedIsSaidPlainlyAndTheTextIsKept() = runTest {
        val vm = viewModel()
        vm.fill()
        vm.ask()
        server.unreachable = true

        vm.send().join()

        assertEquals(Strings.SERVER_UNREACHABLE, vm.ui.value.error)
        assertEquals("Schedule change", vm.title)
    }

    @Test
    fun editingAfterAnErrorClearsIt() = runTest {
        val vm = viewModel()
        vm.fill()
        server.announcements.failNextWith(HttpStatusCode.BadRequest, "validation_failed", "Title must be 1 to 100 characters long.", "title")
        vm.ask()
        vm.send().join()
        assertNotNull(vm.ui.value.error)

        vm.setTitle("Better")

        assertNull(vm.ui.value.error)
    }

    // --- the button ---

    @Test
    fun theOwnerMayAnnounceInPendingAndActiveGroupsOnly() = runTest {
        for ((state, allowed) in listOf("draft" to false, "pending" to true, "active" to true, "archived" to false)) {
            val id = server.adminGroups.add("Party $state", state = state, participants = listOf("alice", "bob", "carol", "dave"))
            val vm = AdminGroupViewModel(id, "admin", server.api, immediateScope()).also { it.refresh().join() }
            assertEquals(allowed, vm.canAnnounce, state)
        }
    }
}
