package com.usuallycoding.opensecretsanta.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AdminDrawLabelTest {
    @Test
    fun theLabelAboveThePairsSaysHowManyParticipantsThereAre() {
        assertEquals("Who gives to whom (4 participants)", Strings.adminDraw(4))
        assertEquals("Who gives to whom (13 participants)", Strings.adminDraw(13))
    }
}
