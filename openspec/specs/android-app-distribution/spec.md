# android-app-distribution Specification

## Purpose
How the Android app is built as a signed release APK by a script on the developer's Mac and sent to the Firebase App Distribution tester group, how it is signed and versioned, which server it talks to and which secrets stay on the machine.

## Requirements

### Requirement: A script builds and distributes the app
`scripts/distribute-android.sh` SHALL build a release APK of the Android app, sign it and upload it to Firebase App Distribution for the tester group Dincolo, with release notes. It SHALL accept `--notes` (default: the subject of the last commit), `--version` (default: the app's version) and `--group` (default: `dincolo`), and `--no-upload`, which stops after signing and prints the APK's path. It SHALL stop before building when a file or tool it needs is missing, naming it.

#### Scenario: Distribute
- **WHEN** the script is run with `--notes "Search for participants"`
- **THEN** a signed release APK is built and the group Dincolo receives it with those notes

#### Scenario: Without uploading
- **WHEN** the script is run with `--no-upload`
- **THEN** the signed APK is built, its path is printed and nothing is sent

#### Scenario: A copy to send by hand
- **WHEN** the script has built the APK, with or without uploading
- **THEN** the signed APK is kept at `build/distribution/opensecretsanta-<version>-<code>.apk` and its full path is the last thing the script prints

#### Scenario: Default version
- **WHEN** the script is run without `--version` and the app's version is `1.0.0`
- **THEN** the APK is built, named and versioned as `1.0.0`, with no commit or working-tree marker added

#### Scenario: Unreadable app version
- **WHEN** the script is run without `--version` and cannot read the app's version
- **THEN** it stops before building and says so

#### Scenario: Missing keystore
- **WHEN** the keystore file is not in `secrets/`
- **THEN** the script stops before building and says which file is missing

#### Scenario: Failed build
- **WHEN** the build or the signing fails
- **THEN** nothing is uploaded

### Requirement: The APK is signed with a release key
The distributed APK SHALL be signed with a dedicated release keystore whose file and passwords are read from the git-ignored `secrets/` folder, SHALL be installable on a phone, and SHALL NOT be signed with the debug key. The keystore, its properties file and its passwords SHALL NOT be committed or printed.

#### Scenario: Signed
- **WHEN** the script has built the APK
- **THEN** its signature verifies and is the release key's

#### Scenario: Keystore not committed
- **WHEN** the keystore and its properties file exist in a checkout
- **THEN** git ignores them

#### Scenario: Passwords not shown
- **WHEN** the script runs
- **THEN** no password appears in its output

### Requirement: Every upload has its own version
`versionName` SHALL be the `--version` given, or else the app's version, as the app's own screens show it. `versionCode` SHALL be the number of minutes since 1970-01-01 at build time, so that it is unique and only grows. The script SHALL leave `module.yaml` and the rest of the working tree as it found them, and SHALL refuse to run when `module.yaml` has uncommitted changes.

#### Scenario: Given version
- **WHEN** the script is run with `--version 1.4.0`
- **THEN** the APK has versionName `1.4.0` and a versionCode larger than that of any earlier run

#### Scenario: App's version
- **WHEN** the script is run without `--version` and the app's version is `1.0.0`
- **THEN** the APK has versionName `1.0.0`, the same as the sign-in screen and Settings show

#### Scenario: Tree restored
- **WHEN** the script has finished, or failed
- **THEN** `git status` shows the same changes as before it ran

#### Scenario: Uncommitted module file
- **WHEN** `app/androidApp/module.yaml` has uncommitted changes
- **THEN** the script stops before changing anything and says why

### Requirement: A release build talks to the real server
A release build SHALL always use the server address `https://santa-api.plm360.org`, whatever the working copy it was built from. A debuggable build SHALL keep using the development address.

#### Scenario: Release
- **WHEN** the distributed APK starts
- **THEN** it calls `https://santa-api.plm360.org`

#### Scenario: Debug
- **WHEN** a debug build is installed from a developer's machine
- **THEN** it calls the development address

### Requirement: Secrets stay on this machine
`google-services.json`, the release keystore, its properties file, the signing passwords, any service-account key and the file holding the web app's host SHALL be read from files on the developer's machine, in folders git ignores, and SHALL NOT be committed, printed, or sent anywhere except to Firebase as part of the upload. No secret SHALL be stored on GitHub. The README SHALL describe each file, how to create it and how to run the script.

#### Scenario: Firebase file
- **WHEN** the script builds the app
- **THEN** it uses the `google-services.json` that is already in the module and git still ignores it

#### Scenario: Nothing on GitHub
- **WHEN** the repository is inspected for workflows and secrets
- **THEN** no workflow builds or distributes the Android app and no secret for it is needed

#### Scenario: README
- **WHEN** someone sets the Mac up from the README
- **THEN** they know every file to create and can run a first distribution

#### Scenario: Web app's host not committed
- **WHEN** the repository is inspected after a release has been built
- **THEN** the web app's real host is in no file or commit, only the placeholder `santa.example.com` is

### Requirement: The web app's host is written into the release build
The script SHALL read the web app's host from `secrets/web-app-host`, one line holding a bare host name, and write it into the link filter of the app's manifest and into the app's `WEB_APP_HOST` for the build only. The checkout SHALL name the placeholder `santa.example.com` in both places, and the script SHALL put both files back as it found them when it has finished or failed.

#### Scenario: Release build
- **WHEN** `secrets/web-app-host` holds `santa.example.org` and the script builds the app
- **THEN** the built app's link filter and `WEB_APP_HOST` name `santa.example.org`

#### Scenario: Files put back
- **WHEN** the script has finished, or failed
- **THEN** the manifest and `AppLinks.kt` name the placeholder again and `git status` shows the same changes as before it ran

#### Scenario: Other builds
- **WHEN** the app is built any other way, such as a debug build from Android Studio
- **THEN** it keeps the placeholder host

### Requirement: The host file is checked before anything is built
The script SHALL stop before changing anything, and say why, when `secrets/web-app-host` is missing, holds anything but a plain host name (a scheme, a path, spaces or several words), or still holds the placeholder, and when the manifest or `AppLinks.kt` has uncommitted changes or does not name the placeholder on exactly one line.

#### Scenario: Missing file
- **WHEN** `secrets/web-app-host` does not exist
- **THEN** the script stops before building and names the file

#### Scenario: Not a host name
- **WHEN** the file holds `https://santa.example.org/`
- **THEN** the script stops and says it must hold a host name and nothing else

#### Scenario: Placeholder left in
- **WHEN** the file holds `santa.example.com`
- **THEN** the script stops and says the file still holds the placeholder

#### Scenario: Uncommitted manifest
- **WHEN** the manifest or `AppLinks.kt` has uncommitted changes
- **THEN** the script stops and names the file

### Requirement: The built app is checked for the host
After building, the script SHALL check that the APK's link filter names the host from `secrets/web-app-host` and does not name the placeholder, and SHALL NOT keep or upload an APK that fails the check.

#### Scenario: Host in the APK
- **WHEN** the script has built the APK
- **THEN** the link filter in the APK's manifest names the host from the file

#### Scenario: Placeholder in the APK
- **WHEN** the built APK's link filter still names the placeholder
- **THEN** the script fails and nothing is copied or uploaded
