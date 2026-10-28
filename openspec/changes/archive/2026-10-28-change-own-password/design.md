# Design

## Context

`PUT /me/password` already accepts a current and a new password from any signed-in user and returns a fresh token while invalidating the earlier ones. The app has `ChangePasswordViewModel` (checks, error mapping, `SessionManager.changePassword`, which stores the new token and reloads the account) and `ChangePasswordScreen`, which is only shown as a gate by `App.kt` while the session is in `PasswordChangeRequired`. The top-bar menu is `ShellMenu` in `ShellScreen.kt`, shared by both views; bottom sheets use `ModalBottomSheet` (see `SheetFrame`, `SheetTextField`).

## Goals / Non-Goals

**Goals:**
- A sheet reachable from the menu in both views, reusing the existing checks and error handling.
- Nothing typed survives closing the sheet.

**Non-Goals:**
- Server changes, password strength rules beyond the existing policy, or "forgot password".
- A button on the Profile tab (the menu item covers both views).
- Keeping other devices signed in.

## Decisions

1. **Menu item and sheet in the shell.** `ShellMenu` gets "Change password" (above "Sign out") and `ShellScreen` keeps a `showPasswordSheet` flag; the sheet is drawn over the Scaffold, so the destination behind is untouched.
2. **Reuse the view model with a reset.** The sheet uses `koinViewModel<ChangePasswordViewModel>(key = "change-password-sheet")`, a separate instance from the gate's. The view model gains `done: Boolean` in its state (set on success) and `reset()` (back to the empty state). Dismissing the sheet calls `reset()` so no password stays in memory, and opening again starts empty.
3. **Fields shared with the gate.** The password field composable in `ChangePasswordScreen.kt` is made `internal` so the sheet and the gate use the same one. The sheet's layout is its own (title, intro line, three fields, error text, Save and Cancel), in a `ModalBottomSheet` with `skipPartiallyExpanded` like the others.
4. **After success.** `SessionManager.changePassword` replaces the token and reloads the account; the new `SessionState.SignedIn` equals the old (same account), so the `StateFlow` does not emit and the shell is not rebuilt. The sheet switches to a confirmation ("Password changed. You are signed out on your other devices.") with a Close button. Verify in the app that the screen behind keeps its place; if the account data changes, the shell would otherwise be recreated.
5. **Back and Escape.** The sheet takes part in the existing back handling for sheets (Android back closes it; Escape closes it on the web).
6. **Strings.** New: menu item "Change password" (the existing `CHANGE_PASSWORD` works as the title/menu text), an intro line about other devices, and the confirmation.

## Risks / Trade-offs

- The person is signed out everywhere else; the sheet says so before they save, as well as after.
- A wrong current password is answered with 403 and no field; the existing mapping puts it next to the current password field, so nothing new.
- If the account returned after the change differs from the old (for instance `mustChangePassword` flips), the shell could be rebuilt and navigation reset; task 2.1 checks this.
