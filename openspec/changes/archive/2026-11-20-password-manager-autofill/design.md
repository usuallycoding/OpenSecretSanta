# Design

## Context

See `proposal.md` for motivation. Key constraints from the current codebase:
- **Web (WasmJS)**: Compose Multiplatform renders onto a single HTML `<canvas>` inside `ComposeViewport` (`app/webApp/src/main.kt`). Browser password managers (Chrome, Safari, Firefox, 1Password, Bitwarden) inspect the DOM for `<form>` and `<input autocomplete="...">` elements and do not recognize canvas-drawn text fields.
- **Native (Android / iOS)**: `LoginScreen.kt`, `PasswordField.kt`, `ChangePasswordScreen.kt`, and `ChangePasswordSheet.kt` render Material 3 `OutlinedTextField`s without `semantics { contentType = ... }` or `LocalAutofillManager.current?.commit()`.
- **Shared state**: Both `LoginViewModel` and `ChangePasswordViewModel` already encapsulate all validation, error mapping, and submission logic in `app/shared`, independent of UI toolkit.

## Goals / Non-Goals

**Goals:**
- Make sign-in, mandatory password change, and Settings password change immediately recognizable by browser password managers on Web (both autofill and "Save / Update password?" prompts).
- Preserve all existing `client-app` behaviors on Web (light/dark logo matching app theme, snow/background visible behind the form, per-field password reveal toggle, auto-hide on tab backgrounding, inline field errors, stay-signed-in checkbox, Android download button, app version, and cleanup on dismiss).
- Add native Compose autofill `contentType` semantics and `AutofillManager.commit()` on Android and iOS.

**Non-Goals:**
- Replacing any other Compose screen on Web with HTML.
- Passkeys / WebAuthn or OAuth.
- Android Digital Asset Links (`assetlinks.json`) server hosting (can be added separately if cross-origin Android/Web credential sharing is wanted later).

## Decisions

### 1. Platform `expect`/`actual` auth form hosts in `app/shared`
Introduce three small `@Composable expect fun` hosts in `app/shared/src/ui/form/AuthFormHosts.kt` (or in `ui/login` and `ui/password`):
- `PlatformLoginForm(viewModel, sessionEnded, androidDownloadUrl)`
- `PlatformChangePasswordForm(username, viewModel, onSignOut)`
- `PlatformChangePasswordSheet(username, viewModel, onDismiss)`

- **`src@android` & `src@ios`**: Delegate directly to the existing Compose UIs (enhanced with `semantics { contentType = ... }` and `LocalAutofillManager.current?.commit()`).
- **`src@wasmJs`**: Mount a real HTML `<form>` overlay into `document.body` via `DisposableEffect`, keeping it synchronized with `viewModel.state` and `MaterialTheme.colorScheme` via `LaunchedEffect` / `SideEffect`, and removing it cleanly on dispose.

**Why**: Keeps `LoginViewModel` and `ChangePasswordViewModel` as the single source of truth on all platforms, avoids platform branching inside screen layouts, and leaves Android/iOS Compose UI completely untouched in appearance.
**Alternative considered**: Positioning transparent DOM `<input>`s over Compose `OutlinedTextField` rects. Rejected because pixel-alignment breaks easily under browser zoom/scroll/resize and leaves the `<button type="submit">` outside the `<form>`, hurting "Save password?" prompt reliability.

### 2. Semantic HTML `<form>` structure & theme synchronization on WasmJS
Each web form mounts inside a fixed full-viewport flex container (`pointer-events: auto`, transparent background on full-screen Login/ChangePassword so the canvas theme background and snow show through; translucent scrim + themed card surface for `ChangePasswordSheet`):
- **Sign-in `<form>`**:
  - `method="post"`, `novalidate` (validation handled by `LoginViewModel`), `onsubmit` calls `event.preventDefault()` and `viewModel.submit()`.
  - `<img>` for light/dark logo (`alt="Open Secret Santa"`), switched using the same `logoIsDark(MaterialTheme.colorScheme.background)` check. Load logo PNGs from static web resources (`app/webApp/resources/app_logo_light.png` and `app_logo_dark.png`, copied/linked from the shared drawable resources).
  - `<input type="text" name="username" autocomplete="username" autocapitalize="none" spellcheck="false">`
  - `<input type="password" name="password" autocomplete="current-password">` with adjacent `<button type="button" aria-label="Show password">` eye toggle.
  - `<label><input type="checkbox"> Stay signed in</label>`
  - `<button type="submit">Sign in</button>` (disabled when `!state.canSubmit`).
  - Optional Android download `<button type="button">` and muted version text.
