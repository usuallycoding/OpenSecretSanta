package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.ui.admin.AdminParticipant
import com.usuallycoding.opensecretsanta.ui.admin.Mark
import com.usuallycoding.opensecretsanta.ui.admin.MarkCounts
import com.usuallycoding.opensecretsanta.ui.admin.ParticipantMarks
import com.usuallycoding.opensecretsanta.ui.admin.legendMarks
import com.usuallycoding.opensecretsanta.ui.admin.markCountLine
import com.usuallycoding.opensecretsanta.ui.admin.markCounts
import com.usuallycoding.opensecretsanta.ui.admin.markedText
import com.usuallycoding.opensecretsanta.ui.admin.marks
import com.usuallycoding.opensecretsanta.ui.admin.pairTitle
import com.usuallycoding.opensecretsanta.ui.admin.receiverMarks
import com.usuallycoding.opensecretsanta.ui.admin.santaMarks
import com.usuallycoding.opensecretsanta.ui.admin.toPair
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParticipantMarksTest {
    private val fine = AdminParticipant("alice", hasAccount = true, profileComplete = true)
    private val incomplete = AdminParticipant("bob", hasAccount = true, profileComplete = false)
    private val temporary = AdminParticipant("carol", hasAccount = true, profileComplete = true, mustChangePassword = true)
    private val both = AdminParticipant("dave", hasAccount = true, profileComplete = false, mustChangePassword = true)
    private val noAccount = AdminParticipant("erin-new", hasAccount = false, profileComplete = false)

    @Test
    fun afterTheDraftEachMarkIsItsOwn() {
        assertEquals(ParticipantMarks(), fine.marks(draft = false))
        assertEquals(ParticipantMarks(incompleteInfo = true), incomplete.marks(draft = false))
        assertEquals(ParticipantMarks(temporaryPassword = true), temporary.marks(draft = false))
        assertEquals(ParticipantMarks(temporaryPassword = true, incompleteInfo = true), both.marks(draft = false))
    }

    @Test
    fun aPersonWithNoAccountHasOnlyTheNoAccountMark() {
        assertEquals(ParticipantMarks(noAccount = true), noAccount.marks(draft = false))
        assertEquals(ParticipantMarks(noAccount = true), AdminParticipant("x", false, false, mustChangePassword = true).marks(draft = false))
    }

    @Test
    fun inADraftOnlyAMissingAccountIsMarked() {
        assertEquals(listOf(false, false, false, false, true), listOf(fine, incomplete, temporary, both, noAccount).map { it.marks(draft = true).any })
        assertEquals(ParticipantMarks(noAccount = true), noAccount.marks(draft = true))
    }

    @Test
    fun theLegendExplainsOnlyTheMarksThatAreThereInItsOwnOrder() {
        val all = listOf(fine, incomplete, temporary, noAccount).map { it.marks(draft = false) }

        assertEquals(listOf(Mark.NoAccount, Mark.TemporaryPassword, Mark.IncompleteInfo), legendMarks(all))
        assertEquals(listOf(Mark.TemporaryPassword), legendMarks(listOf(fine, temporary).map { it.marks(draft = false) }))
        assertEquals(emptyList(), legendMarks(listOf(fine).map { it.marks(draft = false) }))
    }

    @Test
    fun theCountsCountPeopleNotMarksAndBothIconsCountInBoth() {
        val counts = markCounts(listOf(fine, incomplete, temporary, both, noAccount), draft = false)

        assertEquals(MarkCounts(temporaryPassword = 2, incompleteInfo = 2), counts)
    }

    @Test
    fun aDraftHasNothingToCount() {
        assertEquals(MarkCounts(0, 0), markCounts(listOf(incomplete, temporary, both, noAccount), draft = true))
    }

    @Test
    fun theCountLineLeavesOutAZeroAndIsAbsentWhenBothAreZero() {
        assertEquals("2 on a temporary password, 3 with delivery info incomplete", markCountLine(MarkCounts(2, 3)))
        assertEquals("1 on a temporary password", markCountLine(MarkCounts(1, 0)))
        assertEquals("4 with delivery info incomplete", markCountLine(MarkCounts(0, 4)))
        assertNull(markCountLine(MarkCounts(0, 0)))
    }

    @Test
    fun aUsernameIsFollowedByItsMarksAndTheOwnersByYou() {
        assertEquals("bob", markedText("bob", false, ParticipantMarks()).text)
        assertEquals("bob (you)", markedText("bob", true, ParticipantMarks()).text)
        assertEquals("erin-new *", markedText("erin-new", false, ParticipantMarks(noAccount = true)).text)
        assertEquals("bob temporary password", markedText("bob", false, ParticipantMarks(temporaryPassword = true)).text)
        assertEquals("bob (you) temporary password delivery info incomplete", markedText("bob", true, ParticipantMarks(temporaryPassword = true, incompleteInfo = true)).text)
    }

    @Test
    fun aReceiversMarksComeBeforeTheNameInTheMirrorOrder() {
        val marks = ParticipantMarks(temporaryPassword = true, incompleteInfo = true)

        assertEquals("delivery info incomplete temporary password bob", markedText("bob", false, marks, marksBefore = true).text)
        assertEquals("temporary password bob (you)", markedText("bob", true, ParticipantMarks(temporaryPassword = true), marksBefore = true).text)
        assertEquals("bob", markedText("bob", false, ParticipantMarks(), marksBefore = true).text)
    }

    @Test
    fun theIconsCarryTheirMeaningAsTheTextAScreenReaderReads() {
        val text = markedText("bob", false, ParticipantMarks(temporaryPassword = true))

        // what follows the name is the words of the mark, not a glyph, so a screen reader says "temporary password"
        assertTrue(text.text.endsWith("temporary password"))
        assertFalse("*" in text.text)
    }

    @Test
    fun aPairsTitleNamesBothSidesWithTheirMarks() {
        val title = pairTitle(markedText("alice", false, ParticipantMarks()), markedText("bob", false, ParticipantMarks(incompleteInfo = true)))

        assertEquals("alice gives to bob delivery info incomplete", title.text)

        val receiverFirst = pairTitle(
            markedText("alice", false, ParticipantMarks(temporaryPassword = true)),
            markedText("bob", false, ParticipantMarks(incompleteInfo = true), marksBefore = true),
        )

        assertEquals("alice temporary password gives to delivery info incomplete bob", receiverFirst.text)
    }

    @Test
    fun theSidesOfAPairCarryTheirOwnMarks() {
        val everyone = listOf(fine, incomplete, temporary, both)

        val pair = AssignmentDto("carol", "bob").toPair("admin", everyone)

        assertEquals(ParticipantMarks(temporaryPassword = true), pair.santaMarks())
        assertEquals(ParticipantMarks(incompleteInfo = true), pair.receiverMarks())
        assertEquals(ParticipantMarks(temporaryPassword = true, incompleteInfo = true), AssignmentDto("dave", "alice").toPair("admin", everyone).santaMarks())
    }
}
