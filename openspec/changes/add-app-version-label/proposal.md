# Proposal

## Why

Nobody looking at the app, the web app or a tester's phone can tell which version they are running, which makes "does this have the fix?" hard to answer. A visible version that is edited by hand in one place, and that the Android build reuses, closes that gap.

## What Changes

- A single hand-edited constant holds the app's version, shared by Android, iOS and the web app.
- The sign-in screen shows "Version <number>" at its bottom, and the Settings page shows it after "Sign out".
- `scripts/distribute-android.sh` uses that constant as the default for `--version`, instead of `git describe`, so an installed APK's `versionName` and its filename match what the screens show. An explicit `--version` still wins, and the script stops when it cannot read the constant.
- The README's description of the script's version default is updated.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-app`: the sign-in screen gains a version label at its bottom.
- `client-settings`: the Settings page gains a version label after "Sign out".
- `android-app-distribution`: the default `--version` and `versionName` come from the app's version constant, not `git describe`.

## Impact

- `app/shared`: a new constant file, `LoginScreen.kt`, `SettingsScreen.kt` and the `Strings` object.
- `scripts/distribute-android.sh` and the README's distribution section.
- No server, API or dependency change. Starting value is `1.0.0`.
