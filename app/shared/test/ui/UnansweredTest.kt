package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.gift.Question
import com.usuallycoding.opensecretsanta.ui.gift.countBadgeText
import com.usuallycoding.opensecretsanta.ui.gift.unansweredCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UnansweredTest {
    private fun question(number: Int, answer: String?) = Question(number, "q$number", answer, asked = "now", answered = answer?.let { "now" }, answerChanged = null)

    @Test
    fun onlyQuestionsWithoutAnAnswerCountWhileAnsweringIsOpen() {
        val questions = listOf(question(1, null), question(2, "yes"), question(3, null))

        assertEquals(2, unansweredCount(questions, canAnswer = true))
    }

    @Test
    fun everyQuestionAnsweredCountsNothing() {
        assertEquals(0, unansweredCount(listOf(question(1, "a"), question(2, "b")), canAnswer = true))
    }

    @Test
    fun noQuestionsCountNothing() {
        assertEquals(0, unansweredCount(emptyList(), canAnswer = true))
    }

    @Test
    fun nothingCountsOnceAnsweringIsClosed() {
        assertEquals(0, unansweredCount(listOf(question(1, null), question(2, null)), canAnswer = false))
    }

    @Test
    fun theBadgeSaysTheNumberUpToNineAndNineAndAPlusAfter() {
        assertNull(countBadgeText(0))
        assertEquals("1", countBadgeText(1))
        assertEquals("9", countBadgeText(9))
        assertEquals("9+", countBadgeText(10))
        assertEquals("9+", countBadgeText(30))
    }
}
