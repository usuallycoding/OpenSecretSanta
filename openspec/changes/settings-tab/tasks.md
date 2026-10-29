# Tasks

## 1. The route and its address

- [x] 1.1 Add `SettingsRoute` and its web address (`addressOf` for routes and for back stack entries, `routeOf` accepting `settings` with no other part, for everyone), and add the cases to `WebAddressesTest` (both directions, an administrator and a participant, and that `#/settings/extra` names no screen); verify `./kotlin test -m shared` passes.

## 2. The page

- [x] 2.1 Add `SettingsScreen` (rows Appearance with its current choice, Notifications only where `PushRegistration.isOffered`, Change password, and a set-apart Sign out), moving the three sheets, their keyed view models and `SheetBackHandler`s into it, plus the strings and the drawn swap icon; verify `./kotlin build -m shared` succeeds with no warnings on every platform.

## 3. The shell

- [x] 3.1 Replace the view-following `isAdminScreen()` with a function that answers admin, participant or neither (Settings), and use it in both places in `ShellScreen` (following a destination and opening the first screen after a switch); verify the shared tests and build pass.
- [x] 3.2 Add the Settings item to `TabBar` and `AdminTabBar` (admin Groups not selected on Settings), the `composable<SettingsRoute>`, the Settings title first in `titleOf`, and replace `ShellMenu` with the administrators-only switch icon (remove `ShellMenu`, the shell's sheet state and `Strings.MENU`); verify on the phone as a participant that the bar shows Groups, Profile and Settings, that Settings opens the page titled "Settings" with no menu in the top bar, that each row opens its sheet and closes it back to the page, that Appearance shows its choice after changing it, and that Sign out signs out.
- [x] 3.3 Verify on the phone as an administrator that the bar shows Groups, Users and Settings in the admin view, that opening Settings keeps the admin view and its bar and title, that the top-bar switch icon is there (announced as "Switch to participant view", checked with `uiautomator`), that using it from Settings opens the participant view's first screen, that the participant view's Settings keeps the participant bar, and that a participant has no icon at all.

## 4. Integration

- [x] 4.1 Update the Purpose sentence of the main `client-notifications` spec ("a menu item and bottom sheet") to say Settings row, run `openspec validate settings-tab --strict` and the whole test suite, verify they pass with no new compiler warnings, then have the web app checked in a browser (the bar, the page, `#/settings` typed and refreshed, the sheets, the switch icon).
