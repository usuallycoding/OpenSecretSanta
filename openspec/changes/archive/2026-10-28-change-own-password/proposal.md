# Proposal

## Why

A person can only choose a new password when the one they have is temporary. Anyone who signed in with a password they chose has no way, in the app, to replace it, for example after sharing it or when they want a stronger one. The server already lets any signed-in user do this; only the app is missing an entry point.

## What Changes

- The top-bar menu gets a "Change password" item, in both the participant and the admin view, next to "Sign out".
- Choosing it opens a bottom sheet over the current screen with the current password, the new password and a repeat of it, checked as the forced password-change screen checks them. The person stays where they were.
- On success the sheet says the password was changed and that the person is signed out on their other devices (the server invalidates every older sign-in), and offers to close; the sheet keeps nothing that was typed once it is closed.
- The forced password-change screen for a temporary password is unchanged.
- No server, API or data changes: `PUT /me/password` is used as it is.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-app`: the top-bar menu offers "Change password", and a requirement describes the sheet.

## Impact

- `app/shared/src/ui/shell/ShellScreen.kt` (menu item and sheet), `ui/password/ChangePasswordViewModel.kt` (a done state and a reset), a new sheet composable in `ui/password`, `ui/Strings.kt`, the DI module, tests and the README.
