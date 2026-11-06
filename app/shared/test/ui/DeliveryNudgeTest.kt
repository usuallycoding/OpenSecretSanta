package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.MyDetails
import com.usuallycoding.opensecretsanta.ui.groups.showsDeliveryNudge
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeliveryNudgeTest {
    private fun details(
        fullName: String? = "Alice A",
        email: String? = "alice@example.org",
        phone: String? = "555-0101",
        address: String? = "1 Main St",
        deliveryInfo: String? = null,
    ) = MyDetails("alice", fullName, email, phone, address, deliveryInfo)

    private fun loaded(details: MyDetails) = LoadState(details, loading = false)

    @Test
    fun detailsAreCompleteWithTheFourRequiredFieldsAndNoDeliveryInformation() {
        assertTrue(details().isComplete)
        assertTrue(details(deliveryInfo = "Ring twice").isComplete)
    }

    @Test
    fun eachMissingRequiredFieldMakesThemIncomplete() {
        assertFalse(details(fullName = null).isComplete)
        assertFalse(details(email = null).isComplete)
        assertFalse(details(phone = null).isComplete)
        assertFalse(details(address = null).isComplete)
        assertFalse(MyDetails("alice", null, null, null, null, "Only this").isComplete)
        assertFalse(MyDetails("alice", null, null, null, null, null).isComplete)
    }

    @Test
    fun theMessageIsShownWhileTheGroupIsPendingOrActiveAndTheDetailsAreIncomplete() {
        for (phase in listOf(GroupPhase.Pending, GroupPhase.Active)) {
            assertTrue(showsDeliveryNudge(phase, loaded(details(phone = null))), "$phase")
            assertTrue(showsDeliveryNudge(phase, loaded(details(fullName = null, email = null, phone = null, address = null))), "$phase")
        }
    }

    @Test
    fun noMessageWhenTheDetailsAreComplete() {
        assertFalse(showsDeliveryNudge(GroupPhase.Pending, loaded(details())))
        assertFalse(showsDeliveryNudge(GroupPhase.Active, loaded(details(deliveryInfo = null))))
    }

    @Test
    fun noMessageOnceTheGroupIsArchivedOrWhenItsPhaseIsNotKnown() {
        assertFalse(showsDeliveryNudge(GroupPhase.Archived, loaded(details(phone = null))))
        assertFalse(showsDeliveryNudge(GroupPhase.Other, loaded(details(phone = null))))
        assertFalse(showsDeliveryNudge(null, loaded(details(phone = null))))
    }

    @Test
    fun noMessageWhileLoadingOrWithNothingLoaded() {
        assertFalse(showsDeliveryNudge(GroupPhase.Active, LoadState(null, loading = true)))
        assertFalse(showsDeliveryNudge(GroupPhase.Active, null))
    }

    @Test
    fun noMessageWhenLoadingFailedEvenIfAnEarlierLoadWasIncomplete() {
        assertFalse(showsDeliveryNudge(GroupPhase.Active, LoadState(null, loading = false, error = ApiError.Network)))
        assertFalse(showsDeliveryNudge(GroupPhase.Active, LoadState(details(phone = null), loading = false, error = ApiError.Network)))
    }

    @Test
    fun aReloadThatIsRunningKeepsTheMessageUntilItsAnswerIsIn() {
        assertEquals(true, showsDeliveryNudge(GroupPhase.Active, LoadState(details(phone = null), loading = true)))
    }
}
