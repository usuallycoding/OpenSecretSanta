# Tasks

## 1. Native Autofill Semantics & Username Threading

- [x] 1.1 Update `PasswordField` in `app/shared/src/ui/form/PasswordField.kt` to accept an optional `ContentType` (defaulting to `ContentType.Password`) and apply `semantics { contentType = ... }`; tag the username field in `LoginScreen.kt` with `ContentType.Username`, current password with `ContentType.Password`, and new/repeat password fields in `ChangePasswordScreen.kt` and `ChangePasswordSheet.kt` with `ContentType.NewPassword`, plus `LocalAutofillManager.current?.commit()` on successful sign-in / password change; verify Android and WasmJS compilation.
- [x] 1.2 Thread the signed-in `username` into `ChangePasswordScreen` (from `App.kt`) and `ChangePasswordSheet` (from `SettingsScreen.kt`) so password-change forms can associate updates with the active account; verify existing ViewModel and screen tests compile and pass.

## 2. Platform Auth Form Hosts & Web Assets

- [x] 2.1 Add `app_logo_light.png` and `app_logo_dark.png` to `app/webApp/resources/` and add Material-3-aligned auth form overlay CSS (using `--oss-*` CSS custom properties) to `app/webApp/resources/styles.css`; verify static assets and CSS rules are present.
- [x] 2.2 Introduce `expect`/`actual` auth form hosts (`PlatformLoginForm`, `PlatformChangePasswordForm`, `PlatformChangePasswordSheet`) in `app/shared/src/ui/form/AuthFormHosts.kt` with Android and iOS actuals delegating to the Compose UIs; verify Android compilation.

## 3. Web (WasmJS) Native HTML `<form>` Overlays

- [x] 3.1 Implement the WasmJS `PlatformLoginForm` mounting a semantic `<form>` with `<input autocomplete="username">`, `<input type="password" autocomplete="current-password">` + reveal toggle, stay-signed-in checkbox, error text, real `<button type="submit">`, light/dark logo, Android download button, app version, theme CSS variables, `visibilitychange` password auto-hide, and clean DOM teardown; verify with WasmJS DOM unit tests in `app/shared/test@wasmJs/`.
- [x] 3.2 Implement the WasmJS `PlatformChangePasswordForm` and `PlatformChangePasswordSheet` mounting semantic `<form>` elements with a visually hidden readonly `<input autocomplete="username">` set to the signed-in username, `autocomplete="current-password"`, two `autocomplete="new-password"` fields with independent reveal toggles and inline errors, real `<button type="submit">`, Sign out / Cancel / Close actions, `visibilitychange` auto-hide, and clean DOM teardown on dismiss; verify with WasmJS DOM unit tests in `app/shared/test@wasmJs/`.

## 4. End-to-End Verification

- [x] 4.1 Run the `shared` unit test suites on both Android and WasmJS targets (`./kotlin test -m shared -p android` and `./kotlin test -m shared -p wasmJs`) and verify all tests pass with zero regressions.
