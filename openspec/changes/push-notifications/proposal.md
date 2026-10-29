# Proposal

## Why

Everything in the app happens while nobody is looking at it: a group is drawn, a Santa asks a question, the receiver answers, a wishlist changes. People find out only by opening the app, so questions sit unanswered and wishlist updates are missed. Push notifications on Android and the web close that gap. iOS is out of scope: it needs a paid Apple developer account that this project does not have and does not plan to get.

## What Changes

- The server can send push notifications through Firebase Cloud Messaging (FCM HTTP v1) to Android devices and to browsers. It is off unless a service-account key is configured, like the Resend emails, and a failed send never fails the action that caused it.
- Each person has four notification categories they can switch on or off, kept on the server so the choice follows them across devices:
  - **Announcements**: being added to a group, and a group's names being drawn.
  - **Wishlist**: the person they are Santa for added, changed or removed a wishlist item.
  - **Questions**: their Santa asked them a question.
  - **Answers**: the person they are Santa for answered one of their questions.
- A device registers a push token with the server when the person turns notifications on for that device, and the token is removed when they turn them off, sign out, or the device stops accepting pushes. A person can have several devices.
- The app's top-bar menu gets a **Notifications** item that opens a bottom sheet with a master switch for the current device and the four category toggles. The master switch asks for the system permission (Android 13+ notification permission, the browser's permission) and registers or removes the device; the category toggles are disabled while it is off.
- Notification text is generic. It never names the Santa, so an anonymous Santa stays anonymous on a lock screen.
- Android receives and shows the notifications through a Firebase messaging service; the web app gets a service worker (`firebase-messaging-sw.js`) that shows them and opens the app when one is tapped.
- The Docker setup gets the Firebase settings: the service-account file mounted into the server only, and the public web Firebase settings written into the web app's `config.json` at start, so the same image works on any address.
- iOS gets nothing: the notification item is not offered there.

## Capabilities

### New Capabilities
- `push-notifications`: the server side: device tokens, per-person category preferences, which events notify whom and with what text, delivery through FCM that never affects the triggering action, and the API for registering devices and reading and changing preferences.
- `client-notifications`: the app side: the Notifications menu item and bottom sheet, the master switch with the system permission flow, the four toggles, token registration on Android and web, and showing and opening notifications.

### Modified Capabilities
- `docker-deployment`: the compose setup passes the optional Firebase settings (service-account file to the server, public web settings to the web container) and keeps them out of git.

## Impact

- **Server**: a new migration (`V007`) with a device-token table and a preferences table; new domain ports and use cases plus an FCM sender in the style of `Mailer`/`ResendMailer`; hooks in group participants, activation, wishlist, ask and answer use cases; new routes in `ApiRoutes`; new `ServerConfig` settings.
- **Contract**: new DTOs and route constants in `core/contract`.
- **App (shared, Android, web)**: a new sheet next to `AppearanceSheet`, a new menu entry in `ShellMenu`, per-platform notification code under `src@android` and `src@wasmJs`, strings; Android gets `FirebaseApp`/messaging dependencies, a messaging service and the notification permission in the manifest; the web app gets the service worker and JS interop.
- **Dependencies**: Firebase Android SDK (started by the Google Services plugin that the build tool applies when `google-services.json` is in the module) and the Firebase JS SDK in the service worker. No Firebase Admin SDK on the server.
- **Secrets and files**: the service-account key lives in the gitignored `secrets/` folder and is never served or committed; `google-services.json` and the web Firebase settings are public client settings.
- **Not affected**: iOS, the existing emails, authentication.
- **Production**: browsers only allow push on HTTPS (except `localhost`), so the web side works on localhost now and on `santa.plm360.org` once TLS is in front of it; setting that up is outside this change.
