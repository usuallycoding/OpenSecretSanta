# Design

## Context

The participant view is a `NavHost` with typed routes under `ShellScreen`; the Groups tab stays selected below a group and the top bar shows a back arrow on `GroupRoute`, `WishlistRoute` and `ReceiverWishlistRoute`. Screens load through `Loader` and `LoadContent` (loading, retry, a refresh that keeps what is shown) and call `refresh()` on `ON_RESUME`. `WishlistViewModel` is the pattern to follow: it loads its data together with the group (`api.group`) so the screen knows the phase, keeps the sheet state in the view model, and handles a 409 by reloading and reading the phase. `GroupDetail.phase` (`Pending`, `Active`, `Archived`, `Other`) already exists. The contract types `ReceiverDto`, `SantaRevealDto`, `QuestionDto`, `AskQuestionRequest` and `AnswerRequest` and the routes exist; the domain's `QuestionLimits` holds the lengths (1000, 2000) and the 30-question cap, but its text check is private, so the app checks the lengths itself. See proposal.md for motivation and specs/ for behavior.

## Goals / Non-Goals

**Goals:**
- Screens that cannot leak the Santa: the Santa's screen is built only from `QuestionDto`, which has no field naming anyone, and the reveal exists only in an Archived group.
- The same sheet-and-reload handling of concurrent changes as the wishlist, so the group being archived mid-edit is a sentence, not a broken screen.

**Non-Goals:**
- Notifications, editing or deleting a question, copying an address, marking a gift as sent and admin screens (see proposal.md).

## Decisions

### D1: Routes and the "Your gift" section
```
 GroupRoute(id) -+-> ReceiverRoute(groupId)         "Your receiver"  (details + my questions)
                 +-> SantaQuestionsRoute(groupId)   "Questions from your Santa"
                 +-> SantaRoute(groupId)            "Your Santa"     (Archived only)
```
Typed routes like the others, so the back button and the web address work for free; the top bar shows a back arrow on all three and the Groups tab stays selected. The group detail shows the section from `GroupDetail.phase`: absent for `Pending` and `Other`, two entries for `Active`, three for `Archived`.

### D2: Calls and how errors read
`ApiClient` gains `receiver(groupId)`, `receiverQuestions(groupId)`, `askQuestion(groupId, text)`, `santaQuestions(groupId)`, `answerQuestion(groupId, number, text)` and `santa(groupId)` through the existing `call` helper. Errors arrive as `Validation("text", ...)` (400), `Rejected(409, ...)` and `Rejected(404, ...)`. The 409 on asking is ambiguous: the group is not Active any more, or the 30-question limit is reached. As with the wishlist it is decided from the reloaded phase and the number of loaded questions, never from the message.

### D3: `ReceiverViewModel`
```
 data   : Loader<ReceiverData(receiver, questions, phase)>   receiver + questions + group loaded together
 sheet  : null | QuestionEditor(text, error, saving)
 notice : null | Locked | LimitReached | Unreachable
 canAsk : phase == Active && questions.size < 30
```
Loading calls `api.receiver`, `api.receiverQuestions` and `api.group`; the first failure wins and a missing group is shown as gone (`isGone`). Asking trims and checks 1 to 1000 characters before sending, shows a server field error in the sheet, and on success appends the returned question (through `Loader.replace`) and closes the sheet. A 409 reloads and says `Locked` if the phase is no longer Active, `LimitReached` otherwise; a network failure keeps the sheet and its text. The screen shows only the fields of `ReceiverDto` that are non-blank.

### D4: `SantaQuestionsViewModel`
Same shape for the other direction: a `Loader` over `santaQuestions` plus the group phase, an `AnswerEditor(number, text, error, saving)` sheet, `canAnswer = phase == Active`. Answering trims and checks 1 to 2000 characters and replaces that question with the one the server returns. A 404 (the question is not there) or a 409 reloads and says `QuestionGone` or `Locked`. The model exposes only `QuestionDto` data, so there is nothing about the Santa to show by accident.

### D5: `SantaRevealViewModel`
A plain `Loader` over `santa(groupId)`. A 409 (the group is not Archived yet, for example a stale screen) is shown as the sentence that the Santa is revealed once the group is archived, not as a failure. It has no state of its own.

### D6: One text sheet for asking and answering
A single composable, `TextSheet(title, label, value, error, saving, onChange, onSave, onDismiss)`, over Material 3 `ModalBottomSheet` opened fully, with a multi-line field capped at six visible lines, Cancel and Save, scrolling and `imePadding` as in the wishlist sheet. Both view models drive it, so asking and answering look and behave the same. Platform dismissal (back, tap outside, Escape) closes it and discards the text.

### D7: Tests
Common tests over `ScriptedServer`, extended with the six routes and a stateful fake thread store (a question list, answers, the phase) with scripted failures:
- `ApiClient`: every call, the 400 field error, 404 and 409.
- `ReceiverViewModel`: details with and without fields, questions in order with and without answers, ask (appended), blank and over-length text, the 30 limit, 409 as archived and as limit, a server field error keeping the sheet, a network failure keeping the text, and Archived being read-only.
- `SantaQuestionsViewModel`: order, empty, answer and change answer, blank and over-length, 404, 409, Archived read-only, and that nothing exposed names the Santa.
- `SantaRevealViewModel`: the reveal, the 409 sentence, retry.
- The group detail's Your gift entries for each phase.

The screens and sheets are verified by running the app: the Android emulator, headless Chrome and, for iOS, whatever the owner can do on their device.

## Risks / Trade-offs

- [A question asked in a stale group] → Handled as in the wishlist: the refusal reloads, the phase decides the sentence, the screen turns read-only.
- [A hint of who the Santa is leaking through the screens] → The model has no such field; a test asserts the exposed data, and the reveal entry is tied to the Archived phase.
- [Long answers in a long thread make a tall screen] → Each entry is a card in a lazy list; nothing is truncated.
- [Sheets with text fields differ between Android, iOS and the web] → The wishlist sheet already ships on all three; this one reuses its pattern.

## Migration Plan

Nothing to migrate: new screens, no server change and no new dependency. Rollback is the previous build.
