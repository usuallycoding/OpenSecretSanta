# Tasks

## 1. Calls and the admin shell

- [x] 1.1 Add the nine admin group calls to `ApiClient` and extend `ScriptedServer` with a stateful fake of the admin group routes (owner scoping, details, participants with account creation in Pending, the three steps with the server's rules, delete of a Draft, scripted failures); verify common tests for every call, 204 on delete, a 400 naming a field, a 404 and a 409
- [x] 1.2 Add `AdminGroupsRoute`, `AdminGroupNewRoute`, `AdminGroupRoute` and `AdminGroupEditRoute` to the shell (the admin view starts on the list, a back arrow and titles below it, a view switch opening the other view's first screen) with stand-in screens; verify on the Android emulator as an administrator that the admin view opens on a groups placeholder, that switching to the participant view and back opens each on its first screen, and that back from either first screen leaves the app

## 2. View models

- [x] 2.1 Implement `AdminGroupsViewModel` and `AdminGroupFormViewModel` (the five fields, `GroupDetails.of` per-field errors, blanks as empty, create and edit, a server field error, a network failure keeping the text) and register them in Koin; verify common tests for each outcome in the design
- [x] 2.2 Implement `AdminGroupViewModel` (the loaded group, the actions per phase, the participants sheet with `Username.parse`, the confirmations, the four-participant rule, the steps and delete, the temporary passwords state, 409 and 404 handling) and register it in Koin; verify common tests for each outcome in the design, including that passwords are gone after dismissal

## 3. Screens

- [x] 3.1 Implement the admin groups list and the create and edit form (state and count in each row, the empty state, "New group", hints under date and time, field errors, refresh and retry); verify on the Android emulator: creating a group with only a name and with every field, a blank name, an invalid date and time, editing and clearing a field, the list after returning, and the server stopped while saving
- [x] 3.2 Implement the admin group detail with the participants (marks for no account and incomplete profile, state sentence, read-only roster after the draw) and the participants bottom sheet (add, remove, invalid and repeated names, save); verify on the Android emulator: adding and removing people, an invalid and a repeated name, the back button closing only the sheet, and the roster being read-only once Active
- [x] 3.3 Implement the steps and their confirmations, "Delete" for a Draft and the temporary passwords dialog; verify on the Android emulator with a real server: Make pending creating accounts and showing their passwords once, the same for a person added while Pending, Draw the names with four people and the unavailable action with three, Archive, deleting a Draft, cancelling each confirmation, and a step refused because the group changed on the server

## 4. Platforms and integration

- [x] 4.1 Document the admin groups screens in the README and extend the Koin graph test for the new view models; verify the web in headless Chrome: the admin view opening on the list, creating a group, the participants sheet, a step with its confirmation, the passwords dialog, and the address bar following `#AdminGroupRoute/<id>`
- [x] 4.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then check end to end against a real server on the Android emulator: create a group, add four people, make it Pending, sign in as one of the new accounts with the shown password, draw the names, archive, and see the participant view show the group; note what could not be tried on iOS (Done on Android, web and the full build and test run, in pieces on one real server: created, filled, made pending, drawn and archived in the app, and a new account signed in with the shown password and saw the group. The iOS app builds with the rest of the repo but was not exercised by hand. A server that is down while the form saves, and a group deleted by someone else while its page is open, are covered by the view-model tests only.)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.
