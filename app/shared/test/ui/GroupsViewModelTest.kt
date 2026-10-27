package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.groups.GroupRow
import com.usuallycoding.opensecretsanta.ui.groups.GroupStateInfo
import com.usuallycoding.opensecretsanta.ui.groups.GroupsViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupsViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private fun group(id: Int, name: String, state: String = "active", date: String? = "2026-12-24", time: String? = "18:30", location: String? = "Town Hall", participants: Int = 4): String {
        fun field(key: String, value: String?) = if (value == null) "" else ""","$key":"$value""""
        val names = (1..participants).joinToString(",") { "\"user$it\"" }
        return """{"id":$id,"name":"$name","state":"$state"${field("date", date)}${field("time", time)}${field("location", location)},"participants":[$names]}"""
    }

    private fun answer(vararg groups: String) {
        server.groupsAnswer = HttpStatusCode.OK to groups.joinToString(",", "[", "]")
    }

    /** The first load runs on the client's own threads, so wait for it before looking at the state. */
    private suspend fun viewModel() = GroupsViewModel(server.api, immediateScope()).also { it.refresh().join() }

    @Test
    fun aListedGroupShowsEverythingTheRowNeeds() = runTest {
        answer(group(7, "Office party"))

        val rows = viewModel().state.value.value!!

        assertEquals(
            listOf(GroupRow(7, "Office party", GroupStateInfo("Active", "The names have been drawn."), "24 Dec 2026, 18:30", "Town Hall", 4)),
            rows,
        )
    }

    @Test
    fun groupsKeepTheOrderTheServerGaveThem() = runTest {
        answer(group(1, "Zebra", state = "archived"), group(2, "Apple", state = "pending"), group(3, "Mango"))

        assertEquals(listOf("Zebra", "Apple", "Mango"), viewModel().state.value.value!!.map { it.name })
    }

    @Test
    fun noGroupsIsAnEmptyListNotAnError() = runTest {
        answer()

        val state = viewModel().state.value

        assertEquals(emptyList(), state.value)
        assertNull(state.error)
        assertTrue(!state.loading)
    }

    @Test
    fun aGroupWithoutDateTimeOrLocationShowsOnlyWhatItHas() = runTest {
        answer(group(1, "Quiet group", state = "pending", date = null, time = null, location = null, participants = 1))

        val row = viewModel().state.value.value!!.single()

        assertNull(row.whenText)
        assertNull(row.location)
        assertEquals(1, row.participantCount)
        assertEquals("Pending", row.state.label)
    }

    @Test
    fun aDateWithoutATimeShowsTheDateOnly() = runTest {
        answer(group(1, "Day trip", time = null))

        assertEquals("24 Dec 2026", viewModel().state.value.value!!.single().whenText)
    }

    @Test
    fun anUnreachableServerCanBeRetried() = runTest {
        answer(group(1, "Office party"))
        server.unreachable = true
        val model = viewModel()

        assertEquals(ApiError.Network, model.state.value.error)
        assertNull(model.state.value.value)

        server.unreachable = false
        model.refresh().join()

        assertNull(model.state.value.error)
        assertEquals("Office party", model.state.value.value!!.single().name)
    }

    @Test
    fun aRefreshShowsAStateThatChangedOnTheServer() = runTest {
        answer(group(1, "Office party", state = "pending"))
        val model = viewModel()
        assertEquals("Pending", model.state.value.value!!.single().state.label)

        answer(group(1, "Office party", state = "active"))
        model.refresh().join()

        assertEquals("Active", model.state.value.value!!.single().state.label)
    }

    @Test
    fun aFailedRefreshKeepsTheListThatWasShown() = runTest {
        answer(group(1, "Office party"))
        val model = viewModel()

        server.unreachable = true
        model.refresh().join()

        assertEquals("Office party", model.state.value.value!!.single().name)
        assertEquals(ApiError.Network, model.state.value.error)
    }
}
