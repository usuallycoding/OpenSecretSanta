## Why

Testers need the Android app without a cable or a Play Store listing. Firebase App Distribution can hand a signed APK to a tester group, and a script on the developer's own Mac can build, sign and upload it in one command, with every secret staying on that machine. Nothing builds a signed release APK today: the toolchain produces an unsigned one, no version numbers are set, and the committed app points at the emulator's address (`http://10.0.2.2:8080`), which would make a release APK useless on a phone.

## What Changes

- A script, `scripts/distribute-android.sh`, builds a signed release APK of the Android app and uploads it to Firebase App Distribution for the tester group **Dincolo**, with release notes. It runs on the developer's Mac only: there is no GitHub Actions workflow and nothing is stored on GitHub.
- The APK is signed with a dedicated release keystore kept in the git-ignored `secrets/` folder, through the toolchain's own signing settings (`settings.android.signing`) if they work without disturbing debug builds, and otherwise by signing the APK in the script with the SDK's `apksigner`.
- `versionName` comes from the command line (or `git describe`) and `versionCode` from the clock, so every upload has its own, ever-growing version and testers can tell them apart. The script puts them in `module.yaml` for the build and puts the file back afterwards.
- The app picks its server address by build type: a debuggable build keeps the development address, a release build always uses `https://santa-api.plm360.org`. Today a single constant is edited by hand, and the committed value would be baked into the APK.
- `google-services.json`, the keystore and its properties file and any service-account key live on the Mac, git-ignored, and are never committed, printed or uploaded anywhere but to Firebase as part of the APK.
- The README describes the one-time setup (creating the keystore, signing in to Firebase, the group alias) and how to run the script.

## Capabilities

### New Capabilities

- `android-app-distribution`: how the signed Android app is built and sent to testers by a local script, what it is signed with, how it is versioned, which server it talks to and what stays on the developer's machine.

### Modified Capabilities

None.

## Impact

- New: `scripts/distribute-android.sh`. Changed: `app/androidApp/module.yaml` (signing settings, only if the toolchain's way works), `OpenSecretSantaApplication.kt` (the address by build type), `.gitignore` (the keystore and its properties file), `README.md`.
- No server change. Needs, once, on the Mac: Node (for `npx firebase-tools`), the Android SDK build-tools (already there), a release keystore, and a Firebase sign-in (`firebase login`) or a service-account key.
- Not in scope: GitHub Actions, Google Play, an iOS build, an AAB, release notes written from commits.
- Note for the person: `OpenSecretSantaApplication.kt` and `network_security_config.xml` have uncommitted edits of their own in the working copy; they are theirs to commit, and this change builds on the committed state.
