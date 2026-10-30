# android-app-distribution Specification

## Purpose
How the Android app is built as a signed release APK by a script on the developer's Mac and sent to the Firebase App Distribution tester group, how it is signed and versioned, which server it talks to and which secrets stay on the machine.

## Requirements

### Requirement: A script builds and distributes the app
`scripts/distribute-android.sh` SHALL build a release APK of the Android app, sign it and upload it to Firebase App Distribution for the tester group Dincolo, with release notes. It SHALL accept `--notes` (default: the subject of the last commit), `--version` (default: `git describe`) and `--group` (default: `dincolo`), and `--no-upload`, which stops after signing and prints the APK's path. It SHALL stop before building when a file or tool it needs is missing, naming it.

#### Scenario: Distribute
- **WHEN** the script is run with `--notes "Search for participants"`
- **THEN** a signed release APK is built and the group Dincolo receives it with those notes

#### Scenario: Without uploading
- **WHEN** the script is run with `--no-upload`
- **THEN** the signed APK is built, its path is printed and nothing is sent

#### Scenario: A copy to send by hand
- **WHEN** the script has built the APK, with or without uploading
- **THEN** the signed APK is kept at `build/distribution/opensecretsanta-<version>-<code>.apk` and its full path is the last thing the script prints

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
`versionName` SHALL be the `--version` given, or else the output of `git describe`. `versionCode` SHALL be the number of minutes since 1970-01-01 at build time, so that it is unique and only grows. The script SHALL leave `module.yaml` and the rest of the working tree as it found them, and SHALL refuse to run when `module.yaml` has uncommitted changes.

#### Scenario: Given version
- **WHEN** the script is run with `--version 1.4.0`
- **THEN** the APK has versionName `1.4.0` and a versionCode larger than that of any earlier run

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
`google-services.json`, the release keystore, its properties file, the signing passwords and any service-account key SHALL be read from files on the developer's machine, in folders git ignores, and SHALL NOT be committed, printed, or sent anywhere except to Firebase as part of the upload. No secret SHALL be stored on GitHub. The README SHALL describe each file, how to create it and how to run the script.

#### Scenario: Firebase file
- **WHEN** the script builds the app
- **THEN** it uses the `google-services.json` that is already in the module and git still ignores it

#### Scenario: Nothing on GitHub
- **WHEN** the repository is inspected for workflows and secrets
- **THEN** no workflow builds or distributes the Android app and no secret for it is needed

#### Scenario: README
- **WHEN** someone sets the Mac up from the README
- **THEN** they know every file to create and can run a first distribution
