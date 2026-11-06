package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.groups.MyDetailsViewModel
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.PairViewModel
import com.usuallycoding.opensecretsanta.ui.groups.showsDeliveryNudge
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        server.deliveryInfoBody = """{"fullName":"Alice A","address":"1 Main St","phone":"  "}"""
        val model = MyDetailsViewModel(1, "alice", server.api, immediateScope()).also { it.refresh().join() }

        val details = model.details.value.value!!
        assertEquals("alice", details.username)
        assertEquals("Alice A", details.fullName)
        assertEquals("1 Main St", details.address)
        assertNull(details.phone)
        assertTrue(server.calls.contains("GET /groups/1/delivery-info"))
    }

    private suspend fun myDetails() = MyDetailsViewModel(1, "alice", server.api, immediateScope()).also { it.refresh().join() }

    private val completeInfo = """{"fullName":"Alice A","email":"alice@example.org","phone":"555","address":"1 Main St"}"""

    @Test
    fun anIncompleteLoadAsksForTheDeliveryInfoInAPendingAndAnActiveGroup() = runTest {
        server.deliveryInfoBody = """{"fullName":"Alice A","address":"1 Main St"}"""
        val model = myDetails()

        assertTrue(showsDeliveryNudge(GroupPhase.Pending, model.details.value))
        assertTrue(showsDeliveryNudge(GroupPhase.Active, model.details.value))
        assertFalse(showsDeliveryNudge(GroupPhase.Archived, model.details.value))
    }

    @Test
    fun aCompleteLoadAsksForNothingEvenWithoutDeliveryInformation() = runTest {
        server.deliveryInfoBody = completeInfo

        assertFalse(showsDeliveryNudge(GroupPhase.Active, myDetails().details.value))
    }

    @Test
    fun aFailedLoadAsksForNothing() = runTest {
        server.deliveryInfoBody = """{"fullName":"Alice A"}"""
        server.unreachable = true
        val model = myDetails()

        assertFalse(showsDeliveryNudge(GroupPhase.Active, model.details.value))
    }

    @Test
    fun aFailedReloadHidesTheMessageInsteadOfKeepingAWrongOne() = runTest {
        server.deliveryInfoBody = """{"fullName":"Alice A"}"""
        val model = myDetails()
        assertTrue(showsDeliveryNudge(GroupPhase.Active, model.details.value))

        server.unreachable = true
        model.refresh().join()

        assertFalse(showsDeliveryNudge(GroupPhase.Active, model.details.value))
    }

    @Test
    fun theMessageGoesWhenTheServerHasTheCompleteDetailsAfterTheNextReload() = runTest {
        server.deliveryInfoBody = """{"fullName":"Alice A"}"""
        val model = myDetails()
        assertTrue(showsDeliveryNudge(GroupPhase.Pending, model.details.value))

        server.deliveryInfoBody = completeInfo
        model.refresh().join()

        assertFalse(showsDeliveryNudge(GroupPhase.Pending, model.details.value))
        // the first load and the reload, however many times the view model refreshed itself on the way
        assertTrue(server.calls.count { it == "GET /groups/1/delivery-info" } >= 2)
    }
}
