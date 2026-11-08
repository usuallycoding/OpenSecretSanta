# Tasks

## 1. The icons and the name

- [x] 1.1 Show the Send icon on the "Ask a question" button (`gift/ReceiverScreen.kt`) and delete `QuestionIcon` from `form/AppFab.kt`; verify the build succeeds and that no reference to `QuestionIcon` remains (`grep`)
- [x] 1.2 Make the letters' floating button the Send icon named `Strings.SEND_LETTER` on every width (`letters/LetterScreens.kt`), and delete `WRITE_LETTER` from `Strings.kt`; verify the build succeeds and that no reference to `WRITE_LETTER` remains (`grep`)
- [x] 1.3 Run `openspec validate send-icon-for-questions-and-letters --strict`, `./kotlin build` and `./kotlin test`, and verify they pass (apart from the known failure of `aReleaseBuildAlwaysUsesTheRealServer` caused by uncommitted local changes)

## 2. Newest question first

- [x] 2.1 Order the questions to the receiver (`gift/ReceiverViewModel.kt`) and the questions from the Santa (`gift/SantaQuestionsViewModel.kt`) newest first, and put a question just asked first; verify with view model tests that each list is newest first after loading, that a new question is first, and that an answer saved keeps the order
- [x] 2.2 Run `openspec validate send-icon-for-questions-and-letters --strict`, `./kotlin build` and `./kotlin test`, and verify they pass (apart from the known failure of `aReleaseBuildAlwaysUsesTheRealServer`)

## 3. On a device

- [x] 3.1 On a phone and on a wide window, in an Active group: see the newest question first on both questions pages and a new question at the top; see the Send icon on "Ask a question" and on the letter button, on both widths, the plus still on "Add item" and "New group", and the letter button announced as "Send letter"; note anything that looks off
