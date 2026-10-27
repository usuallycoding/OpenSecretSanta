# Design

## Context

`app/shared` is a Compose Multiplatform library for Android, iOS (device and simulator) and wasmJs. It already has Koin wired through `initKoin(baseUrl, platformModule)`, but only a template screen: no HTTP client, no navigation, no session. The three hosts (`androidApp`, `iosApp`, `webApp`) are thin and call `initKoin` and `App()`. `core/contract` holds the DTOs and `ApiRoutes` that the server also uses, and `core/domain` holds `PasswordPolicy` and `Username`. The server enforces the forced-password gate with 403 `password_change_required`, locks usernames with 429 and `Retry-After`, and has no CORS. The web host assumes the server serves it (`window.location.origin`). See proposal.md for motivation and specs/ for behavior.

## Goals / Non-Goals

**Goals:**
- One shared code path for requests, session handling and screens, with platform code limited to token storage and the web server address.
- Screen logic that is testable in common code without a UI harness.
- A shell the later participant and admin changes can fill in without reworking login, session or navigation.

**Non-Goals:**
- Participant and admin screens, profile editing, notifications, web token persistence, refresh tokens, server-side sign out.
- A localization system. Strings live in one place so one can be added later.
- Docker, images and CI (a later stage).

## Decisions

### D1: Layout inside `app/shared`
```
 data/api/      ApiClient, ApiResult/ApiError, session-aware request helper
 data/session/  TokenStore (interface), SessionManager, SessionState
 data/web/      parse of config.json (pure function, common)
 ui/            App (root), theme, Strings
 ui/login/      LoginViewModel + LoginScreen
 ui/password/   ChangePasswordViewModel + ChangePasswordScreen
 ui/shell/      ShellViewModel + placeholder participant/admin views, role switch
 src@android/   AndroidTokenStore, src@ios/ KeychainTokenStore, src@wasmJs/ MemoryTokenStore
```
It follows the `data` / `di` / `ui` split already in the module. A screen is a stateless composable fed by a view model's `StateFlow`, so tests never need Compose. *Alternative:* a separate `app/client` module for non-UI code; rejected for now, because one module keeps the build simple and the split can be made when it hurts.

### D2: HTTP client and errors
One Ktor `HttpClient` with `ContentNegotiation(json)` and no explicit engine: each platform's engine (OkHttp, Darwin, Js) is a platform dependency and Ktor picks the one on the classpath. `expectSuccess` stays off; `ApiClient` maps every response to `ApiResult<T>`:
```
 Success(value)
 Failure(ApiError)
   Unauthorized                  401
   PasswordChangeRequired        403 + code password_change_required
   Validation(field, message)    400 with ErrorResponse.field
   RateLimited(retryAfterSeconds) 429 + Retry-After
   Rejected(code, message)       other ErrorResponse (404, 409, ...)
   Network                       no response / timeout
   Unexpected                    anything unparsable
```
Request and response types are the `core/contract` DTOs and the paths are `ApiRoutes`, so the client cannot drift from the server. Authenticated calls go through one helper that adds `Authorization: Bearer` from the `SessionManager` and nothing else; the password is only ever a field of `LoginRequest` and `ChangePasswordRequest`. *Alternative:* Ktor's `Auth` bearer plugin; rejected because its refresh model does not match this API (no refresh tokens) and we need to intercept 401 and 403 ourselves.

### D3: Session as a state machine
`SessionManager` owns a `StateFlow<SessionState>`:
```
            start (native: token found)               start (no token / web)
 Loading ------------------------------> SignedIn/PasswordChangeRequired     SignedOut
    |   GET /me 200                         ^   ^                                  ^
    |   GET /me 401 -> clear token ---------+---+----------------------------------+
    v
 SignedOut(note?) --login 200--> SignedIn(account)  or  PasswordChangeRequired(account)
 PasswordChangeRequired --PUT /me/password 200 (new token)--> SignedIn
 any state --401 anywhere--> SignedOut("signed out")      --sign out--> SignedOut
 SignedIn --403 password_change_required--> PasswordChangeRequired
```
The root composable is a `when` over this state, so a rejected token or a hit gate moves the whole UI from one place instead of every screen handling it. The account (`GET /me`, with `isAdmin` and `mustChangePassword`) is read after every login, after a token is restored and after a password change; it is never taken from the token. *Alternative:* each screen reacting to 401 itself (duplicated and easy to forget).

