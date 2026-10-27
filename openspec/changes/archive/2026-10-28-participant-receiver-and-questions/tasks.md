# Tasks

## 1. Calls and the group page

- [x] 1.1 Add the six calls to `ApiClient` (`receiver`, `receiverQuestions`, `askQuestion`, `santaQuestions`, `answerQuestion`, `santa`) and extend `ScriptedServer` with a stateful fake of the receiver, both threads and the phase, with failures that can be scripted; verify common tests for every call, a 400 naming `text`, a 404 and a 409
- [x] 1.2 Add `ReceiverRoute`, `SantaQuestionsRoute` and `SantaRoute`, the "Your gift" section on the group detail (absent for Pending, two entries for Active, three for Archived), stand-in screens, the back arrow and the titles; verify a common test of the entries per phase, then on the Android emulator with a Pending, an Active and an Archived group that the right entries show, that each opens and that back returns to the group

## 2. View models

- [x] 2.1 Implement `ReceiverViewModel` (receiver, questions and phase loaded together, only the details that are set, asking with local checks, the 30-question limit, the 409 decided from the phase and the count, the sheet kept on a network failure) and register it in Koin; verify common tests for each outcome listed in the design
- [x] 2.2 Implement `SantaQuestionsViewModel` (questions and phase, answering and changing an answer with local checks, 404 and 409 handling, read-only when not Active) and register it in Koin; verify common tests for each outcome and that the exposed data names no one
- [x] 2.3 Implement `SantaRevealViewModel` (the reveal, a 409 as the "revealed once archived" sentence) and register it in Koin; verify common tests for the reveal, the 409 and a retry

## 3. Screens

- [x] 3.1 Add the shared text sheet (Material 3 bottom sheet, multi-line field capped at six lines, Cancel and Save, field error, keyboard-safe) and implement the "Your receiver" screen (details, the person's questions with answers or "Not answered yet", Ask a question, the limit and Archived states, refresh and retry, notices); verify on the Android emulator: details with and without fields, asking, an empty and an over-long question, the limit at 30 (seed through the API), Archived read-only, the sheet kept with the server stopped, and back dismissing only the sheet
- [x] 3.2 Implement the "Questions from your Santa" screen (numbered questions, Answer or Change answer in the sheet, the empty state, Archived read-only, refresh and retry, notices); verify on the Android emulator: the Santa asks through the API and the questions appear after Refresh, answering and changing an answer, a blank answer, Archived read-only, and that nothing on the screen hints at the Santa
- [x] 3.3 Implement the "Your Santa" screen (username and full name, the not-yet sentence, retry); verify on the Android emulator with an Archived group, and that a stale screen shows the sentence when the group is not Archived

## 4. Platforms and integration

- [x] 4.1 Document the screens in the README and extend the Koin graph test for the new view models; verify the web in headless Chrome: opening each screen and going back with the browser's back button, the sheet saving a question and an answer, and that the address bar follows `#ReceiverRoute/<id>`, `#SantaQuestionsRoute/<id>` and `#SantaRoute/<id>`
- [x] 4.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then check end to end against a real server on the Android emulator: ask as one participant, answer as that participant's receiver through a second account, archive the group and see both threads turn read-only and the reveal appear; note what could not be tried on iOS (Done on Android, web and the full build and test run. The iOS app builds with the rest of the repo but was not exercised by hand. The question limit and the stale-screen cases (archived or full while a sheet is open, a question that is gone, the reveal before archiving) are covered by the view-model tests, because the seed data could not reach them from the UI.)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.
