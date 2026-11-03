## Context

`QuestionCard` in `ReceiverScreen.kt` serves the Gifter tab and the Receiver tab; `ThreadQuestions` in `OversightSheets.kt` repeats the same question, times and answer rows for the admin sheet. Both put everything on one left edge.

## Goals / Non-Goals

**Goals:** make the question-to-answer relationship visible, make open questions easy to spot, and keep one layout for all three places.

**Non-Goals:** chat bubbles, new data, a change of order or numbering, or any change to the actions.

## Decisions

- **One block, two users.** A shared composable takes the question and draws a number circle beside the question and its times, then an answer slot. `QuestionCard` wraps it in a `Card` with the action below; `ThreadQuestions` uses it without a card. This removes the duplicated rows.
- **Number in a circle.** A 24 dp circle in `secondaryContainer` holds the number in `labelMedium`, at the start of the question. The question's text follows after a 10 dp gap and no longer starts with "1.".
- **Line, not bubbles.** The answer slot is indented by the circle's width plus the gap, so it lines up with the question's text. One `drawBehind` on the whole block draws a 1.5 dp line in `outlineVariant`, down from the circle's bottom along the circle's centre, then a short curve to the answer's first line. The answer slot reports its top with `onGloballyPositioned`, so a question that wraps to many lines, or a long answer, needs no special case.
- **Unanswered keeps the slot.** "Not answered yet" fills the slot in `onSurfaceVariant`, so the line reads as "an answer goes here" and the card's height barely changes when it is answered.
- **Actions outside the slot.** "Answer" and "Change answer" stay at the bottom of the card, so nothing moves.
- **Accessibility.** The line is drawn, not a composable with content, so it adds nothing for a screen reader. The circle's number is read before the question's text, and the question's dialog title becomes "Question 1".

## Risks / Trade-offs

- A narrow phone loses 34 dp of text width to the circle and the indent; the answer wraps, and `ReadOnlyText` already cuts at its row limit.
- `outlineVariant` can be faint in some themes. Check light and dark, and change to `outline` only if it is not visible.
