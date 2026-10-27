# Tasks

## 1. Server: opt-in CORS

- [x] 1.1 Add `corsAllowedOrigins` to `ServerConfig`, parsed from `CORS_ALLOWED_ORIGINS` (comma-separated, blanks ignored, each entry `scheme://host[:port]` with no path, query or `*`, otherwise `ConfigurationException` naming the variable); verify config tests for unset, blank, one and several origins, a wildcard, a missing scheme, a trailing path and surrounding spaces
- [x] 1.2 Install Ktor's CORS plugin only when the list is non-empty (`allowHost` per origin, headers `Authorization` and `Content-Type`, methods `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, no credentials); verify server tests for a preflight from a listed origin, a request from an unlisted origin getting no allow headers, the default having no cross-origin headers at all, a preflight needing no token, an unauthenticated listed-origin request still being 401, and that the login rate limit is untouched
- [x] 1.3 Document `CORS_ALLOWED_ORIGINS` (accepted form, default, when to set it) in the README's configuration table and verify with `curl -i -X OPTIONS -H "Origin: ..." -H "Access-Control-Request-Method: PUT"` against a real server; `./kotlin test -m server` passes

## 2. Client foundation: dependencies and API client

- [x] 2.1 Add the Ktor client (core, content negotiation, kotlinx JSON; OkHttp for Android, Darwin for iOS, Js for wasm; MockEngine for tests) and `koin-compose-viewmodel` to `app/shared`, and drop the template `Greeting`/`compose_multiplatform` screen content; verify `./kotlin build -m shared` succeeds for Android, iOS and wasm
- [x] 2.2 Implement `ApiResult`/`ApiError` and `ApiClient` (login, current account, change password) over `ApiRoutes` and the `core/contract` DTOs, with the shared authenticated-request helper that adds only the bearer header; verify common tests with `MockEngine` for success, 401, 403 `password_change_required`, 400 with a field, 429 with `Retry-After`, other `ErrorResponse` bodies, no connection, an unparsable body, the bearer header present only when a token exists, and the password appearing only in the login and password-change bodies

## 3. Session

- [x] 3.1 Add the `TokenStore` interface and `SessionManager` with `SessionState` (`Loading`, `SignedOut(note)`, `PasswordChangeRequired`, `SignedIn`), start-up restore through `GET /me`, sign-in, password change that swaps the token, sign-out, central handling of 401 and of 403 `password_change_required`, and the in-memory token that survives a failing store; verify common tests with a fake store for every transition in the design's diagram, a stale stored token being deleted, a store that throws on read or write, sign-out clearing the store and the next sign-in showing nothing of the previous account
- [ ] 3.2 Implement the three token stores (Android Keystore + AES-GCM in private preferences excluded from backup, iOS Keychain generic password this-device-only, web in-memory) and supply each through the platform module in its host; verify by running on the Android emulator and the iOS simulator that a login survives closing and reopening the app and a sign-out removes it, that the web token is gone after a refresh, and that nothing is written to browser storage

## 4. Screens and shell

- [x] 4.1 Add `Strings`, the theme (system light or dark) and the root `App` that shows loading, login, password change or the shell from `SessionState`; verify the app starts on all three platforms and shows the login screen when signed out
- [x] 4.2 Implement the login view model and screen (blank fields and in-progress disable the action, generic 401 message with username kept and password cleared, 429 message with minutes from `Retry-After`, unreachable-server message, "you were signed out" note); verify common view-model tests for each outcome and a manual run against a real server
- [x] 4.3 Implement the password-change view model and screen (current, new and repeat; at least 8 characters via `PasswordPolicy`, must differ, must match; server field errors shown on the matching field; success continues to the first view); verify common tests for each check, a refusal naming a field, and success replacing the token, plus a manual run with a temporary password
- [x] 4.4 Implement the shell with the participant and admin placeholder views, the switch (admins only, starting in the admin view, lasting for the run) and sign out, driven by `isAdmin` from `GET /me`; verify common tests that a non-admin can never select the admin view, an admin starts in it and can switch both ways, and a manual run as an admin and as a normal participant

## 5. Web and native hosts

- [ ] 5.1 Add `resolveBaseUrl(configText, origin)` and have the web host fetch `config.json` before starting (missing file, invalid JSON or blank `baseUrl` fall back to the page origin), with a sample `config.json` in the web resources; verify common tests for every fallback and a manual run of the web app against a server on another origin with `CORS_ALLOWED_ORIGINS` set
- [ ] 5.2 Allow cleartext traffic only to the development hosts (Android network security config for `10.0.2.2` and `localhost`, iOS `NSAllowsLocalNetworking`); verify the Android emulator and iOS simulator reach a local server over `http`, and that no other `http` host is allowed by the config

## 5b. Android local-network permission

- [x] 5.3 Add the pure common `isLocalNetworkAddress(baseUrl)`; verify common tests for loopback, `10.x`, `172.16` to `172.31` (and `172.15`/`172.32` being public), `192.168.x`, link-local, `.local`, a name without a dot, `localhost`, a public host and a public `https` URL, an IPv6 loopback, an address with a port or path, and a malformed URL
- [x] 5.4 Add the `gate` slot to `App` and the Android gate in `androidApp` (explanation, system request on API 37+, denied screen with "Try again" and "Open settings", nothing on other versions or addresses) with its strings; verify on the Android emulator after `adb shell pm revoke com.usuallycoding.opensecretsanta android.permission.ACCESS_LOCAL_NETWORK`: the app shows the explanation and the system request before any request is sent, allowing continues to sign-in, denying shows the retry screen, and a build pointed at a public `https` address shows no prompt
- [x] 5.5 Replace the README's manual `adb pm grant` workaround with a description of the prompt (and keep the command as a way to pre-grant it for development); verify the README matches what the emulator showed

## 6. Integration

- [ ] 6.1 Extend the Koin graph test to resolve every new definition and document the client (running each platform, the web `config.json`, what is placeholder until the next changes) in the README; run `./kotlin build` and `./kotlin test` for the whole repo, then verify end to end with a real server: sign in as a normal user and as an admin on web and Android (and iOS if the simulator is available), change a temporary password, get signed out when a token is invalidated by a password reset from another session, and see lockout and unreachable-server messages, and on an Android 17 device see the local-network prompt appear before the first request
