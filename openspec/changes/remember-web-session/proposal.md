# Proposal

## Why

On the web the sign-in lives in the tab's session storage, so closing the tab, or the browser, signs the person out and the next visit starts at the sign-in screen. Android and iOS already keep the person signed in across restarts. The web should do the same for people who want it, without forcing it on a shared computer.

## What Changes

- The web sign-in screen gets a "Stay signed in" checkbox, ticked by default. Native sign-in screens do not show it: the apps already keep the session.
- Signed in with the box ticked, the token is kept in the browser's local storage, so closing and reopening the tab or the browser finds the person still signed in until the server stops accepting the token (it lives 7 days).
- Signed in with the box unticked, the token stays in the tab's session storage as today: a refresh keeps the session, closing the tab ends it.
- Signing out, a rejected token and a changed password still clear the stored token, wherever it was kept. Changing the password keeps the token where it was.
- No server change and no new storage on the other platforms.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-app`: "Session storage per platform" now allows local storage on the web for a person who chose to stay signed in, and a new requirement describes the checkbox.

## Impact

- `app/shared/src/data/session/TokenStore.kt` (`write` can say whether to remember), `SessionManager.signIn`, `LoginViewModel`/`LoginScreen`, `Platform` (whether the platform offers the choice), `app/shared/src@wasmJs/` (the web store and its test), `Strings.kt`, the README.
