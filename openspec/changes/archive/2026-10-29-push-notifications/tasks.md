# Tasks

## 1. Spikes (before building on them)

- [x] 1.1 Android: add the Firebase messaging dependency to `app/androidApp` and log the token on a device or emulator; verify the app builds and a token appears in logcat. (Done: the build tool applies the Google Services plugin itself, so no manual initialisation is needed.)
- [x] 1.2 Web: with the current web settings, register a throwaway service worker from a page served by `./kotlin run -m webApp` on `localhost`, get a token with the VAPID key and receive a test message sent from the Firebase console; verify the notification appears and a tap reaches the page. (Done by the real implementation instead of a throwaway: web push works end to end on localhost.)
- [x] 1.3 Server key: write a throwaway check that the key in `secrets/` signs a JWT and the token endpoint returns an access token; verify a test message to the token from 1.1 is accepted by `messages:send` (this also shows whether the Cloud Messaging API (V1) is enabled).

## 2. Server: storage, config and the FCM sender

- [x] 2.1 Add `V007__notifications.sql` (`push_devices`, `notification_prefs`), list it in `index.txt`, and make user deletion remove their rows; verify with a migration test that a fresh database and an upgraded one reach the same schema and that deleting a user leaves no rows.
- [x] 2.2 Add the domain ports and models: `PushSender`, `PushMessage`, the four categories, `DeviceRepository`, `NotificationPreferences`, with SQLite implementations in `server/src/data`; verify with repository tests (upsert and move of a token, defaults when no row, update of one switch).
- [x] 2.3 Add `FCM_SERVICE_ACCOUNT_FILE` to `ServerConfig` (unreadable or malformed file stops startup naming the variable, nothing secret in `toString()`), `README.md`'s variable table and `.env.example`; verify with config tests for unset, valid, missing and malformed files.
- [x] 2.4 Implement `FcmPushSender` (JWT-signed access token cached until near expiry, one `messages:send` per message, unregistered/invalid token reported separately, other failures logged by status only) and `DisabledPushSender`, wired in a Koin module like `emailModule`; verify with tests against a stand-in HTTP server for success, a 404 `UNREGISTERED`, a 5xx, a timeout, and that no log line contains the key, a token or the text.

## 3. Server: events, API and delivery rules

- [x] 3.1 Implement the `Notifier` (recipients per event, preference filter, device lookup, removal of dead tokens, never throws, pure logic in the domain, run off the request thread by a server-side dispatcher with an injectable executor) and the five generic texts; verify with unit tests for each category, a switched-off category, several devices, a dead token among live ones, and that no text contains a Santa's name, question or answer text, or wishlist content.
- [x] 3.2 Add the wishlist debounce (60 s quiet, 5 min cap, per Santa and group, injectable scheduler); verify with a test where three changes produce one notification, a reorder produces none and a Pending group produces none.
- [x] 3.3 Call the notifier from the routes after success: participants added (new names only, not the actor), activation (all but the actor), wishlist add/update/delete, ask question, answer question; verify with route tests that a refused action (wrong state, not a member) sends nothing and that a failing sender still returns the normal success response.
- [x] 3.4 Add `ApiRoutes` constants, DTOs and routes: `GET`/`PUT /notifications`, `PUT /notifications/devices`, `POST /notifications/devices/remove`, behind the access-token and password-change gates, with the 400/401 rules of the spec; verify with route tests including `available` false without a key, a token moving between users, and removing someone else's token being a no-op.
- [x] 3.5 Document the notification behaviour and the Firebase setup (project, key file, variables, what is public and what is secret) in `README.md`; verify the documented steps match `.env.example`.

## 4. Docker and secrets

