package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.admin.AdminParticipant
import com.usuallycoding.opensecretsanta.ui.admin.marked
import kotlin.test.Test
import kotlin.test.assertEquals

class ParticipantMarksTest {
    private val complete = AdminParticipant("alice", hasAccount = true, profileComplete = true)
    private val incomplete = AdminParticipant("bob", hasAccount = true, profileComplete = false)
    private val noAccount = AdminParticipant("carol-new", hasAccount = false, profileComplete = false)

    @Test
    fun inAPendingGroupAMissingAccountAndIncompleteDeliveryInfoAreMarked() {
        assertEquals(listOf(false, true, true), listOf(complete, incomplete, noAccount).map { it.marked(draft = false) })
    }

    @Test
    fun inADraftOnlyAMissingAccountIsMarkedBecauseNobodyCanFillDeliveryInfoYet() {
        assertEquals(listOf(false, false, true), listOf(complete, incomplete, noAccount).map { it.marked(draft = true) })
    }
}