### D4: Token storage behind one interface
`TokenStore { suspend read(): String?; suspend write(token); suspend clear() }`, supplied through the existing `platformModule` hook of `initKoin`, so common code never names a platform.
- **Android:** an AES-GCM key in the Android Keystore encrypts the token; the ciphertext and IV sit in a private `SharedPreferences` file. Any failure to decrypt (key lost, backup restored onto a new device) is treated as "no stored token". The preferences file is excluded from backup because a restored blob could never be decrypted anyway. *Alternative:* `androidx.security:security-crypto`, which is deprecated.
- **iOS:** a generic-password item in the Keychain (`kSecClassGenericPassword`, accessible after first unlock, this device only), through Kotlin/Native's built-in `platform.Security` bindings, no extra library.
- **Web:** an in-memory holder. Nothing is written to `localStorage` or `sessionStorage`; a script on the page could read a JWT there. Persistence on web (for example an httpOnly cookie plus refresh) is a separate later design.

Storage failures never crash: reads become `null`, writes keep the token for the current run only (the `SessionManager` holds the token in memory in every case and uses the store only to survive restarts).

### D5: Navigation without a library, for now
The screens are few and all hang off `SessionState`, so navigation is `when (state)` plus one `View { Participant, Admin }` choice kept in the shell view model for the current run. No back stack is needed yet. The participant and admin changes will add nested destinations and may adopt the Compose navigation library at that point; keeping this change free of it keeps the decision with the change that actually needs back navigation. The admin starts in the admin view; a non-admin never gets a `View.Admin` value to select.

### D6: View models and Koin
Each screen has a view model on the lifecycle library already in the module (`viewModel` from `lifecycle-viewmodel-compose`), created through Koin (`koin-compose-viewmodel`). View models take the `SessionManager` or `ApiClient` and expose a single `StateFlow` of UI state plus plain functions for events. Time and clipboard-like platform needs are not used here. The Koin graph test already in `app/shared/test@android` is extended to resolve the new definitions.

### D7: Server address
- **Android and iOS:** unchanged, the hosts pass their development address to `initKoin`.
- **Web:** before the first composition the host fetches `config.json` (relative URL, with a time in the query so a cached copy is never used) and passes `baseUrl` from it, or `window.location.origin` when the file is missing, not JSON or has no non-blank `baseUrl`. The parse is a pure common function (`resolveBaseUrl(configText, origin)`) so it is unit-tested; only the `fetch` lives in `main.kt`. A sample `config.json` ships with the web app pointing at the origin's default development server. One build therefore works for any server address, including the later container image.

### D8: Cleartext development traffic
Android 9+ and iOS block plain `http://` by default, which would stop the emulator and simulator from reaching the development server. The Android host adds a network security config that allows cleartext only for `10.0.2.2` and `localhost`, and the iOS `Info.plist` sets `NSAllowsLocalNetworking`. Any other address must be `https`; nothing global is relaxed. Allowing the connection is not the same as being permitted to make it: see D11 for Android 17.

### D9: Server CORS
`ServerConfig` gains `corsAllowedOrigins: List<String>`, parsed from `CORS_ALLOWED_ORIGINS` (comma-separated, blanks ignored). Each entry must be `scheme://host[:port]` with no path, query or wildcard; anything else is a `ConfigurationException` naming the variable. When the list is empty the plugin is not installed at all, so default behavior is byte-for-byte what it was. Otherwise Ktor's `CORS` plugin is installed with `allowHost` per origin, the `Authorization` and `Content-Type` headers, and the methods the API uses (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`), no credentials (tokens travel in a header, not cookies). Preflight is answered by the plugin before authentication and before the login rate limit, which only guards `POST /auth/login`. *Alternative:* `*`; refused, since the API is authenticated and the list of web origins is known to whoever deploys it.