- [x] 4.1 Add `secrets` to `.dockerignore`, mount the key file read-only into the server and pass `FCM_SERVICE_ACCOUNT_FILE` only when set, pass `PUBLIC_FIREBASE_*` to the web service only, and extend `40-write-config.sh` to write the `firebase` object into `config.json` (rejecting quotes, backslashes and spaces like `PUBLIC_SERVER_URL`); verify with `docker compose config` with and without the variables and by reading the generated `config.json`.
- [x] 4.2 Update `nginx.conf` so `firebase-messaging-sw.js` and `push.js` are served with `no-cache`, and list the new variables in `.env.example`; verify by `curl -I` against the running web container and by checking that `docker compose up --build` without the variables still starts both services.
- [x] 4.3 Verify the built web image and the web files do not contain the key (search the image's html folder for the service-account email and `private_key`).

## 5. Shared client: sheet, menu and registration

- [x] 5.1 Add the API client calls and models for the notification endpoints, the `PushDevice` interface (supported, permission, enable, disable) with an iOS stub, a Koin module for it, and strings for the sheet; verify with view-model tests using a fake device and a mock engine.
- [x] 5.2 Implement `NotificationsViewModel`: load switches and `available`, master switch flow (permission refused returns to off with a message, success registers, off unregisters), toggles saved at once and reverted on failure, toggles disabled while the master is off, master remembered locally and re-registered at start while granted; verify with tests for each scenario in `client-notifications`.
- [x] 5.3 Add `NotificationsSheet` on `SheetFrame` and the "Notifications" item in `ShellMenu` for participant and admin views, hidden where the device is unsupported (iOS); verify by running the web app on localhost and opening the sheet in both views, and that iOS shows no item.
- [x] 5.4 Make sign-out unregister the device first (best effort, bounded wait, never blocking sign-out) and forget the local flag; verify with a session-manager test for online, offline and a failing server, and that a second user on the same device sees the master off.

## 6. Android

- [x] 6.1 Implement the Android `PushDevice`: `POST_NOTIFICATIONS` request through the activity (manifest entry added), token from Firebase, delete token on disable, and re-registration on `onNewToken`; verify on an emulator running Android 13+ that the permission prompt appears only after turning the switch on.
- [x] 6.2 Add the `FirebaseMessagingService` that builds the notification from the data message, four channels created at start-up, a monochrome small icon, and a tap that opens the app on the group (group list when unavailable); verify on a device that a message from the server shows with the app open, in the background and closed, and the tap opens the right group.
- [x] 6.3 Point the debug build at the local server for the test (reuse the `192.168.0.172` / `10.0.2.2` settings already allowed) and run the full flow on an emulator: enable, ask a question from the web as a Santa, receive it; verify each of the four categories arrives once and a switched-off one does not.

## 7. Web

- [x] 7.1 Rewrite `firebase-messaging-sw.js` (Firebase initialised from its URL query, notification from the data map, click focuses or opens `#/groups/{id}`, no hard-coded keys) and add `push.js` (support check, permission, registration of the worker with the settings in its URL, token and deletion, Firebase loaded lazily from a pinned version); load it from `index.html`; verify in the browser that nothing loads from Firebase until the switch is turned on.
- [x] 7.2 Implement the web `PushDevice` in `src@wasmJs` over `push.js`, and add the `firebase` block (public settings and VAPID key) to the repo's `config.json` and to the config reader; verify with the sheet on localhost: unsupported or denied states show their message, enabling registers a token on the server.
- [x] 7.3 Run the full flow in two browsers or a browser plus the emulator: the group is drawn, the wishlist changes (one notification after the quiet period), a question and an answer are sent; verify each arrives once with the generic text, a tap opens the group, and a switched-off category sends nothing.

## 8. Integration

- [x] 8.1 Run the whole stack with `docker compose up --build` on localhost with the Firebase variables set, repeat the Android and web flows against it, then again with the variables removed; verify push works in the first run and is reported unavailable (sheet disabled, no errors in the server log) in the second.
- [x] 8.2 Run `openspec validate push-notifications` and the full test suite for server, shared and the platform modules; verify they pass.

## Workflow follow-up

- Archive the change after review, then restrict the Firebase API keys to the real domain once `santa.plm360.org` has TLS.
