# Proposal

## Why

The server is feature-complete for the exchange itself, but nothing a person can use exists: `app/shared` still holds the Compose Multiplatform template screen, with no way to talk to the server, no login and no session. Every participant and admin screen to come needs the same base: a client for the API, a session that survives (or deliberately does not survive) a restart, a place to put screens and a way in. Building that once, in shared code, keeps the three clients identical and lets the participant and admin screens follow as separate, shippable changes.

## What Changes

- **One UI for all three platforms.** Screens live in `app/shared` (Compose Multiplatform) and run unchanged on Android, iOS and web; the platform hosts stay thin.
- **API client.** A shared HTTP client over the existing `core/contract` DTOs and `ApiRoutes`, so the app and the server cannot disagree about a request or response. Server errors (`ErrorResponse`) become typed results the screens can react to.
- **Login and forced password change.** A login screen. When the server says the password is temporary, the app shows a password-change screen and nothing else until it is changed (the server enforces the same gate).
- **Session.** The access token is kept in the platform keystore on Android and in the Keychain on iOS, so a login survives closing the app. On web the token is kept in memory only, so a page refresh asks for the password again; persisting it on web is a later follow-up. An expired, rejected or replaced token returns the user to login.
- **Navigation shell with the admin/participant switch.** After login, a participant sees the participant view; an admin sees an admin view and a switch to the participant view (and back). The views are placeholders in this change; the participant and admin screens fill them in later changes. Sign out is available everywhere.
- **Server address.** Android and iOS keep their development defaults. The web app reads the server address from a `config.json` served next to it, falling back to the page's own origin, so one build works against any server.
- **Local-network permission on Android.** From Android 17 an app must hold the local-network permission before it may reach a private-network address (this machine's development server, or a server on a home or office network); without it a request just hangs. When the server address is such an address, the app explains why and asks for the permission before its first request, and explains what to do when it is denied. Public `https` servers and older Android versions never see the prompt.
- **Opt-in CORS on the server.** Because the web app may be hosted separately from the server, a new `CORS_ALLOWED_ORIGINS` variable lists the web origins the server allows. It is empty by default, which leaves today's behavior untouched.

Out of scope:
- Participant screens (groups, receiver, wishlist, questions, reveal) and admin screens (groups, lifecycle, password resets, oversight): separate changes that build on this one.
- Profile editing, notifications, persisting the web token, refresh tokens or a server-side sign out.
- Docker images, compose files and GitHub Actions, which come in a later stage.
- The matching iOS local-network prompt (iOS asks on its own).

## Capabilities

### New Capabilities
- `client-app`: how the shared app starts and finds the server, how a person signs in, changes a temporary password, keeps or loses their session, switches between the participant and admin views and signs out.

### Modified Capabilities
- `server-runtime`: gains an opt-in requirement for cross-origin access by browser clients (`CORS_ALLOWED_ORIGINS`), added as a new requirement without changing the existing ones.

## Impact

- Code: an optional wrapper slot in `App`, a pure address check in `app/shared` and the Android permission gate in `androidApp`; HTTP client, session and token-store interface with platform implementations, view models, navigation, theme and the login, password-change and placeholder screens in `app/shared`; small changes to the three platform hosts for the token store; `config.json` loading and a sample file in `app/webApp`; the CORS option in `server` and the README.
- APIs: none new. The app only uses `POST /auth/login`, `GET /me` and `PUT /me/password`. The server gains CORS response headers for configured origins only.
- Dependencies: Ktor client (common core and content negotiation, with the OkHttp, Darwin and Js engines) and the Ktor server CORS plugin.
- Systems: one optional environment variable, `CORS_ALLOWED_ORIGINS`.
