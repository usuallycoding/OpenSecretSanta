package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.groups.GiftEntry
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.giftEntries
import com.usuallycoding.opensecretsanta.ui.groups.GroupStateInfo
import com.usuallycoding.opensecretsanta.ui.groups.formatWhen
import com.usuallycoding.opensecretsanta.ui.groups.groupPhase
import com.usuallycoding.opensecretsanta.ui.groups.groupStateInfo
import com.usuallycoding.opensecretsanta.ui.groups.isMe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupFormatTest {
    @Test
    fun aDateAndATimeAreReadable() {
        assertEquals("24 Dec 2026, 18:30", formatWhen("2026-12-24", "18:30"))
        assertEquals("5 Jan 2027, 09:05", formatWhen("2027-01-05", "9:05"))
    }

    @Test
    fun secondsInTheTimeAreLeftOut() {
        assertEquals("24 Dec 2026, 18:30", formatWhen("2026-12-24", "18:30:00"))
        assertEquals("24 Dec 2026, 18:30", formatWhen("2026-12-24", "18:30:15.250"))
    }

    @Test
    fun onlyWhatTheGroupHasIsShown() {
        assertEquals("24 Dec 2026", formatWhen("2026-12-24", null))
        assertEquals("24 Dec 2026", formatWhen("2026-12-24", "  "))
        assertEquals("18:30", formatWhen(null, "18:30"))
        assertNull(formatWhen(null, null))
        assertNull(formatWhen("", " "))
    }

    @Test
    fun everyMonthHasItsName() {
        val names = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        for ((index, name) in names.withIndex()) {
            val month = (index + 1).toString().padStart(2, '0')
            assertEquals("1 $name 2026", formatWhen("2026-$month-01", null))
        }
    }

    @Test
    fun anUnreadableValueIsShownAsItCameNeverAnError() {
        assertEquals("tomorrow, 18:30", formatWhen("tomorrow", "18:30"))
        assertEquals("24 Dec 2026, evening", formatWhen("2026-12-24", "evening"))
        assertEquals("2026-13-01", formatWhen("2026-13-01", null)) // no such month
        assertEquals("2026-00-01", formatWhen("2026-00-01", null))
    }

    @Test
    fun eachStateHasALabelAndANoteThatNamesNobody() {
        assertEquals(GroupStateInfo("Pending", "The organiser has not drawn the names yet."), groupStateInfo("pending"))
        assertEquals(GroupStateInfo("Active", "The names have been drawn."), groupStateInfo("active"))
        assertEquals(GroupStateInfo("Archived", "This exchange is over."), groupStateInfo("archived"))
        for (state in listOf("pending", "active", "archived")) {
            val note = groupStateInfo(state).note.orEmpty().lowercase()
            assertFalse("santa" in note || "receiver" in note || "gives to" in note, note)
        }
    }

    @Test
    fun theStateIsReadIgnoringCase() {
        assertEquals("Active", groupStateInfo("ACTIVE").label)
        assertEquals("Pending", groupStateInfo("Pending").label)
    }

    @Test
    fun anUnknownStateIsShownCapitalisedWithoutANote() {
        assertEquals(GroupStateInfo("Frozen", null), groupStateInfo("frozen"))
        assertEquals(GroupStateInfo("", null), groupStateInfo(""))
    }

    @Test
    fun usernamesAreComparedIgnoringCase() {
        assertTrue(isMe("alice", "alice"))
        assertTrue(isMe("Alice", "alice"))
        assertTrue(isMe("alice", "ALICE"))
        assertFalse(isMe("alice", "bob"))
        assertFalse(isMe("alice", "alice2"))
    }

    @Test
    fun theParticipantCountReadsNaturally() {
        assertEquals("1 participant", Strings.participantCount(1))
        assertEquals("4 participants", Strings.participantCount(4))
        assertEquals("0 participants", Strings.participantCount(0))
    }

    @Test
    fun aStateStringIsReadAsAPhaseIgnoringCase() {
        assertEquals(GroupPhase.Pending, groupPhase("pending"))
        assertEquals(GroupPhase.Active, groupPhase("ACTIVE"))
        assertEquals(GroupPhase.Archived, groupPhase("Archived"))
        assertEquals(GroupPhase.Other, groupPhase("draft"))
        assertEquals(GroupPhase.Other, groupPhase(""))
    }

    @Test
    fun yourGiftHasNothingBeforeTheDrawAndTheSantaOnlyOnceArchived() {
        assertEquals(emptyList(), giftEntries(GroupPhase.Pending))
        assertEquals(emptyList(), giftEntries(GroupPhase.Other))
        assertEquals(listOf(GiftEntry.Receiver, GiftEntry.SantaQuestions), giftEntries(GroupPhase.Active))
        assertEquals(listOf(GiftEntry.Receiver, GiftEntry.SantaQuestions, GiftEntry.Santa), giftEntries(GroupPhase.Archived))
    }
}
