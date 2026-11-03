# Proposal

## Why

Passwords are typed blind, so a typo on a phone keyboard shows up only as a refused sign-in or a "does not match" message. The password-change form asks for three of them. An eye at the end of the field lets the person check what they typed.

## What Changes

- Every password field the person types into (the sign-in field and the three fields of the password-change form, on its screen and in its sheet) gets an eye button at its end that shows the password as text and, chosen again, hides it.
- Each field has its own eye, and a field starts hidden every time it is shown.
- A shown password is hidden again when the app goes to the background.
- The button is named "Show password" while the password is hidden and "Hide password" while it is shown, for screen readers.
- The sign-in field uses the same password input as the password-change form, so the four fields cannot differ.
- Temporary passwords that are displayed for copying are not changed.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-app`: password fields can be revealed.

## Impact

- `app/shared/src/ui/login/LoginScreen.kt` and `app/shared/src/ui/password/ChangePasswordScreen.kt` (`PasswordField`); `ChangePasswordSheet.kt` already uses `PasswordField`.
- Two small icons drawn in `app/shared/src/ui/form/`, like `SwapIcon`, since the core icon set has neither.
- Two strings in `Strings.kt`.
- No server, API or data change.
