package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.groups.MyDetailsViewModel
import com.usuallycoding.opensecretsanta.ui.groups.PairViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PairViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    @Test
    fun theReceiverRowKnowsTheReceiversUsernameAndDetailsOnceTheNamesAreDrawn() = runTest {
        server.gift.state = "active"
        val model = PairViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        val receiver = model.receiver.value.value!!
        assertEquals("bob", receiver.username)
        assertEquals("1 Main Street", receiver.address)
    }

    @Test
    fun anUnreachableServerIsAFailureThatCanBeRetried() = runTest {
        server.gift.state = "active"
        server.unreachable = true
        val model = PairViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        assertNull(model.receiver.value.value)
        assertTrue(model.receiver.value.error != null)

        server.unreachable = false
        model.refresh().join()
        assertEquals("bob", model.receiver.value.value!!.username)
    }

    @Test
    fun aRefusedRequestBeforeTheDrawIsNotAReceiver() = runTest {
        server.gift.state = "pending"
        val model = PairViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        assertNull(model.receiver.value.value)
        assertTrue(model.receiver.value.error != null)
    }

    @Test
    fun ownDetailsAreLoadedWithBlankValuesLeftOut() = runTest {
        val model = MyDetailsViewModel(server.api, immediateScope()).also { it.refresh().join() }

        val details = model.details.value.value!!
        assertEquals("alice", details.username)
    }
}
