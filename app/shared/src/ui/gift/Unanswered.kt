package com.usuallycoding.opensecretsanta.ui.gift

/** The most a badge counts exactly: one more is shown as "9+". */
internal const val BADGE_MAX = 9

/** How many of the Santa's [questions] wait for an answer: those without one, but only while answering is open ([canAnswer]). */
internal fun unansweredCount(questions: List<Question>, canAnswer: Boolean): Int =
    if (canAnswer) questions.count { it.answer == null } else 0

/** What a badge for [count] says: the number, "9+" above [BADGE_MAX], and nothing, so no badge, for none. */
internal fun countBadgeText(count: Int): String? = when {
    count <= 0 -> null
    count > BADGE_MAX -> "$BADGE_MAX+"
    else -> count.toString()
}
