# Tasks

## 1. Calls

- [x] 1.1 Add `adminAssignments`, `adminParticipantWishlist` and `adminQuestions` to `ApiClient` and extend `ScriptedServer` with a fake of the three routes (owner scoping, states, 404, 409, scripted failures); verify common tests for the three calls, a 404 and a 409

## 2. View models

- [x] 2.1 Load the draw in `AdminGroupViewModel` only for Active and Archived groups, marking the administrator's own username; a failure becomes a notice for that section only; verify common tests for each state and a failure
- [x] 2.2 Implement `AdminParticipantWishlistViewModel` and `AdminQuestionsViewModel` (order, empty, unanswered, 404 and 409 handling) and register them in Koin; verify common tests for each outcome in the design

## 3. Screens and routes

- [x] 3.1 Add `AdminParticipantWishlistRoute` and `AdminQuestionsRoute` to the admin `NavHost` under the Groups tab; lift the receiver wishlist item row and the question and answer rows into shared read-only composables; implement the participant wishlist screen and the Questions screen; verify on the Android emulator as the owner: a Pending group's participant wishlist, an Active group's draw and threads including an unanswered question and a changed answer, no write controls anywhere, back returning to the detail, and the Groups tab staying selected
- [x] 3.2 Add the draw section, the participant tap-through and the Questions entry to the group detail, shown only in the states the spec allows; verify Draft shows none of them, Pending shows only the wishlist tap-through, Active and Archived show all three, and the owner's own pairs are marked "you"

## 4. Platforms and integration

- [x] 4.1 Replace the README sentence saying oversight is the next admin change with a description of the screens, and extend the Koin graph test; verify the web in headless Chrome: the draw, a wishlist, the questions, a link opening, and the address bar following both new routes
- [x] 4.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then check end to end against a real server on the Android emulator: a participant writes a wishlist and answers a question, the owner sees both with the right times; stop the server while a screen is open and check "Try again"; note what could not be tried on iOS

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.
