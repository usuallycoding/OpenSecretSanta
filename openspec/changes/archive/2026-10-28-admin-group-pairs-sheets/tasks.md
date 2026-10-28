# Tasks

## 1. Remove the oversight screens

- [x] 1.1 Delete `AdminQuestionsScreen`, `AdminQuestionsViewModel`, `AdminParticipantWishlistScreen`, `AdminParticipantWishlistRoute` and `AdminQuestionsRoute` with their `NavHost` entries, `titleOf` and `belowAdminGroups` branches, Koin registrations, `AppModulesTest` lines and strings only they used; keep `AdminParticipantWishlistViewModel`; remove the "Read all questions" button and the Active and Archived "Read wishlist" link and the separate draw section from the group detail so it compiles; verify `./kotlin build` and that `grep -rn "AdminQuestions" app` finds nothing

## 2. View models

- [x] 2.1 Fill `santaIncomplete` and `receiverIncomplete` on `AdminPair` from the group's participants when the draw loads and after a step, and add the sheet state (`GroupSheet`: user, pair, wishlist, none) with open and dismiss functions to `AdminGroupViewModel`; verify common tests for the `*` flags (both rows, none when complete, none for Pending and Draft) and each sheet transition
- [x] 2.2 Implement `AdminPairThreadViewModel` (the thread of one pair picked from the questions call, "nothing asked" when absent, 404, 409 and network handling) and register it in Koin; move the questions tests to it; verify common tests for each outcome in the design and that only GETs are sent

## 3. Screens

- [x] 3.1 Move the user `Detail` and field helper out of `AdminUserScreen` into a shared composable and use it from the Users tab; verify the Users tab is unchanged on the Android emulator
- [x] 3.2 Build the pair list on the group detail (rows with two username buttons and an arrow, "you" and `*` marks, the legend when a `*` is shown, the failure with "Try again") for Active and Archived groups, and keep the participants list with "Read wishlist" for Pending and the plain list for Draft; verify on the Android emulator as the owner: Active and Archived groups show one list and no participants list, the owner's own pairs are marked, a participant with an incomplete profile shows `*` in both rows with the legend, Pending shows participants with "Read wishlist", Draft shows neither
- [x] 3.3 Implement the user sheet over `AdminUserViewModel` (refresh on open, reset confirmation and one-time password dialog above the sheet, no reset for the owner, "no longer available" with Close) and the pair sheet with the wishlist and the pair's thread, and the Pending wishlist sheet; verify on the Android emulator: user sheet from the Santa and from the receiver, reset with cancel and with confirm, no reset on the owner's own name, pair sheet with an unanswered question and a changed answer and a pair with nothing asked, link opens, Pending wishlist sheet, the server stopped while a sheet is open shows "Try again" with the other half kept, and a sheet reopened reloads
- [x] 3.4 Close an open sheet on back where the platform offers a back handler (check first what the Compose Multiplatform version provides) and document any platform that cannot; verify Android back closes the sheet then leaves the group, and the browser's back on the web (Android closes the sheet, then leaves the group; on the web the browser back leaves the group, which the user accepted, and Escape closes the sheet; the iOS swipe was not tried)

## 4. Platforms and integration

- [x] 4.1 Update the README's admin and oversight text for the pair list, the sheets and the removed screens and note any back-handling gap; verify the web in headless Chrome: the pair list, both sheets, a reset with its dialog, a link opening, and the address staying on the group while a sheet is open
- [x] 4.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then check end to end against a real server on the Android emulator: a drawn group with one incomplete profile, a participant who has answered and changed an answer, the reset flow and the sign-in with the new password; note what could not be tried on iOS

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.
