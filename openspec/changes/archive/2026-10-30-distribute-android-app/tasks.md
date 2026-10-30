## 1. The app

- [x] 1.1 Make `OpenSecretSantaApplication` choose the server address by whether the build is debuggable (development constant, and `https://santa-api.plm360.org` for release), keeping the person's uncommitted edits to that file out of the commit
- [x] 1.2 Check a release build contains the production address and a debug build the development one

## 2. Signing and versions

- [x] 2.1 Generate a throw-away keystore outside the repository, try `settings.android.signing` with a `keystore.properties`, and verify with `apksigner` that the release APK is signed with it
- [x] 2.2 Check debug builds and the test suites still work on a machine without `keystore.properties`: they do, with a warning on every build, so `module.yaml` keeps signing off and the script turns it on for its own build (recorded in `design.md`)
- [x] 2.3 Check that `versionCode` and `versionName` can be set by editing `module.yaml` before the build, and that the built APK carries them
- [x] 2.4 Make git ignore the keystore and its properties file (`secrets/` already is)

## 3. The script

- [x] 3.1 Add `scripts/distribute-android.sh` with `--notes`, `--version`, `--group` and `--no-upload`: checks for the files and tools it needs, refuses uncommitted changes to `module.yaml`, sets the versions and signing and restores `module.yaml` on exit, builds, signs and verifies, then uploads with `npx firebase-tools appdistribution:distribute`
- [x] 3.2 Test it end to end with a throw-away keystore and `--no-upload`: the APK is signed and carries the versions, `git status` is the same before and after, a missing keystore stops it with a clear message, and no password appears in its output
- [x] 3.3 Run `shellcheck` (or `sh -n`) on it and check it never prints a secret

## 4. Documentation

- [x] 4.1 Add the README section: creating the keystore (`keytool`) and `secrets/android-signing.properties`, `npx firebase-tools login` (or the service-account option and its role), the group alias, running the script, and backing up the keystore

## 5. Verify

- [x] 5.1 `openspec validate distribute-android-app --strict`
- [x] 5.2 The person creates the real keystore, signs in to Firebase, runs the script, installs the APK from the Firebase invitation and checks the app talks to the real server