- **Password-change `<form>` (screen and sheet)**:
  - Visually hidden (`position:absolute; width:1px; height:1px; overflow:hidden; opacity:0; pointer-events:none; tabIndex=-1`) readonly `<input type="text" name="username" autocomplete="username" value="<username>">` so password managers associate the update with the right account.
  - Current password (`autocomplete="current-password"`), new password (`autocomplete="new-password"`), repeat password (`autocomplete="new-password"`), each with its own independent reveal toggle and inline error element.
  - Real `<button type="submit">` plus secondary `Sign out` (screen) or `Cancel` / `Close` (sheet) `<button type="button">`.
- **Foreground auto-hide**: Attach a `document` `visibilitychange` listener inside `DisposableEffect`; when `document.hidden` becomes true, reset all revealed password inputs back to `type="password"` and update their `aria-label` to `"Show password"`.
- **Dynamic theming**: Apply CSS custom properties (`--oss-bg`, `--oss-surface`, `--oss-on-surface`, `--oss-on-surface-variant`, `--oss-primary`, `--oss-on-primary`, `--oss-outline`, `--oss-error`) from `MaterialTheme.colorScheme` onto the overlay root whenever the Compose theme changes, with clean base styles in `app/webApp/resources/styles.css`.
- **Two-way sync without caret jumps**: Only assign `input.value = stateValue` when `input.value != stateValue` (e.g., when a failed login clears the password or a sheet resets), and listen to both `input` and `change` events (some password managers fire `change` or programmatic `input`).

### 3. Passing signed-in username to password-change forms
- `SessionState.PasswordChangeRequired` already knows the signed-in account (`current.account.username`) in `App.kt`.
- `SettingsScreen` already receives or has access to the signed-in `Account` / `username`.
- Pass `username: String` into `ChangePasswordScreen` and `ChangePasswordSheet` so the web form can populate its hidden `autocomplete="username"` field.

### 4. Native Compose Autofill (Android & iOS)
- Add an optional `contentType: ContentType? = null` parameter to `PasswordField`, defaulting to `ContentType.Password`, and pass `ContentType.NewPassword` for the new and repeat password fields in `ChangePasswordScreen` and `ChangePasswordSheet`.
- Tag the username `OutlinedTextField` in `LoginScreen` with `Modifier.semantics { contentType = ContentType.Username }`.
- Capture `val autofillManager = LocalAutofillManager.current` and invoke `autofillManager?.commit()` when sign-in transitions to signed-in / password-change succeeds (e.g., via `LaunchedEffect(state.done)` on `ChangePasswordSheet` and on successful submit callback in `LoginScreen` / `ChangePasswordScreen`).

## Risks / Trade-offs

- **Duplicated logo files in `app/webApp/resources/`** → Light/dark logo PNGs already exist in `app/shared/composeResources/drawable/`; copying them to `app/webApp/resources/` (or serving them alongside `favicon.ico`) ensures instant `<img>` rendering in the HTML form with zero Compose resource decoding overhead.
- **Password manager autofill timing (before user interaction)** → Some browsers autofill inputs on page load without firing `input` until blur/submit, or fire `change`. Listening to both `input` and `change` events AND reading the DOM `input.value` on `submit` before calling `viewModel.submit()` guarantees autofilled credentials are never missed.
- **Visual drift between HTML form and Compose** → Mitigated by driving all colors from `MaterialTheme.colorScheme` via CSS variables and keeping the layout minimal and Material-3-aligned (420px max width, rounded inputs, pill primary button).

## Migration Plan

Purely client-side UI change; no database migration, server deployment coordination, or API version bump required.
