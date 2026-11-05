# Proposal

## Why

The draw and announcement emails end with "Open the app: <address>", which opens the web app's front page and leaves the person to find the group the email is about. The link should go to that group. And on a phone that has the Android app installed, such a link should open in the app and not in the browser, which the app cannot do today: it answers no web address at all.

## What Changes

- The draw email and the announcement email link to the group that sent them, `<app address>/#/groups/<group id>`, instead of the bare app address. A trailing slash on the configured address makes no difference, and with no app address configured there is still no link.
- The Android app opens `https://santa.example.com` addresses itself, when the address is one it can verify belongs to the app: tapping the link in an email opens the group inside the app. Without the app installed, or on a device where the address is not verified, the link opens the web app in the browser as before.
- The app reads the group from the address with the mapping the web app already uses and opens it the way a tapped notification does: after sign-in, with the group list behind it, and also when the app is already open. Only group addresses open from a link; an admin address does not.
- The web container serves `/.well-known/assetlinks.json`, written when it starts from a new setting that holds the release certificate's SHA-256, so Android can verify the web address belongs to the app. Without the setting no file is served and nothing else changes.
- The README says how to read the release certificate's fingerprint with `keytool` and where to set it, and how to check or force the verification on a device with `adb`.
- No change to the server's API, to the web app's addresses, or to the app's way of reaching its server.

## Capabilities

### New Capabilities
- `client-app-links`: how the Android app takes over the web app's group addresses: which addresses open in the app, how it opens them, and what happens when it cannot or the address is not verified.

### Modified Capabilities
- `draw-emails`: the draw email links to the group that was drawn, not to the app's front page.
- `group-announcements`: the announcement email links to the group it is about.
- `docker-deployment`: the web container can serve the file that lets Android verify the web address as the app's, from an optional setting.

## Impact

- `core/domain/src/email/DrawEmails.kt`, `SendDrawEmails` and `SendAnnouncement`: the group id reaches the email text; their tests in `core/domain/test`.
- `app/androidApp/src/AndroidManifest.xml` and `MainActivity.kt`: a link filter and the reading of a link's address into the routes a notification tap already opens; tests next to the existing route and address tests where the mapping is pure.
- `app/shared/src/ui/navigation`: a way to use `WebAddresses.routeOf` for a link without the admin routes.
- `app/webApp/docker` (`40-write-config.sh`, `nginx.conf`), `docker-compose.yml`, `docker-compose.images.yml` and `.env.example`: the optional setting and the served file.
- `README.md`: the setting, the fingerprint and the checks.
- The production address `https://santa.example.com` is fixed in the app, like its server address; the file the app is verified by lives on that host and needs the real release key's fingerprint, which only the owner of `secrets/android-release.keystore` can read. Debug builds are signed with another key and are not verified.
