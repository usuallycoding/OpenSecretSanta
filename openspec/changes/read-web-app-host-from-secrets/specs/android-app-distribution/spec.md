## MODIFIED Requirements

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

## ADDED Requirements

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
