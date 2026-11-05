# Proposal

## Why

The Android app opens the web app's links only for the web host named in its manifest and in `AppLinks.kt`, and that host is the owner's own address, which must not be committed or pushed. The code that does this (commit `08ce0af`) is already in place: the checkout holds a placeholder host and the release script writes the real one from a git-ignored file. The specs do not say so yet, and two of them now state things that are no longer true or complete (the app's link host is written into a spec as a placeholder, and the script's requirements know nothing of the host file or its checks).

## What Changes

- The release script reads the web app's host from `secrets/web-app-host` (one line, a bare host name), writes it into the manifest's link filter and into `WEB_APP_HOST` for the build only, and puts both files back afterwards, so the real host is never committed.
- Before building, the script stops and says why when the file is missing, does not hold a plain host name, still holds the placeholder, or when the manifest or `AppLinks.kt` has uncommitted changes or does not name the placeholder exactly once.
- After building, the script checks that the APK's link filter names the real host and not the placeholder.
- A build made any other way, such as a debug build from Android Studio, keeps the placeholder and never opens the links.
- This change records what is already built: no code changes, and its tasks are all complete.

## Capabilities

### New Capabilities

### Modified Capabilities
- `android-app-distribution`: three new requirements for the host file, its checks before the build and the check of the built APK; "Secrets stay on this machine" also covers the host file.
- `client-app-links`: "Group links open in the Android app" names the web app the release was built for and not a fixed address; "Without the app, or unverified, a link opens the web app" extends the debug-build rule to every build the release script did not make.

## Impact

- Specs only: `openspec/specs/android-app-distribution` and `openspec/specs/client-app-links` at archive time.
- Already implemented in `scripts/distribute-android.sh`, `app/androidApp/src/AndroidManifest.xml`, `app/androidApp/src/AppLinks.kt` and the README (commit `08ce0af`).
- The real host appears nowhere in the repository; the artifacts use `santa.example.org` for examples and `santa.example.com` for the placeholder in the checkout.
