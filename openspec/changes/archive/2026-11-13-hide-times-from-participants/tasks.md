# Tasks

## 1. Hide the time lines on participant screens

- [x] 1.1 Add `showTimes: Boolean = false` to `ItemText`, skipping `TimesLine` and an empty row when false; verify the own wishlist and the receiver's wishlist (`ReceiverWishlistScreen.kt`) show no "Added"/"Changed" while "New"/"Edited" pills still appear
- [x] 1.2 Add `showTimes: Boolean = false` to `QuestionThread` for the "Asked", "Answered" and "Answer changed" lines; verify the Gifter and Receiver pages show none while "Unanswered" and the answer "New" pill still appear
- [x] 1.3 Add `showTimes: Boolean = false` to `LetterCard`; verify the Send letter page and the Inbox show no "Sent" line while the Inbox "New" pill still appears
- [x] 1.4 Leave `AnnouncementCard` as is; verify the Announcements page still shows "Sent" in both views

## 2. Keep the times in the Admin view

- [x] 2.1 Pass `showTimes = true` at the three call sites in `admin/OversightSheets.kt` (participant wishlist, `ThreadQuestions`, `PairLetters`); verify the sheets show "Added"/"Changed", "Asked"/"Answered"/"Answer changed" and "Sent" in Bucharest time
- [x] 2.2 Verify an administrator who switches to the Participant view sees no time line on the same wishlist, thread and letters

## 3. Tests and specs

- [x] 3.1 Update tests that read the time lines on participant screens to assert they are absent, keep read-mark and `TimesMappingTest` assertions passing, and add admin-sheet assertions that the lines are present; verify with the project's test task for `app/shared`
- [x] 3.2 Run `openspec validate hide-times-from-participants --strict` and check the web and Android builds render the participant screens without the gap left by the removed row

## Workflow follow-up

- Archive the change once implemented and reviewed, which syncs the deltas into `client-times`, `client-letters`, `client-read-marks` and `client-receiver-and-questions`.
