# Design

## Context

- The server is Ktor on the JVM with SQLite and append-only migrations (`V001`..`V006`). Optional outbound integrations follow one pattern: a domain port (`Mailer`), a `java.net.http` implementation (`ResendMailer`), a `DisabledMailer`, a Koin module that picks one from `ServerConfig`, and a rule that a failed send never undoes the action (`SendDrawEmails` runs after `ActivateGroup` in `LifecycleRoutes`).
- The apps are Compose Multiplatform from `app/shared`, built with the standalone Kotlin Toolchain (`module.yaml`), not Gradle. Platform pieces are injected through Koin modules (`tokenStoreModule`, `settingsStoreModule`) with the implementations under `src@android` / `src@wasmJs` / `src@ios`. The web app is Kotlin/Wasm; its `resources/index.html` is custom, `config.json` is fetched at start and rewritten by `40-write-config.sh` at container start, and its addresses are `#/groups/7` style (`WebAddresses`).
- `ShellMenu` in `ShellScreen.kt` already holds Appearance, Change password and Sign out, and `AppearanceSheet` shows the bottom-sheet pattern (`SheetFrame`).
- The build tool runs the Google Services plugin itself when `google-services.json` sits in the Android module's root (task 1.1 saw its `processDebugGoogleServices` step), so Firebase starts without any initialisation code.
- Browsers allow push only on HTTPS or `localhost`. Production will be `santa.plm360.org` behind TLS later; this change is developed and tested on localhost.
- Already in place and gitignored: `secrets/` with the service-account key, `app/androidApp/google-services.json` (project `open-secret-santa`, package `com.usuallycoding.opensecretsanta`). `app/webApp/resources/firebase-messaging-sw.js` currently holds only the web config.

## Goals / Non-Goals

**Goals:**
- One server send path (FCM HTTP v1) for Android and web.
- Preferences per person on the server, device registration per device, the master switch per device.
- Work on localhost end to end, with the same Docker image working on any address later.

**Non-Goals:**
- iOS (no paid Apple account), email or SMS fallback, notification history or an inbox in the app, per-group mute, quiet hours, retrying failed sends, a queue that survives a server restart.
- TLS for the Raspberry Pi deployment.

## Decisions

### Data-only FCM messages, rendered by our own code
The server sends FCM messages with a `data` map only (`category`, `groupId`, `title`, `body`) and no `notification` block, at high priority on Android. The Android messaging service and the web service worker both build the visible notification from it.
- Why: one wording and one tap behaviour on both platforms, the title and body are decided by the server and always generic, and Android channels and the tap target are under our control.
- Alternative: `notification` messages, which the system shows by itself. Rejected: on web the click target must be an https `link`, which does not work for `http://localhost`, and Android would route taps through the launcher with no group.

### Server: FCM over `java.net.http`, no Firebase Admin SDK
A `PushSender` port in the domain (`enabled`, `send(messages)` returning a result per message: sent / invalid token / failed), a `FcmPushSender` that signs an RS256 JWT with the service account's private key (JDK `java.security`, no library), exchanges it at the key's `token_uri` for an access token with the `firebase.messaging` scope, caches it until shortly before expiry, and posts each message to `https://fcm.googleapis.com/v1/projects/{project_id}/messages:send`. A `DisabledPushSender` is used without a key.
- Why: matches `ResendMailer` (small, testable against a stand-in URL like `RESEND_API_URL`) and avoids the heavy Admin SDK and its transitive dependencies.
- Alternative: Firebase Admin SDK. Rejected for weight and because the two calls we need are simple.
- Errors: `404 UNREGISTERED` / `400 INVALID_ARGUMENT` for the token means the registration is deleted; `401`/`403`, timeouts and `5xx` are logged by status only and never retried. Logs never carry the key, tokens or message text.
- Config: `FCM_SERVICE_ACCOUNT_FILE` (a path). A set but unreadable or malformed file stops startup naming the variable. `ServerConfig.toString()` stays free of it. A test-only `FCM_API_URL` / token-URL override mirrors `RESEND_API_URL`.

