package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupsViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminPhase
import com.usuallycoding.opensecretsanta.ui.admin.copyName
import com.usuallycoding.opensecretsanta.ui.admin.adminPhase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.http.HttpStatusCode
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminGroupsViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private suspend fun viewModel() = AdminGroupsViewModel(server.api, immediateScope()).also { it.refresh().join() }

    @Test
    fun theOwnGroupsAreListedInTheOrderTheServerReturnsThem() = runTest {
        server.adminGroups.add("Office party", state = "active", participants = listOf("alice", "bob"))
        server.adminGroups.add("Hiking trip")
        server.adminGroups.addForeign("Not mine")

        val groups = viewModel().state.value.value!!

        assertEquals(listOf("Office party", "Hiking trip"), groups.map { it.name })
        assertEquals(listOf(AdminPhase.Active, AdminPhase.Draft), groups.map { it.phase })
        assertEquals(listOf("Active", "Draft"), groups.map { it.state.label })
        assertEquals(2, groups.first().participants.size)
    }

    @Test
    fun noGroupsIsAnEmptyListNotAnError() = runTest {
        val state = viewModel().state.value

        assertEquals(emptyList(), state.value!!.let { it })
        assertNull(state.error)
    }

    @Test
    fun anUnreachableServerCanBeRetried() = runTest {
        server.adminGroups.add("Office party")
        server.unreachable = true
        val model = AdminGroupsViewModel(server.api, immediateScope()).also { it.refresh().join() }

        assertEquals(ApiError.Network, model.state.value.error)
        assertNull(model.state.value.value)

        server.unreachable = false
        model.refresh().join()

        assertEquals(listOf("Office party"), model.state.value.value!!.map { it.name })
    }

    @Test
    fun aFailedRefreshKeepsTheListAndSaysSo() = runTest {
        server.adminGroups.add("Office party")
        val model = viewModel()
        server.unreachable = true

        model.refresh().join()

        assertEquals(listOf("Office party"), model.state.value.value!!.map { it.name })
        assertEquals(ApiError.Network, model.state.value.error)
    }

    @Test
    fun aStateIsReadAsAPhaseIgnoringCaseAndAnUnknownOneIsOther() {
        assertEquals(AdminPhase.Draft, adminPhase("DRAFT"))
        assertEquals(AdminPhase.Pending, adminPhase("pending"))
        assertEquals(AdminPhase.Active, adminPhase("active"))
        assertEquals(AdminPhase.Archived, adminPhase("archived"))
        assertEquals(AdminPhase.Other, adminPhase("frozen"))
    }

    // --- duplicating -----------------------------------------------------------------------------------

    @Test
    fun aCopyIsNamedAfterTheOriginal() {
        assertEquals("Copy of Office party", copyName("Office party"))
    }

    @Test
    fun aCopyNameIsCutToTheLimitWithAnEllipsis() {
        val fits = "x".repeat(92)
        assertEquals("Copy of $fits", copyName(fits))
        assertEquals(100, copyName(fits).length)

        val cut = copyName("x".repeat(93))
        assertEquals(100, cut.length)
        assertTrue(cut.startsWith("Copy of x"))
        assertTrue(cut.endsWith("…"))
        assertEquals(100, copyName("y".repeat(100)).length)
    }

    @Test
    fun confirmingMakesADraftWithTheDetailsAndParticipantsOfAnyGroup() = runTest {
        val id = server.adminGroups.add("Party", state = "active", participants = listOf("alice", "bob", "newcomer"))
        server.adminGroups.group(id).apply {
            description = "Yearly"; location = "Office"; date = "2026-12-24"; time = "18:30"
        }
        val model = viewModel()

        model.askDuplicate(model.state.value.value!!.first())
        assertEquals("Copy of Party", model.duplicate.value!!.copyName)
        model.confirmDuplicate()?.join()

        val copy = server.adminGroups.group(model.created.value!!)
        assertEquals("Copy of Party", copy.name)
        assertEquals("draft", copy.state)
        assertEquals(listOf("alice", "bob", "newcomer"), copy.participants)
        assertEquals("Yearly", copy.description)
        assertEquals("Office", copy.location)
        assertEquals("2026-12-24", copy.date)
        assertEquals("18:30", copy.time)
        assertNull(model.duplicate.value)
        // the original is as it was
        assertEquals("active", server.adminGroups.group(id).state)
        assertEquals("Party", server.adminGroups.group(id).name)
    }

    @Test
    fun cancellingCreatesNothing() = runTest {
        server.adminGroups.add("Party")
        val model = viewModel()

        model.askDuplicate(model.state.value.value!!.first())
        model.cancelDuplicate()

        assertNull(model.duplicate.value)
        assertNull(model.created.value)
        assertEquals(1, server.adminGroups.ids.size)
    }

    @Test
    fun anUnreachableServerLeavesTheDialogOpenWithAMessageAndCreatesNothing() = runTest {
        server.adminGroups.add("Party")
        val model = viewModel()
        model.askDuplicate(model.state.value.value!!.first())
        server.unreachable = true

        model.confirmDuplicate()?.join()

        assertEquals(Strings.SERVER_UNREACHABLE, model.duplicate.value!!.error)
        assertFalse(model.duplicate.value!!.busy)
        assertNull(model.created.value)
        server.unreachable = false
        assertEquals(1, server.adminGroups.ids.size)
    }

    @Test
    fun aRefusedCopyShowsTheServersMessage() = runTest {
        server.adminGroups.add("Party")
        val model = viewModel()
        model.askDuplicate(model.state.value.value!!.first())
        server.adminGroups.failNextWriteWith(HttpStatusCode.BadRequest, "validation_failed", "Name is not allowed.", "name")

        model.confirmDuplicate()?.join()

        assertNotNull(model.duplicate.value)
        assertEquals("Name is not allowed.", model.duplicate.value!!.error)
        assertNull(model.created.value)
    }

    @Test
    fun theCreatedGroupIsHandedOverOnce() = runTest {
        server.adminGroups.add("Party")
        val model = viewModel()
        model.askDuplicate(model.state.value.value!!.first())
        model.confirmDuplicate()?.join()
        assertNotNull(model.created.value)

        model.createdHandled()

        assertNull(model.created.value)
    }
    @Test
    fun theChipsAreEveryStateIncludingDraft() = runTest {
        assertEquals(listOf("draft", "pending", "active", "archived"), viewModel().filter.chips.map { it.key })
    }

    @Test
    fun theSearchAndTheChipsSurviveAReloadAndNarrowTheLoadedGroups() = runTest {
        server.adminGroups.add("Office party", state = "active")
        server.adminGroups.add("Office lunch")
        server.adminGroups.add("Hiking trip", state = "active")
        val model = viewModel()
        model.filter.setQuery("office")
        model.filter.toggle("draft")

        model.refresh().join()

        assertEquals("office", model.filter.query)
        assertEquals(listOf("Office lunch"), model.filter.apply(model.state.value.value!!).map { it.name })
        assertEquals(3, model.state.value.value!!.size, "the loaded list itself is whole")
    }

}
