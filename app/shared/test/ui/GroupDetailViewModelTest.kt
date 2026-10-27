package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.groups.GroupDetail
import com.usuallycoding.opensecretsanta.ui.groups.GroupDetailViewModel
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.GroupStateInfo
import com.usuallycoding.opensecretsanta.ui.groups.ParticipantItem
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupDetailViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private val full = """{"id":7,"name":"Office party","description":"Bring cake","location":"Town Hall","date":"2026-12-24","time":"18:30","state":"active","participants":["alice","bob","carol","dave"]}"""

    /** The first load runs on the client's own threads, so wait for it before looking at the state. */
    private suspend fun viewModel(me: String = "alice") = GroupDetailViewModel(7, me, server.api, immediateScope()).also { it.refresh().join() }

    @Test
    fun aFullGroupShowsEverythingTheDetailNeeds() = runTest {
        server.groupAnswer = { HttpStatusCode.OK to full }

        val detail = viewModel().state.value.value

        assertEquals(
            GroupDetail(
                name = "Office party",
                state = GroupStateInfo("Active", "The names have been drawn."),
                phase = GroupPhase.Active,
                description = "Bring cake",
                whenText = "24 Dec 2026, 18:30",
                location = "Town Hall",
                participants = listOf(
                    ParticipantItem("alice", isMe = true),
                    ParticipantItem("bob", isMe = false),
                    ParticipantItem("carol", isMe = false),
                    ParticipantItem("dave", isMe = false),
                ),
            ),
            detail,
        )
    }

    @Test
    fun aGroupWithOnlyANameLeavesTheRestOut() = runTest {
        server.groupAnswer = { HttpStatusCode.OK to """{"id":7,"name":"Bare group","state":"pending","participants":["alice","bob","carol","dave"]}""" }

        val detail = viewModel().state.value.value!!

        assertNull(detail.description)
        assertNull(detail.whenText)
        assertNull(detail.location)
        assertEquals("Pending", detail.state.label)
        assertEquals("The organiser has not drawn the names yet.", detail.state.note)
    }

    @Test
    fun blankDescriptionAndLocationCountAsMissing() = runTest {
        server.groupAnswer = { HttpStatusCode.OK to """{"id":7,"name":"Group","description":"  ","location":"","state":"archived","participants":["alice"]}""" }

        val detail = viewModel().state.value.value!!

        assertNull(detail.description)
        assertNull(detail.location)
    }

    @Test
    fun theOwnUsernameIsMarkedWhateverItsCase() = runTest {
        server.groupAnswer = { HttpStatusCode.OK to full }

        val mine = viewModel(me = "BOB").state.value.value!!.participants.filter { it.isMe }

        assertEquals(listOf("bob"), mine.map { it.username })
    }

    @Test
    fun aGroupThatIsGoneIsReportedAsGoneNotAsAFailure() = runTest {
        server.groupAnswer = { HttpStatusCode.NotFound to """{"code":"not_found","message":"Group not found."}""" }

        val state = viewModel().state.value

        assertTrue(state.isGone())
        assertNull(state.value)
        assertTrue(state.error is ApiError.Rejected)
    }

    @Test
    fun otherErrorsAreNotGone() = runTest {
        server.groupAnswer = { HttpStatusCode.InternalServerError to """{"code":"internal_error","message":"x"}""" }

        assertFalse(viewModel().state.value.isGone())
    }

    @Test
    fun aGroupRemovedWhileOpenBecomesGoneOnTheNextRefresh() = runTest {
        server.groupAnswer = { HttpStatusCode.OK to full }
        val model = viewModel()
        assertFalse(model.state.value.isGone())

        server.groupAnswer = { HttpStatusCode.NotFound to """{"code":"not_found","message":"Group not found."}""" }
        model.refresh().join()

        assertTrue(model.state.value.isGone())
    }

    @Test
    fun anUnreachableServerCanBeRetried() = runTest {
        server.groupAnswer = { HttpStatusCode.OK to full }
        server.unreachable = true
        val model = viewModel()

        assertEquals(ApiError.Network, model.state.value.error)
        assertFalse(model.state.value.isGone())

        server.unreachable = false
        model.refresh().join()

        assertNull(model.state.value.error)
        assertEquals("Office party", model.state.value.value!!.name)
    }

    @Test
    fun itAsksForTheRightGroup() = runTest {
        server.groupAnswer = { HttpStatusCode.OK to full }

        viewModel()

        assertTrue(server.calls.isNotEmpty() && server.calls.all { it == "GET /groups/7" }, server.calls.toString())
    }

    @Test
    fun theGroupsPhaseFollowsItsState() = runTest {
        for ((state, phase) in listOf("pending" to GroupPhase.Pending, "active" to GroupPhase.Active, "archived" to GroupPhase.Archived, "frozen" to GroupPhase.Other)) {
            server.groupAnswer = { HttpStatusCode.OK to """{"id":7,"name":"Group","state":"$state","participants":["alice"]}""" }

            assertEquals(phase, viewModel().state.value.value!!.phase, state)
        }
    }
}
