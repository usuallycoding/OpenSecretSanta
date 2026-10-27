# Tasks

## 1. Navigation dependency

- [x] 1.1 Add the Compose Multiplatform navigation library (`navigation-compose` 2.10.0 via `libs.versions.toml`) and Kotlin serialization to `app/shared`, with one placeholder `@Serializable` route used from `App` so the dependency is exercised; verify `./kotlin build` succeeds for Android, iOS and wasm, and `./kotlin test -m shared` still passes (if resolution fails against the catalog's lifecycle 2.11.0, pin or step the navigation version and say so)

## 2. Calls, load state and formatting

- [x] 2.1 Add `groups()`, `group(id)` and `updateProfile(request)` to `ApiClient` and extend `ScriptedServer` with `GET /groups`, `GET /groups/{id}` and `PUT /me`; verify common tests for a list, one group, a 404 arriving as `Rejected(404)`, the exact `PUT /me` body (all five fields plus the username, blanks as null) with the bearer token, a 400 naming a field, and the password never appearing in any of them
- [x] 2.2 Add `LoadState` and `Loader` (first load, retry, refresh that keeps the old value, a failed refresh that keeps it, a refresh ignored while one is running) and the `LoadContent` composable with the new `Strings`; verify common tests for every row of the table in the design and that error kinds map to the unreachable and generic messages
- [x] 2.3 Add the pure helpers `formatWhen`, the group state label and note, and the "is me" check; verify common tests for date and time, date only, seconds in the time, an unparsable value returned unchanged, each state's label and note, an unknown state, and case-insensitive username matching

## 3. Groups view models

- [x] 3.1 Implement `GroupsViewModel` (list through `Loader`, refresh, retry) and register it in Koin; verify common tests for a listed group's fields, no groups, a missing date, time or location, unreachable then retry, and refresh after a state change
- [x] 3.2 Implement `GroupDetailViewModel(id)` (detail through `Loader`, "no longer available" on 404, participants with the person's own marked); verify common tests for a full group, a group with only a name, a 404, unreachable then retry, and the own username marked whatever its case

## 4. Shell and navigation

- [x] 4.1 Replace the placeholder shell with the scaffold of the design: top bar (title, back arrow on a group, menu with the view switch for administrators and sign out), bottom tabs `Groups` and `Profile` on the participant view's top-level screens, the typed `NavHost` with `GroupsRoute`, `GroupRoute(id)` and `ProfileRoute`, tab save and restore, the shell keyed by username, and the admin placeholder under the same top bar; verify `ShellViewModel` tests still hold (admin starts in the admin view, a normal participant can never select it, sign out) and run the Android emulator as an administrator and as a normal participant: tabs switch, the menu shows the right items, sign out works
- [x] 4.2 Add the `onNavigationReady` parameter to `App` and call it from the participant shell; verify the build on all targets and that the Android system back button and gesture go from a group to the list and, from the first screen, leave the app

## 5. Groups screens

- [x] 5.1 Implement the Groups tab screen (rows with name, state, date and time, location, participant count; the empty state; refresh action; refresh on resume) and the group detail screen (details, participants with "you", state note, "no longer available" with a way back); verify against a real server on the Android emulator with a Pending, an Active and an Archived group, one without date, time or location, no groups at all, the server stopped (message and "Try again"), and a group activated while the list is open and then refreshed

## 6. Profile

- [x] 6.1 Implement `ProfileViewModel` (load, dirty tracking, `Profile.of` validation, save with all five fields, server field errors, network failure keeping edits, reload on resume that never overwrites edits) and register it in Koin; verify common tests for an existing and an empty profile, nothing changed so saving is unavailable, a successful save becoming the new baseline, a cleared field, an invalid email, a field over 500 characters, a server validation error landing on its field, unreachable keeping the edits, and a resume reload not replacing typed text
- [x] 6.2 Implement the Profile tab screen (read-only username, five fields, per-field errors, saved confirmation, the note about name and address in its two forms, load error with "Try again"); verify on the Android emulator: edit and save, clear a field, an invalid email, the note appearing and disappearing as name and address are typed, edits surviving a switch to the Groups tab and back, and a saved profile still shown after restarting the app

## 7. Platforms and integration

- [x] 7.1 Bind the web host to the browser history (`bindToNavigation` through `onNavigationReady`) and document the new screens in the README; verify in a real browser: opening a group and pressing back shows the list without reloading or losing the session, and the address bar follows the screens
- [x] 7.2 Extend the Koin graph test to resolve the new view models and run `./kotlin build` and `./kotlin test` for the whole repo; verify they pass, then run an end-to-end check against a real server with an administrator who is also a participant and with a normal participant on the Android emulator and the iOS simulator or device (groups list and detail, profile edit, back navigation, view switch, sign out and a different person signing in to a fresh first screen)
