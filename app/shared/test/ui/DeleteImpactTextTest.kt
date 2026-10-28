package com.usuallycoding.opensecretsanta.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteImpactTextTest {
    @Test
    fun everythingThatIsThereIsListedWithItsPlural() {
        assertEquals("This also deletes 12 wishlist items, 7 questions, 5 answers and the draw.", Strings.deleteImpactBody(12, 7, 5, draw = true))
    }

    @Test
    fun onesAreSingularAndZerosAreLeftOut() {
        assertEquals("This also deletes 1 wishlist item, 1 question and 1 answer.", Strings.deleteImpactBody(1, 1, 1, draw = false))
        assertEquals("This also deletes 3 wishlist items.", Strings.deleteImpactBody(3, 0, 0, draw = false))
        assertEquals("This also deletes the draw.", Strings.deleteImpactBody(0, 0, 0, draw = true))
        assertEquals("This also deletes 2 wishlist items and the draw.", Strings.deleteImpactBody(2, 0, 0, draw = true))
    }

    @Test
    fun nothingLinkedStillSaysSomethingSensible() {
        assertEquals("This also deletes everything in it.", Strings.deleteImpactBody(0, 0, 0, draw = false))
    }
}
