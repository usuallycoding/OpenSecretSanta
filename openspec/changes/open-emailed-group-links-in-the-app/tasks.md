# Tasks

## 1. Emails link to the group

- [x] 1.1 Add a function building the group link from the app address and the group id (one slash before `#`, null without an address), make `drawEmail`, `announcementEmail` and `appendDetailsAndLink` use it, and pass the group id from `SendDrawEmails` and `SendAnnouncement`; verify `./kotlin test` compiles and the domain tests build
- [x] 1.2 Update `DrawEmailsTest` and `SendAnnouncementTest` for the new link (`https://santa.example.org/#/groups/<id>`), and add cases for an address with a trailing slash and for no address; verify they pass

## 2. Links open in the Android app

- [x] 2.1 Add the pure function that turns a link's fragment into the routes to open (group first, then the Gifter or Receiver page and tab) and null for admin, profile, settings, empty or unknown addresses, in `ui/navigation` next to `NotificationTarget`, with unit tests for each case; verify they pass on every target
- [ ] 2.2 Add the `https` `VIEW` / `DEFAULT` / `BROWSABLE` filter with `autoVerify` for `santa.example.com` to the manifest, and make `MainActivity.routesOf` use the link's routes when the intent is such a link (scheme and host checked), clearing `intent.data` once handled; verify the app builds and, with `adb shell am start -a android.intent.action.VIEW -d "https://santa.example.com/#/groups/7"`, a signed-in app opens group 7 with the list behind it, and a signed-out app opens it after sign-in

## 3. The web host proves the app's ownership

- [x] 3.1 Make `40-write-config.sh` write `.well-known/assetlinks.json` from `PUBLIC_ANDROID_CERT_SHA256` (validated, removed when unset), serve it as JSON without a redirect in `nginx.conf`, pass the variable to the web container in both compose files and list it in `.env.example`; verify by running the container with and without the variable and with an invalid one, and checking `curl -i` of the file
- [ ] 3.2 Document the setting, reading the release fingerprint with `keytool`, and checking or forcing the verification with `adb` in the README; verify the commands run as written against the real keystore and a device

## 4. Specs and checks

- [x] 4.1 Run `openspec validate open-emailed-group-links-in-the-app --strict` and `./kotlin test`; verify validation passes and only the unrelated `ServerAddress` test fails
- [ ] 4.2 With the real fingerprint set on `https://santa.example.com` and a release APK installed, send a draw email and an announcement email and tap their links; verify `pm get-app-links` says the host is verified and each link opens its group in the app, and that on a phone without the app it opens the web app on that group

## Workflow follow-up

- Archive the change after review so the specs pick up the new requirements.
