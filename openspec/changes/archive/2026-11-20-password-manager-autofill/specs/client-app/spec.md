# Spec Delta

## ADDED Requirements

### Requirement: Web sign-in is a real HTML form for password managers
On the web, the sign-in screen SHALL render as an HTML `<form>` containing a username `<input autocomplete="username">`, a password `<input type="password" autocomplete="current-password">`, and a real submit `<button type="submit">`. Submitting the form SHALL prevent page navigation and run the normal sign-in flow, preserving the light/dark logo, stay-signed-in option, password reveal toggle, background auto-hide, errors, download link, and app version.

#### Scenario: Browser password manager detects the login form
- **WHEN** the sign-in screen is shown on the web
- **THEN** the DOM contains a `<form>` with `<input autocomplete="username">`, `<input type="password" autocomplete="current-password">`, and `<button type="submit">`

#### Scenario: Autofill populates and submits cleanly
- **WHEN** a password manager fills the username and password inputs and the person submits the form
- **THEN** the app signs in through `POST /auth/login` without reloading the page and removes the `<form>` once signed in

#### Scenario: Revealed password hides when tab leaves foreground
- **WHEN** the person reveals the password on the web sign-in form and switches away from the browser tab
- **THEN** the password input returns to `type="password"`

### Requirement: Web password-change forms are real HTML forms for password managers
On the web, both the mandatory password-change screen and the Settings password-change sheet SHALL render as an HTML `<form>` containing a visually hidden readonly `<input autocomplete="username">` set to the signed-in username, a current-password `<input type="password" autocomplete="current-password">`, two new-password `<input type="password" autocomplete="new-password">` inputs, and a real submit `<button type="submit">`.

#### Scenario: Forced password change exposes username and new-password hints
- **WHEN** a user with a temporary password reaches the password-change screen on the web
- **THEN** the DOM contains a `<form>` with the signed-in username in a hidden readonly `autocomplete="username"` input, `autocomplete="current-password"`, two `autocomplete="new-password"` inputs, and a submit button

#### Scenario: Settings password-change sheet cleans up on close
- **WHEN** the person closes the Settings password-change sheet on the web
- **THEN** the HTML `<form>` overlay is removed from the DOM and typed values are discarded

### Requirement: Native sign-in and password-change fields declare autofill types
On Android and iOS, the sign-in username field SHALL declare username autofill semantics, current-password fields SHALL declare password autofill semantics, and new/repeat password fields SHALL declare new-password autofill semantics. Successful sign-in and password change SHALL commit the platform autofill context so the OS can offer to save or update credentials.

#### Scenario: Native sign-in fields expose autofill hints
- **WHEN** the sign-in screen is shown on Android or iOS
- **THEN** the username field exposes username autofill semantics and the password field exposes password autofill semantics

#### Scenario: Native password change commits autofill on success
- **WHEN** a password change succeeds on Android or iOS
- **THEN** the platform autofill context is committed so the password manager can offer to update the stored password
