# Proposal

## Why

A question and its answer look alike today: both sit on one left edge and differ only in text size. At a glance it is hard to tell which line is which, or which questions are still open. A question with its answer is a short thread, and an indented reply with a connector line is the familiar way to show that.

## What Changes

- Wherever a question is shown with its answer, the question's number sits in a small filled circle at the start of the question, and a thin vertical line runs from that circle down and turns toward the answer, which is indented to line up with the question's text.
- A question without an answer shows the same line and slot, holding "Not answered yet" in a muted colour, so open questions stand out.
- The "Answer" and "Change answer" actions stay at the bottom of the card, outside the indented slot.
- The Gifter page's Questions tab, the Receiver page's questions tab and the admin pair thread all use one shared layout, so they cannot drift apart.
- Lines, not chat bubbles; the circle holds only the number, which no longer starts the question's text. No change to what is shown, its order or its numbering, and no change to the screen-reader labels "Question" and "Answer".

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-receiver-and-questions`: how a question and its answer are laid out on the participant pages.
- `client-admin-oversight`: the pair thread in the admin pair sheet uses the same layout.

## Impact

- `app/shared/src/ui/gift/ReceiverScreen.kt` (`QuestionCard`), `SantaQuestionsScreen.kt` and `app/shared/src/ui/admin/OversightSheets.kt` (`ThreadQuestions`).
- A new shared composable in `app/shared/src/ui/gift/` for the question-and-answer block.
- No server, API or data change.