### D10: Strings, theme, tests
User-visible text lives in one `Strings` object (English), so every message in the specs has a single definition and a later localization has one place to start. The theme is `MaterialTheme` following the system light or dark setting. Tests are common and need no UI: `ApiClient` against Ktor's `MockEngine` (every status mapping, `Retry-After`, bearer header present only after sign-in, password only in the two allowed bodies), `SessionManager` with a fake `TokenStore` (every arrow in the D3 diagram, a failing store), the three view models, `resolveBaseUrl`, and on the server a CORS test per scenario and config tests. Platform storage classes get small tests where the platform allows (Android instrumented-free logic around the cipher is kept thin); the screens are verified by running the app.

### D11: Local network permission on Android
Android 17 (API 37) requires the runtime permission `android.permission.ACCESS_LOCAL_NETWORK` before an app may reach a private-network address. Without it the connection does not fail, it hangs until the client's connect timeout, which looks like "server unreachable". Found when the app was first run on an API 37 emulator.
- **Which addresses:** a pure common function `isLocalNetworkAddress(baseUrl)` (loopback, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, link-local, `.local`, a name without a dot) decides, so it is unit-tested without Android. The emulator's `10.0.2.2` is inside `10.0.0.0/8`.
- **Where it runs:** `App` takes an optional wrapper, `App(gate: @Composable (content: @Composable () -> Unit) -> Unit = { it() })`, and the session is restored only inside it. The Android host passes a gate that, when `Build.VERSION.SDK_INT >= 37`, the address is local and the permission is not granted, shows an explanation, launches the system request through the activity-result API and shows a denied screen with "Try again" and, once the system will no longer ask (`shouldShowRequestPermissionRationale` is false after a denial), "Open settings". Everywhere else the gate shows the app unchanged, so the common code, iOS and web never see it.
- **Why a gate and not a retry after failure:** the first request would hang for the whole connect timeout before the person learned anything.
- **Alternatives:** a common `Permission` interface (nothing else needs it yet); catching the timeout and then asking (slow, and a real outage would be mistaken for a missing permission).

## Risks / Trade-offs

- [Ktor client dependency names and engines in Amper's catalog (OkHttp, Darwin, Js, MockEngine) may not match what I expect] → The first task adds them and builds all targets before anything else depends on them.
- [Keychain and Keystore cannot be fully exercised in common tests] → Keep each implementation to a few lines, test the `SessionManager` against a fake store, and verify the real ones by running on the Android emulator and iOS simulator.
- [Web users sign in again on every refresh] → Accepted and stated in the spec; persistence is a separate follow-up.
- [Hand-rolled navigation may need replacing] → It is a single `when`; the participant change introduces real navigation when it needs a back stack.
- [CORS misconfiguration silently breaks the web app] → Strict origin parsing that fails at startup, a test per scenario, and a README note including the exact origin form (scheme, host, port).
- [Cleartext allowance leaks into release builds] → Limited to the two development host names, so a production server address that is `https` is unaffected; any other `http` host stays blocked.
- [A promoted account only sees the admin view after the next start or sign-in] → Matches the spec; refreshing `GET /me` periodically can be added later.

- [The permission constant exists only on API 37] → The gate checks `SDK_INT >= 37` before touching it, so older systems are unaffected.
- [The system stops showing the request after repeated denials] → The denied screen offers "Open settings" then.
- [The prompt is shown for a development server a person never uses on a real device] → It only appears for private-network addresses; a public `https` server never triggers it.

## Migration Plan

Deploy normally. The server change is additive and off by default; set `CORS_ALLOWED_ORIGINS` only when the web app is served from another origin. The clients are new, so there is nothing to migrate. Rollback is restoring the previous server build and not shipping the new clients.
