# Design

## Context

`TokenStore` (`read`, `write`, `clear`) is the port each platform fills: Android Keystore, iOS Keychain and, on the web, `SessionStorageTokenStore` (`sessionStorage`, key `opensecretsanta.token`) created by `webTokenStore()`. `SessionManager.signIn` writes the token after a successful login, `changePassword` writes the replacement, and `dropSession`/`signOut` clear it; at start the manager reads the store and checks the token with the server. `Platform` exposes per-platform flags such as `hasPullToRefresh`. The token lives 7 days (`JwtTokenService.DEFAULT_LIFETIME_MILLIS`); a password change invalidates all earlier tokens. The earlier `web-session-persistence` design chose `sessionStorage` to keep the token's exposure short and noted `localStorage` as the option that survives closing the tab.

## Goals / Non-Goals

**Goals:**
- A web person can stay signed in across closing the tab or browser, by default, and can opt out at sign-in.
- Sign out, rejection and password change never leave a stale token behind.

**Non-Goals:**
- Cookies or other server work, a longer or shorter token life, a "remember me" on Android or iOS (they already persist), or syncing the choice between tabs beyond what shared `localStorage` gives.

## Decisions

1. **`write` learns whether to remember.** `TokenStore.write(token: String, remember: Boolean? = null)`: `true` means the longest-lived place the platform has, `false` the shortest, `null` keeps the token where it is now. Android, iOS and `MemoryTokenStore` ignore the argument. `SessionManager.signIn(username, password, remember = true)` passes it; `changePassword` writes with `null`.
2. **One web store with two storages.** `SessionStorageTokenStore` becomes `WebTokenStore` (the key is unchanged): `read` looks in `localStorage`, then in `sessionStorage`; `write(token, true)` sets `localStorage` and removes the session copy, `write(token, false)` the reverse, `write(token, null)` writes to whichever holds the token now (session storage if neither); `clear` removes both. Every access stays inside `try/catch`, so a blocked storage still reads as "nothing stored".
3. **Checkbox in the login view model.** `LoginUiState.staySignedIn` is `true` by default and `LoginViewModel.onStayChange` flips it; `submit` passes it to `signIn`. `Platform.canChooseToStaySignedIn` is `true` only on the web, and the login screen shows the checkbox only when it is.
4. **Login screen.** A `Checkbox` with the label "Stay signed in" between the password field and the button, with the same width as the fields; Enter in the password field still submits.
5. **Migration.** A token already in `sessionStorage` is read as before and stays there until the person signs in again; nothing is moved.
6. **Cross-tab.** `localStorage` is shared by tabs of the same browser. Signing out in one clears it, and another open tab keeps its in-memory session until its next request is rejected or it is refreshed. Accepted: it is the same person on the same browser.

## Risks / Trade-offs

- A token that outlives the tab is readable by a script running on the page for up to 7 days; the app builds no HTML from user text, which keeps this small, and the person can untick the box on a shared computer.
- An administrator who leaves the default on a shared computer stays signed in as an administrator; the box, the 7-day limit and sign-out are the controls.
- The default being ticked changes existing behaviour for web users who relied on the tab closing the session; they can untick it.
