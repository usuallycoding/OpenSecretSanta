# client-update-required Specification

## Purpose

Stops the Open Secret Santa apps from being used against a server whose API is newer than they understand, and tells the person to update, so that an out-of-date app never misbehaves silently.

## Requirements

### Requirement: The app stops when the server's API is newer
When the server answers `GET /health` with an `apiVersion` higher than the version the app was built with, the app SHALL show a full-screen update screen in place of everything else, and nothing else SHALL be usable: not the shell, not the sign-in screen and not the password change. The screen SHALL say that this version of the app is too old for the server and that the app must be updated.

#### Scenario: Newer server
- **WHEN** the app was built with API version 1 and the server answers with `apiVersion` 2
- **THEN** the update screen is shown and no other screen can be reached

#### Scenario: Signed in
- **WHEN** a person is signed in and the app finds the server's API newer
- **THEN** the shell is replaced by the update screen, and the person is still signed in once the app is updated

#### Scenario: Same or older server
- **WHEN** the server answers with an `apiVersion` equal to or lower than the app's
- **THEN** the app works as it always did and shows no update screen

### Requirement: Only a clear answer stops the app
The app SHALL stop only on a successful answer that carries an `apiVersion` higher than its own. A server that cannot be reached, an error answer, and an answer with no `apiVersion`, as an older server gives, SHALL NOT stop it, and nothing SHALL be said about the check. A later check that fails SHALL leave things as they are, and a later answer that is no longer higher SHALL end the stop.

#### Scenario: Server unreachable
- **WHEN** the app starts with no connection to the server
- **THEN** it carries on as it does without the check, and shows no update screen

#### Scenario: Older server
- **WHEN** the server's answer has no `apiVersion`
- **THEN** the app is not stopped

#### Scenario: Error answer
- **WHEN** the server answers `GET /health` with 503
- **THEN** the app is not stopped

#### Scenario: A check fails while stopped
- **WHEN** the update screen is shown and the next check cannot reach the server
- **THEN** the update screen stays

#### Scenario: A check fails while working
- **WHEN** the app is in use and the next check cannot reach the server
- **THEN** the app stays in use and shows no update screen

#### Scenario: The server is rolled back
- **WHEN** the update screen is shown and the next answer carries an `apiVersion` no higher than the app's
- **THEN** the update screen goes away and the app is usable again

### Requirement: The server is checked at launch and on return
The app SHALL check the server at launch, at the same time as it restores the session, and again each time it returns to the foreground. Until the launch check and the restore are both done, the app SHALL show its loading indicator and no other screen, so that neither the sign-in screen nor the shell shows before a stop that is about to be made.

#### Scenario: At launch
- **WHEN** the app starts against a server whose API is newer
- **THEN** the loading indicator is followed by the update screen, never by the sign-in screen or the shell

#### Scenario: Slow or absent server
- **WHEN** the server cannot be reached at launch
- **THEN** the wait is the one the session restore already has, not that and then another

#### Scenario: Returning to the app
- **WHEN** the server's API became newer while the app was in the background and the person returns to it
- **THEN** the update screen replaces what was shown

### Requirement: The update screen offers what the platform can do
On the web, the update screen SHALL offer a "Reload" button, which reloads the page to fetch the new version, and say so. On Android, it SHALL offer a "Download update" button that opens the server's `/download_latest` address when the answer that stopped the app says an Android app can be downloaded, and otherwise only the text. On iOS, it SHALL show only the text.

#### Scenario: Web
- **WHEN** the update screen is shown on the web
- **THEN** it has a "Reload" button, and choosing it reloads the page

#### Scenario: Android with an app to download
- **WHEN** the update screen is shown on Android and `/health` said `androidDownload` is true
- **THEN** it has a "Download update" button, and choosing it opens `/download_latest` in the browser

#### Scenario: Android with nothing to download
- **WHEN** the update screen is shown on Android and `/health` said `androidDownload` is false
- **THEN** it shows the text and no button

#### Scenario: iOS
- **WHEN** the update screen is shown on iOS
- **THEN** it shows the text and no button