### Delivery is after the fact and off the request path
Routes call a server-side `NotificationDispatcher` after the use case succeeded, like `SendDrawEmails`. The dispatcher hands the work to a domain `Notifier` on a small server-owned executor (the domain module is multiplatform and has no locks or threads), and also owns the wishlist debounce. The notifier works out the recipients (who is whose Santa, via the assignment data), filters by each recipient's preference for the category, loads their devices and hands the messages to the sender. It runs on a small server-owned executor with a time limit per send, so a slow FCM never delays the response; tests inject a direct executor.
- Why: the spec requires the action to be stored first and never to depend on the push. Hooking into the routes keeps the use cases unchanged and free of delivery concerns.
- Recipients: moved out of Draft = all participants minus the actor; added to a group = the usernames newly added to a Pending group (`SetParticipants` reports them in its result; in Draft nothing is sent because the group is invisible to participants); drawn = all participants minus the actor; wishlist = the Santa of the editor (none unless Active); question = the asker's receiver; answer = the answerer's Santa.

### Wishlist notifications are debounced in memory
Wishlist add/update/delete schedule a notification for (Santa, group) after 60 seconds without another change, at most 5 minutes after the first. A later change inside the window just extends it. The state is in memory and lost on restart; the scheduler is injected so tests do not wait.
- Why: someone filling in a wishlist makes many small edits; one push per edit would be noise.
- Alternative: persist pending notifications. Rejected as more machinery than a lost "your receiver changed their list" justifies.

### Storage
`V007__notifications.sql`:
- `push_devices(token PRIMARY KEY, username REFERENCES users, platform CHECK IN ('android','web'), created_at)` plus an index on `username`. A token is the primary key, so registering it again for another person is an upsert that moves it.
- `notification_prefs(username PRIMARY KEY REFERENCES users, announcements, wishlist, questions, answers INTEGER NOT NULL DEFAULT 1)`. A missing row means all four on; the first change inserts it.
- Deleting a user must delete both; the existing user-deletion path is checked for how other tables are handled.

### API
Under `/notifications` in `ApiRoutes`, all behind the existing access-token authentication and the password-change gate:
- `GET /notifications` → `{ available, announcements, wishlist, questions, answers }`; `available` is whether push is configured.
- `PUT /notifications` with the four booleans (all four required) → the saved values; anything else is 400.
- `PUT /notifications/devices` with `{ token, platform }` → 204; `POST /notifications/devices/remove` with `{ token }` → 204 (no body on DELETE, which proxies such as nginx may drop).
Removing a token that is not the caller's is a quiet 204 and removes nothing.

### Client: one shared sheet, a per-platform device
`NotificationsSheet` (shared, built on `SheetFrame`, next to `AppearanceSheet`) and a `NotificationsViewModel` talk to the API and to a `PushDevice` interface supplied per platform through a Koin module, as with `TokenStore`:
- `isSupported`, `permissionGranted()`, `enable(): String?` (asks the permission if needed, returns the push token or null when refused), `disable()`.
- Android implementation in `src@android` (Firebase Messaging, `POST_NOTIFICATIONS` request routed through the activity, which owns the permission launcher); web implementation in `src@wasmJs` (JS interop); iOS a stub with `isSupported = false` and the menu item hidden.
- The master switch's "on" is remembered locally in the existing `SettingsStore` and shown only if the permission is still granted; on start-up with it on, the app re-registers the current token (idempotent) so a token the server dropped comes back.
- Sign-out: `SessionManager.signOut()` first asks the device to unregister (a short, best-effort call while the token is still valid), then clears the local flag, then drops the session. A session that ends by itself (401) cannot call the server; the stale registration is replaced when the next person turns notifications on on that device, which moves the token (see the registration rule), and is removed when FCM reports it invalid.
- Token refresh: Android's `onNewToken` and the web SDK's refresh re-register when the master is on.

