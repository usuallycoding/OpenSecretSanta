# Tasks

## 1. Remove promotion

- [x] 1.1 Delete `PromoteUser`, its DI binding and its route, `ApiRoutes.ADMIN_USER_PROMOTE` and `adminUserPromote`, `UserRepository.markAdmin` and its SQLite implementation, and the promotion tests in `core/domain/test/ManagedUsersTest.kt`, `server/test/AdminRoutesTest.kt` and `server/test/AuthenticationTest.kt`; add a server test that the old path answers 404 for an administrator; verify `./kotlin test` for the server and core modules and that `grep -ri promote` finds nothing in code outside archived changes

## 2. Calls and the admin tab bar

- [x] 2.1 Add `adminUsers`, `adminUser` and `resetPassword` to `ApiClient` and extend `ScriptedServer` with a fake of the admin user routes (managed set including the administrator, profile fields, temporary passwords that appear and disappear, reset, scoping, scripted failures); verify common tests for the three calls and a 404
- [x] 2.2 Add `AdminUsersRoute` and `AdminUserRoute`, the admin bottom bar (Groups and Users, each tab keeping its place), the back arrow and titles below the Users list, and stand-in screens; verify on the Android emulator as an administrator that both tabs show, that each keeps its place when switching, that back from a tab root leaves the app, and that the participant view still has only its two tabs

## 3. View models

- [x] 3.1 Implement `AdminUsersViewModel` and `AdminUserViewModel` (profile fields left out when blank, the temporary password from the server, reset with a confirmation, own account offering no reset, 404 and network handling) and register them in Koin; verify common tests for each outcome in the design

## 4. Screens

- [x] 4.1 Make the passwords dialog a shared composable with the new wording and use it from the group detail; implement the Users list and the user detail with Reset password and its confirmation; verify on the Android emulator: the list with a person who still has a temporary password and one who changed it, the detail with and without the password, reset with cancel and with confirm (the dialog, the detail updated, the old password refused and the new one accepted at sign-in), no reset on one's own account, a user dropping out of scope while the detail is open, and the server stopped while resetting

## 5. Platforms and integration

- [x] 5.1 Document the Users tab and the removal of promotion in the README and extend the Koin graph test; verify the web in headless Chrome: both tabs, the list, a detail with its password, a reset with its confirmation and dialog, and the address bar following `#AdminUsersRoute` and `#AdminUserRoute/<name>`
- [x] 5.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then check end to end against a real server on the Android emulator: make a group Pending so accounts are created, find a new account under Users with its temporary password, sign in with it, reset it from the Users tab and sign in again with the new one; confirm the promote path answers 404; note what could not be tried on iOS (Done on Android, web and the full build and test run; the cycle of a new account made by a Pending group, its password under Users, a reset and the sign-in with the new password was checked against a real server partly through the app and partly through the API, and the promote path answers 404. The iOS app builds with the rest of the repo but was not exercised by hand. A user dropping out of scope while their page is open is covered by the view-model tests only.)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`, and update the `user-administration` Purpose line that mentions promotion.
