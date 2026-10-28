package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupsViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminPhase
import com.usuallycoding.opensecretsanta.ui.admin.adminPhase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
