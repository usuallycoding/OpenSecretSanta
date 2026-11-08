# Proposal

## Why

After asking, the newest question sits at the end of a list of up to 200, out of sight, while letters already show the newest first. And the two actions that send something to the other person look different: asking a question shows a question-mark circle, and sending a letter shows a plus on a narrow window and a Send icon on a wide one, under two different names ("Write a letter" and "Send letter"). The cards on the group page already use the Send icon for the questions, so the same action should look and be named the same everywhere.

## What Changes

- "Ask a question" shows the Send icon, on a narrow window (floating button) and on a wide one (heading button).
- "Send letter" shows the Send icon on a narrow window too, in place of the plus, so it matches the wide window.
- The narrow floating button for a letter is named "Send letter", as the wide button is, in place of "Write a letter".
- Both questions pages, the person's questions to the receiver and the questions from their Santa, list the newest question first, as the letters do; a question just asked appears at the top. Questions keep their numbers.
- The hand-drawn question icon and the "Write a letter" text are removed, as nothing uses them.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-letters`: the button that opens the letter sheet is the Send icon button named "Send letter", not a "+" button.
- `client-app`: asking a question and sending a letter use the Send icon, on every width.
- `client-receiver-and-questions`: both questions pages list the newest question first.

## Impact

- `app/shared/src/ui`: `gift/ReceiverViewModel.kt` and `gift/SantaQuestionsViewModel.kt` (the order), `letters/LetterScreens.kt`, `gift/ReceiverScreen.kt`, `form/AppFab.kt` (the question icon goes), `Strings.kt` (`WRITE_LETTER` goes).
- No server (it still answers oldest first, and the owner's pair thread keeps that order), address, or navigation change. Adding a wishlist item and creating a group keep the plus.
