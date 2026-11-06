package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.admin.OversightDeliveryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MissingDeliveryFieldsTest {
    private fun info(
        fullName: String? = "Bob Brown",
        email: String? = "bob@example.com",
        phone: String? = "0700",
        address: String? = "1 Main St",
        deliveryInfo: String? = null,
    ) = OversightDeliveryInfo(fullName, email, phone, address, deliveryInfo)

    @Test
    fun someMissingAreNamedInTheOrderTheyAreAsked() {
        val partial = info(email = null, phone = null)

        assertEquals(listOf("email", "phone"), partial.missing)
        assertEquals("Missing: email, phone", partial.missingLine)
    }

    @Test
    fun eachRequiredFieldCanBeTheOneMissing() {
        assertEquals("Missing: full name", info(fullName = null).missingLine)
        assertEquals("Missing: email", info(email = null).missingLine)
        assertEquals("Missing: phone", info(phone = null).missingLine)
        assertEquals("Missing: address", info(address = null).missingLine)
    }

    @Test
    fun noLineWhenAllFourAreSetEvenWithoutDeliveryInformation() {
        assertEquals(emptyList(), info().missing)
        assertNull(info().missingLine)
        assertNull(info(deliveryInfo = "Ring twice").missingLine)
    }

    @Test
    fun noLineWhenNothingIsSetBecauseTheSheetSaysSoInItsOwnSentence() {
        val nothing = info(null, null, null, null, null)

        assertEquals(listOf("full name", "email", "phone", "address"), nothing.missing)
        assertNull(nothing.missingLine)
    }

    @Test
    fun onlyDeliveryInformationSetStillNamesAllFourAsMissing() {
        assertEquals("Missing: full name, email, phone, address", info(null, null, null, null, "Ring twice").missingLine)
    }
}
