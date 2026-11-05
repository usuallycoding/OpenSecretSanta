# Design

## Context

Both emails get their link from `appendDetailsAndLink(group, appUrl)` in `core/domain/src/email/DrawEmails.kt`, which prints `Open the app: <appUrl>`; `drawEmail` and `announcementEmail` receive only `GroupDetails`, which has no id, while `SendDrawEmails` has the `Group` and `SendAnnouncement` has `groupId`. The web app routes on the part after `#`, and `WebAddresses.routeOf(fragment, isAdmin)` already turns that into a route. The Android `MainActivity` has only the launcher filter; it already opens a group from a tapped notification through `requestedRoutes`, waiting for the signed-in navigation, and handles `onNewIntent` and clears the intent's extras after use. The APK is signed with the release key in `secrets/android-release.keystore`, distributed through Firebase App Distribution, and talks to one fixed server. The web container writes `config.json` from environment variables when it starts (`40-write-config.sh`). See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- A link in each email that names the group, built in one place.
- Verified Android App Links for `https://santa.example.com`, opening group screens in the app by reusing the notification path.
- The one file Android needs on the web host, produced from a setting.

**Non-Goals:**
- iOS universal links (iOS keeps opening the web app).
- A custom URL scheme, or changing the web app's addresses to real paths.
- Making the app's host configurable, or supporting several signing keys.
- Opening admin, profile or settings screens from a link.

## Decisions

**One function builds the group link.** A small function in `DrawEmails.kt` returns `appUrl.trimEnd('/') + "/#/groups/" + id`, or null with no app address; both emails call it, and `appendDetailsAndLink` prints `Open the app: <link>` only when it is non-null. `drawEmail` and `announcementEmail` take the group id. The wording of the line stays, so only its address changes. Alternative rejected: trimming the slash when `APP_URL` is parsed; the domain functions are also called with other addresses in tests and the link is the only place it matters.

**The link goes to the group page for both emails.** It is the page that shows what the email is about and where the person's Gifter and Receiver pages are one tap away. Alternative rejected: sending the draw email's reader straight to their Gifter page, which would show who they give to before they have chosen to look.

**A verified `https` App Link with the host in the manifest.** `MainActivity` gets a `VIEW` / `DEFAULT` / `BROWSABLE` filter with `scheme=https`, `host=santa.example.com` and `autoVerify`. Android matches on the host and path only, and our links have the path `/`, so the filter takes every address on that host; that is acceptable because only this web app lives there, and a link to the front page then simply opens the app. The host is a literal in the manifest and the same constant in Kotlin, which checks the scheme and host of an incoming link again before using it. Alternatives rejected: a custom scheme such as `opensecretsanta://`, which email clients do not turn into links; real paths instead of `#` routes, a rework of the web routing and nginx for no gain.

**The fragment is read by the app and mapped by the existing code.** For a link, `Uri.fragment` is passed to `WebAddresses.routeOf(fragment, isAdmin = false)`; a small pure function next to `NotificationTarget` keeps only the group, Gifter and Receiver routes and returns the routes to open with the group's page first (the same list shape as a notification), or null for anything else. `MainActivity.routesOf` tries the notification extras and then the link, so the rest of the flow is untouched: `requestedRoutes` waits for sign-in and navigation, `openNotification` resets the stack, `onNewIntent` handles an open app. The activity clears `intent.data` after use, as it removes the notification extras, so a recreated activity does not open the group again. Admin addresses are excluded by the function, not by `isAdmin`, which is unknown before sign-in. The pure function is unit-tested like `WebAddresses`.

**The web container writes `assetlinks.json` from `PUBLIC_ANDROID_CERT_SHA256`.** `40-write-config.sh` validates the value (32 hex pairs with colons) and writes `/usr/share/nginx/html/.well-known/assetlinks.json` with the `delegate_permission/common.handle_all_urls` relation, package `com.usuallycoding.opensecretsanta` and the fingerprint, and removes a stale file when the value is unset. `nginx.conf` serves it as `application/json` with `no-cache` and no redirect, which verification requires. Compose and the published-images compose pass the variable to the web container only, and `.env.example` lists it with a placeholder. Alternative rejected: committing the file, which would put the owner's fingerprint in git and tie the image to one key.

**Fingerprint documentation lives in the README.** `keytool -list -v -keystore secrets/android-release.keystore -alias release` prints the SHA-256, and `adb shell pm get-app-links com.usuallycoding.opensecretsanta` and `adb shell pm verify-app-links --re-verify` show or retry the verification; `pm set-app-links` forces it for a debug build.

## Risks / Trade-offs

- [A wrong or missing fingerprint fails silently: the link opens in the browser] → The README gives the `adb` checks, and the container refuses a value that is not a fingerprint.
- [The fingerprint is the release key's; a debug build or a later Play-signed app has another] → One value now, as the app is distributed by Firebase App Distribution; a second key would need the setting to take a list, noted as a non-goal.
- [An email service that rewrites links for click tracking sends the tap through its own host, which the app does not own] → Keep link tracking off for the sending domain; check once with a real email.
- [Android 12 and later never open an unverified web address in the app, and verification needs `https` and a reachable file at install time] → A newly installed app may need `pm verify-app-links --re-verify` until the file exists; the spec says an unverified link opens the web app, so nothing is lost.
- [A recreated or relaunched activity replaying the link] → `intent.data` is cleared once its routes are handed over.
- [An administrator who taps a group link starts in the admin view] → The same as a tapped notification, which already opens the group's page from either view; not changed here.

## Migration Plan

Deploy the server and web container first, set `PUBLIC_ANDROID_CERT_SHA256` and restart the web container, check `https://santa.example.com/.well-known/assetlinks.json`, then distribute the new APK; verification happens when the new version is installed. Emails sent before the server update keep their old link, which still opens the web app. Rollback: an older server sends the old link; an older app simply does not claim the host.

## Open Questions

- Whether the sending domain has link tracking on (it changes nothing in the plan, only whether the first real email opens the app).