### Android specifics
- Firebase is started by the Google Services plugin from `app/androidApp/google-services.json` (gitignored); no code initialises it, and calling `FirebaseApp.initializeApp` by hand crashes because the default app already exists. Dependencies come from the Firebase BoM (`bom: com.google.firebase:firebase-bom:…`) plus `com.google.firebase:firebase-messaging` in `module.yaml`.
- A `FirebaseMessagingService` handles `onMessageReceived` (foreground and background, since the messages are data-only) and `onNewToken`.
- Four notification channels (announcements, wishlist, questions, answers) are created at start-up so the system settings mirror the in-app toggles. The in-app toggles remain authoritative because the server filters.
- A monochrome small icon is added; tapping opens `MainActivity` with the group id, which navigates to that group once signed in, and to the group list when the group is not visible to the person.
- `POST_NOTIFICATIONS` is declared in the manifest and requested only from the sheet.

### Web specifics
- `firebase-messaging-sw.js` stays in `app/webApp/resources/` (it must be served from the site root) and is rewritten: it imports the Firebase messaging service-worker bundle from the pinned gstatic URL, initialises Firebase from values passed in its own URL query when the page registers it (a service worker must set its handlers synchronously, so it cannot fetch `config.json` first), shows a notification from the data map in `onBackgroundMessage`, and on click focuses or opens the app at `#/groups/{id}`. It no longer contains hard-coded keys.
- A small plain script, `push.js`, in the same folder is loaded from `index.html`. It exposes support detection, the permission request, the service-worker registration and token fetch (`getToken` with the VAPID key from `config.json`) and token deletion, and loads the Firebase web SDK by dynamic import from a pinned gstatic version only when the person turns notifications on. Kotlin/Wasm calls it through `js(...)` interop in `src@wasmJs`.
- `config.json` gains an optional `firebase` object (the public web settings and the VAPID public key). `40-write-config.sh` writes it from `PUBLIC_FIREBASE_*` variables when they are set; the repo's `config.json` for `./kotlin run` carries the project's public web settings so localhost works without Docker. `nginx.conf` serves `firebase-messaging-sw.js` and `push.js` with `no-cache` so an update is picked up.
- The page shows notifications only when the app is in the background, as the SDK does; in the foreground the page still shows one through `ServiceWorkerRegistration.showNotification` for consistency with Android.

### Docker
`docker-compose.yml` mounts `./secrets/${FCM_KEY_FILE}` (default name documented in `.env.example`) read-only into the server at a fixed path and sets `FCM_SERVICE_ACCOUNT_FILE` to it only when the variable is set; `PUBLIC_FIREBASE_*` go to the web service only. Nothing from `secrets/` enters any image (a `.dockerignore` entry keeps it out of the build context, which today is the whole repository with `COPY . .`).

## Risks / Trade-offs

- [Kotlin/Wasm ↔ service-worker interop is the least-trodden part] → keep the JS surface tiny (`push.js`) and test with the real browser early; the service worker is plain JS, outside Wasm.
- [A service-account key copied into an image or `webApp/resources` would be public] → `secrets/` is gitignored, `.dockerignore` excludes it, and a task checks the built web image does not contain it.
- [A session that ended by itself leaves a registration behind on a shared device] → the token moves to whoever enables notifications next, is removed when FCM reports it invalid, and the lock-screen text is generic anyway.
- [In-memory debounce loses pending wishlist notifications on restart] → accepted; at worst one "wishlist changed" push is missed.
- [FCM data-only messages can be delayed by Android battery optimisation] → inherent to push; high priority is set, and the app still shows everything when opened.
- [Web push is untestable without HTTPS or localhost] → development is on localhost as agreed; the Pi deployment needs TLS before the web side works there.
- [The repo's `config.json` and the Android file carry the project's public Firebase client settings] → these are client identifiers, not secrets; restrict the API keys in the Google Cloud console to the app and the web origins when the real domain exists.

## Migration Plan

1. Deploy the new server: `V007` is applied at start-up; with no `FCM_SERVICE_ACCOUNT_FILE` nothing changes for anyone.
2. Place the key in `secrets/`, set `FCM_SERVICE_ACCOUNT_FILE` and the `PUBLIC_FIREBASE_*` variables in `.env`, restart.
3. Ship the new web and Android builds; people opt in from the Notifications sheet.
- Rollback: remove the variables and restart (push turns off, tables stay unused); the migration is additive.

## Open Questions

- The exact wording of the five notification texts (to be written during implementation, within the generic-text rule).
- Whether the Android channels should also be offered in the sheet as a link to the system settings (a nicety that can follow).
