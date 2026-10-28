# Design

## Context

The admin group detail (`AdminGroupScreen`, `AdminGroupViewModel`) lists the participants with marks and, for a drawn group, shows the draw from `AdminGroupViewModel.draw` as plain text and a "Read all questions" button; each participant row has "Read wishlist". Those two actions go to `AdminParticipantWishlistRoute` and `AdminQuestionsRoute`, built by the last change (`AdminParticipantWishlistScreen`, `AdminQuestionsScreen`, their view models, strings, Koin entries and tests). The user detail is the screen `AdminUserScreen` over `AdminUserViewModel` (load, confirm reset, one-time password dialog, own-account rule, 404). The only sheet in the admin group view today is the participants editor (`ModalBottomSheet`). The group DTO already carries per participant `hasAccount` and `profileComplete`, so the `*` needs no new call. See proposal.md for motivation and specs/ for behavior.

## Goals / Non-Goals

**Goals:**
- One pair list on a drawn group, with the user and pair details one tap away in sheets, and no more oversight screens.
- Keep reading side-effect free and reuse what exists (user detail, wishlist view model, thread rendering).

**Non-Goals:**
- Server changes, writing anything, a pair-scoped questions call, changing the Users tab.

## Decisions

### D1: The group page and its sheets
```
 Active / Archived                          Draft / Pending
 +------------------------------+           +------------------------------+
 | alice * ->  bob              |           | alice (profile incomplete)   |
 | bob      ->  carol           |           |   [Read wishlist]  (Pending) |
 | carol    ->  admin (you)     |           | ...                          |
 | admin (you) -> alice         |           +------------------------------+
 | * profile incomplete         |
 +------------------------------+
   tap name  -> user sheet       (AdminUserViewModel, same Detail as Users tab)
   tap arrow -> pair sheet       (receiver's wishlist + that pair's thread)
   Pending "Read wishlist" -> wishlist sheet (receiver wishlist part only)
```
The open sheet is state in the group view model (`GroupSheet`: `User(username)`, `Pair(santa, receiver)`, `Wishlist(username)`, or none), not a route. It is set by the row taps and cleared when the sheet is dismissed. A sheet therefore dies with the group screen and the web address bar stays on the group.

### D2: The pair list rows
`AdminPair` gains `santaIncomplete` and `receiverIncomplete`, filled from the group's participants (`profileComplete` false, for participants with an account) when the draw loads and again after a step changes the group, as `loadDraw` already does. A row is a `Row` with two username buttons and an arrow icon button, each at least touch-target size. A username reads `name`, `name (you)`, `name *` or `name (you) *`. The legend "* profile incomplete" is shown only when at least one `*` is on screen. If the draw fails to load, the list area shows the failure with "Try again" in place of the list.

### D3: The user sheet
The sheet hosts `AdminUserViewModel` obtained with `koinViewModel` keyed `admin-user-<username>`, parameters (username, signed-in username), so the own-account rule and 404 handling come for free. The `Detail` composable and the field helper are moved out of `AdminUserScreen` into a shared composable used by both the Users tab and the sheet. The sheet refreshes the view model each time it opens (a view model may outlive a closed sheet), shows the confirmation and the one-time password dialog on top of the sheet exactly as the screen does, and shows the "no longer available" message with a Close button instead of Back.

### D4: The pair and wishlist sheets
```
 AdminParticipantWishlistViewModel(groupId, receiver)   reused as is
 AdminPairThreadViewModel(groupId, santa, receiver, me) new: Loader over GET .../questions,
                                                          picks the thread of (santa, receiver)
```
The pair sheet shows the wishlist (title, note, link, times) and below it the thread (numbered questions, answers, times, "Not answered yet"). The two loads are independent: one failing shows its own message and "Try again" and leaves the other. Both reload on open. The thread view model reuses `toThread` and `OversightQuestion`; a missing thread is shown as "nothing asked". For Pending, the wishlist sheet is the first half of the pair sheet. The item card and thread card composables are lifted out of the deleted screens into the sheet content file.

### D5: Removing the screens
Delete `AdminQuestionsScreen`, `AdminQuestionsViewModel`, `AdminParticipantWishlistScreen` (its content becomes the sheet), `AdminParticipantWishlistRoute`, `AdminQuestionsRoute`, their `NavHost` entries, `titleOf` branches and `belowAdminGroups` entries, their Koin registrations, the strings only they used, the "Read all questions" button and the Active/Archived "Read wishlist" link. `AdminParticipantWishlistViewModel` stays for the sheets. The `AppModulesTest` lines for the removed view models go.

### D6: Back closes a sheet
`ModalBottomSheet` handles the system back on Android, so a first back closes the sheet and a second leaves the group. On the web the browser's back button is bound to the navigation history by the navigation library, ahead of any back handler, and a sheet is not a destination, so it leaves the group; a screen-level `BackHandler` (from `ui-backhandler`, pinned to the Compose version in `libs.versions.toml`) was tried and does not change that, but Escape closes the sheet. The user chose to accept this rather than make the sheets navigation dialogs, which would put a sheet route in the address bar. The iOS edge swipe was not tried. The gap is written in the README and in the `client-app` scenarios.

### D7: Tests
Common tests over `ScriptedServer` (the oversight fake already exists): the pair list rows and `*` flags, none after a failure and none for Pending, the sheet state transitions (open user, open pair, open wishlist, dismiss, one at a time), the pair thread view model (the right thread, unanswered, changed answer, none, 404, 409, unreachable), and that only GETs are sent. The existing questions view model tests are moved to the pair thread view model. Screens are verified by running the app: the Android emulator against a real server for Active, Archived, Pending and Draft, headless Chrome for the web.

## Risks / Trade-offs

- [Pending loses nothing, but only if "Read wishlist" still reaches the list] → It stays and opens a sheet, so no oversight capability is dropped.
- [Four rows of two names and an arrow must fit a phone] → Names wrap; the arrow keeps a fixed touch target; checked on the emulator and at web width.
- [Sheets are not routes, so the web address no longer shows the oversight view and deep links are gone] → Accepted by the user; the screens are removed on purpose.
- [A reused view model can show a stale user in a reopened sheet] → Refresh on every open.
- [Back handling differs per platform: the browser back leaves the group instead of closing a sheet] → Accepted and documented (D6); Escape closes it on the web.

## Migration Plan

Nothing to migrate. Rollback is the previous build.
