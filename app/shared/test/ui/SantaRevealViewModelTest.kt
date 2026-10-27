package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.SantaRevealDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.gift.SantaRevealViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SantaRevealViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private suspend fun viewModel(state: String): SantaRevealViewModel {
        server.gift.state = state
        return SantaRevealViewModel(1, server.api, immediateScope()).also { it.refresh().join() }
    }

    @Test
    fun anArchivedGroupRevealsTheSantaWithTheirUsernameAndFullName() = runTest {
        val santa = viewModel("archived").state.value.value!!

        assertEquals("carol", santa.username)
        assertEquals("Carol Santos", santa.fullName)
    }

    @Test
    fun aSantaWithoutAFullNameIsJustTheUsername() = runTest {
        server.gift.santa = SantaRevealDto("carol", "  ")

        val santa = viewModel("archived").state.value.value!!

        assertEquals("carol", santa.username)
        assertNull(santa.fullName)
    }

    @Test
    fun beforeTheGroupIsArchivedItIsNotYetNotAFailure() = runTest {
        val model = viewModel("active")

        assertNull(model.state.value.value)
        assertTrue(model.notYet)
    }

    @Test
    fun anUnreachableServerIsAFailureThatCanBeRetried() = runTest {
        server.gift.state = "archived"
        server.unreachable = true
        val model = SantaRevealViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        assertEquals(ApiError.Network, model.state.value.error)
        assertFalse(model.notYet)

        server.unreachable = false
        model.refresh().join()

        assertEquals("carol", model.state.value.value!!.username)
    }
}
