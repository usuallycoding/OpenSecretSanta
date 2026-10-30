## Context

The Android module is built with the Kotlin toolchain (`./kotlin build -m androidApp -v release` gives `gradle-project-release-unsigned.apk`; `package` gives an unsigned `.aab`). Its settings already include `android.signing` (`enabled`, default false, and `propertiesFile`, default `keystore.properties`), `versionCode` (default 1) and `versionName` (default unspecified), as `./kotlin show settings -m androidApp` lists. `app/androidApp/google-services.json` is git-ignored and `secrets/` is ignored too. The server address is a literal in `OpenSecretSantaApplication.kt`; the committed value is the emulator's `http://10.0.2.2:8080`, and the working copy has the person's own uncommitted edits. The Mac has Node, the Android SDK with build-tools (including `apksigner` and `zipalign`) and `keytool`.

## Goals / Non-Goals

**Goals:** a signed APK in front of the Dincolo testers from one command on the Mac, repeatable, with no secret outside the machine.

**Non-Goals:** GitHub Actions or any CI, Google Play, an AAB, iOS, release notes written from commit messages, key rotation tooling.

## Decisions

- **A local script, no CI.** The person does not want signing keys, `google-services.json` or credentials on GitHub, so nothing about distribution runs there. `scripts/distribute-android.sh` is plain `sh`/`bash` using tools the Mac already has.
- **APK, not AAB.** App Distribution installs an APK directly; an AAB needs the app linked to Google Play, which is not wanted.
- **Where the secrets live.** `secrets/android-release.keystore` and `secrets/android-signing.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), next to the existing git-ignored push key. The script reads them from there and checks they exist before it builds anything.
- **Signing through the toolchain (tried, and it works).** With `android.signing.enabled: true` and a `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) next to `module.yaml`, `./kotlin build -m androidApp -v release` writes a signed `gradle-project-release.apk` next to the unsigned one; `apksigner verify` accepts it and shows the keystore's certificate. With signing enabled and no `keystore.properties`, debug builds, release builds and the tests still succeed but print a warning on every build. So `module.yaml` keeps signing off, and the script turns it on, together with the version numbers, only for its own build (see below), which leaves nobody else a warning. The `apksigner` route is therefore not needed. The script deletes the APKs of earlier builds first, because the build does not, and so a stale signed APK can never be mistaken for the new one.
- **Version numbers and signing by editing before the build.** `module.yaml` has no environment access, so the script sets `versionCode` and `versionName` and `signing.enabled` in it just for the build and restores the file from a copy on exit (a `trap`, so a failure restores it too). It refuses to start if `module.yaml` already has uncommitted changes, so it can never overwrite the person's edits. `versionCode` is the minutes since the epoch, which needs no counter and only grows.
- **Server address by build type.** The application reads whether it is debuggable (`ApplicationInfo.FLAG_DEBUGGABLE`): debuggable builds use a development constant, release builds `https://santa-api.plm360.org`. This replaces editing one literal by hand, and makes the release APK correct whatever is committed. The development constant is what the person has today in their working copy for local testing.
- **Upload with the Firebase CLI.** `npx firebase-tools appdistribution:distribute <apk> --app <app id> --groups <alias> --release-notes <text>`. The app id is read from `google-services.json` (`mobilesdk_app_id`) with `python3`. Credentials are the person's own sign-in (`npx firebase-tools login`, once), so no key file has to be kept; if `secrets/firebase-service-account.json` exists the script uses it instead (`GOOGLE_APPLICATION_CREDENTIALS`), for a dedicated account with the "Firebase App Distribution Admin" role. The group is Dincolo; Firebase identifies a group by its alias, normally the lower-case name `dincolo`, which `--group` overrides.
- **Nothing printed.** The script never echoes passwords or file contents; it passes them to the tools through files and the environment of the single command that needs them.

## Risks / Trade-offs

- The upload cannot be tried from here: signing and versions can be checked with a throw-away key and `--no-upload`, but the Firebase upload and the sign-in can only be seen working by the person, and may need a small fix.
- Losing the keystore means testers must uninstall the app to install one signed with another key; the README says to back it up.
- A build on the Mac depends on that Mac's state (SDK, Node); there is no clean-room build. Acceptable for a tester build.
- Group aliases are easy to get wrong; the README says where to read it in the Firebase console.

## Open Questions

None.
