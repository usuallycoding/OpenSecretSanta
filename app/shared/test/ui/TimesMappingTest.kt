package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.ui.gift.toQuestion
import com.usuallycoding.opensecretsanta.ui.wishlist.toItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What a person is shown about when their items and questions were made, in Bucharest time. */
class TimesMappingTest {
    private val created = "2026-12-01T10:00:00Z"

    @Test
    fun anItemNeverEditedShowsOnlyWhenItWasAdded() {
        val item = WishlistItemDto(1, "Socks", null, null, created, created).toItem()

        assertEquals("1 Dec 2026, 12:00", item.added)
        assertNull(item.changed)
    }

    @Test
    fun anItemEditedLaterShowsBothTimes() {
        val item = WishlistItemDto(1, "Socks", null, null, created, "2026-12-05T08:30:00Z").toItem()

        assertEquals("1 Dec 2026, 12:00", item.added)
        assertEquals("5 Dec 2026, 10:30", item.changed)
    }

    @Test
    fun theReceiversItemsShowTheSameTimes() {
        val unedited = SharedWishlistItemDto("Tea", null, null, created, created).toItem()
        val edited = SharedWishlistItemDto("Tea", null, null, "2026-07-01T10:00:00Z", "2026-07-02T10:00:00Z").toItem()

        assertEquals("1 Dec 2026, 12:00", unedited.added)
        assertNull(unedited.changed)
        assertEquals("1 Jul 2026, 13:00", edited.added)
        assertEquals("2 Jul 2026, 13:00", edited.changed)
    }

    @Test
    fun anUnansweredQuestionShowsOnlyWhenItWasAsked() {
        val question = QuestionDto(1, "Colour?", "2026-12-02T10:00:00Z").toQuestion()

        assertEquals("2 Dec 2026, 12:00", question.asked)
        assertNull(question.answered)
        assertNull(question.answerChanged)
    }

    @Test
    fun anAnswerGivenOnceShowsWhenItWasAnsweredAndNoChange() {
        val question = QuestionDto(1, "Colour?", "2026-12-02T10:00:00Z", "Green", "2026-12-03T10:00:00Z", "2026-12-03T10:00:00Z").toQuestion()

        assertEquals("3 Dec 2026, 12:00", question.answered)
        assertNull(question.answerChanged)
    }

    @Test
    fun anAnswerEditedLaterShowsAllThreeTimes() {
        val question = QuestionDto(1, "Colour?", "2026-12-02T10:00:00Z", "Blue", "2026-12-03T10:00:00Z", "2026-12-04T10:15:00Z").toQuestion()

        assertEquals("2 Dec 2026, 12:00", question.asked)
        assertEquals("3 Dec 2026, 12:00", question.answered)
        assertEquals("4 Dec 2026, 12:15", question.answerChanged)
    }
}
