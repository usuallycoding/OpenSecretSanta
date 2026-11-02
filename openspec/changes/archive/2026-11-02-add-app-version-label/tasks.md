# Tasks

## 1. The version constant and its label

- [x] 1.1 Add `app/shared/src/AppVersion.kt` with `const val APP_VERSION = "1.0.0"` and a `Strings.version(value)` that returns "Version <value>"; verify with a unit test in `app/shared/test` that `Strings.version("1.0.0")` is "Version 1.0.0"
- [x] 1.2 Show the label as the last item of `LoginScreen`'s column, small, muted and not pressable; verify by running the web app and seeing "Version 1.0.0" below the sign-in button
- [x] 1.3 Show the label after the "Sign out" row in `SettingsScreen`'s scrolling column; verify on a short window that it is reached by scrolling and is the last thing on the page, and that Settings of a participant and an administrator match
- [x] 1.4 Run `./kotlin build` and verify Android, iOS and web targets still compile

## 2. The distribute script

- [x] 2.1 In `scripts/distribute-android.sh`, replace the `git describe` default with a read of `APP_VERSION` from `app/shared/src/AppVersion.kt`; die before building if the line is missing, repeated or empty, and keep `--version` winning; verify with `--no-upload` that the APK is `build/distribution/opensecretsanta-1.0.0-<code>.apk`, that `--version 9.9.9` overrides it, and that a temporarily broken constant stops the script before the build
- [x] 2.2 Update the README's distribution section: the default of `--version`, that the constant is the line to bump, and its required format; verify the text matches the script's behavior and the `git describe` wording is gone
- [x] 2.3 Verify `git status` is the same after a run as before it, as the spec requires
