## MODIFIED Requirements

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
