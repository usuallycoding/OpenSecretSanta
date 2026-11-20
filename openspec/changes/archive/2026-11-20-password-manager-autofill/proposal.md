# Proposal

## Why

On the web (Compose WasmJS), the sign-in and password-change screens are rendered onto an HTML `<canvas>`, so browser password managers (Chrome, Safari, Firefox, 1Password, Bitwarden, etc.) cannot detect a login or password-change form to autofill credentials or offer to save/update passwords. On Android and iOS, the Compose fields also lack platform autofill content-type hints and autofill commit signals.

## What Changes

- Render the **web (WasmJS)** sign-in form, forced password-change screen, and Settings password-change sheet as real semantic HTML `<form>` elements mounted over the canvas and driven by the existing `LoginViewModel` and `ChangePasswordViewModel`:
  - Sign-in `<form>` includes `<input name="username" autocomplete="username">`, `<input type="password" name="password" autocomplete="current-password">` with per-field reveal toggle, "Stay signed in" checkbox, real `<button type="submit">`, light/dark app logo, error display, Android download action, and app version.
  - Password-change `<form>` (both the mandatory first-login screen and the Settings sheet) includes a visually hidden readonly `<input name="username" autocomplete="username">` carrying the signed-in username so password managers update the right entry, `<input type="password" autocomplete="current-password">`, two `<input type="password" autocomplete="new-password">` fields (each with its own reveal toggle), inline validation errors, and a real `<button type="submit">`.
  - Match the active Compose theme colors dynamically (Light/Dark/dynamic), hide any revealed passwords when the browser tab leaves the foreground, and unmount the DOM overlay cleanly when leaving the screen or sheet.
- Add Compose platform autofill semantics (`ContentType.Username`, `ContentType.Password`, `ContentType.NewPassword`) and `LocalAutofillManager.current?.commit()` on successful sign-in and password change for Android and iOS.

## Capabilities

### New Capabilities
<!-- None — this extends the existing client-app sign-in and password-change behaviors. -->

### Modified Capabilities
- `client-app`: Sign-in and password-change forms are recognizable by password managers on web (semantic HTML `<form>` and `autocomplete` inputs with real submit buttons) and on Android/iOS (platform autofill content types and commit on success).

## Impact

- **Client (`app/shared`)**:
  - `expect`/`actual` platform hooks for web native `<form>` overlays vs Compose forms on Android/iOS (`app/shared/src@wasmJs/`, `app/shared/src@android/`, `app/shared/src@ios/`).
  - Updates to `LoginScreen`, `ChangePasswordScreen`, `ChangePasswordSheet`, `PasswordField`, `App.kt`, and `SettingsScreen.kt` (passing the signed-in username to password-change forms and calling `LocalAutofillManager.current?.commit()` on success).
- **Web assets (`app/webApp`)**: Expose light/dark logo assets (or reuse bundled PNG/data URIs) and minimal form CSS rules in `styles.css`.
- **No backend or API contract changes.**
