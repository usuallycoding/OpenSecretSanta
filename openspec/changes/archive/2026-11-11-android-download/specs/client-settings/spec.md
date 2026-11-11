## MODIFIED Requirements

### Requirement: The Settings page
The Settings page SHALL be titled "Settings" and SHALL list, in this order, "Appearance", "Notifications", "Change password" and, set apart below them, "Sign out". On the web, "Download Android app" SHALL come after "Change password" and before the separation, while an Android app is available to download. The Appearance row SHALL show the current choice ("Follow device", "Light" or "Dark") under its name. The Notifications row SHALL NOT be offered on iOS. Every row SHALL be at least 48 density-independent pixels high and shown as something to press.

#### Scenario: Rows in order
- **WHEN** the person opens Settings on Android or the web
- **THEN** the rows are Appearance, Notifications and Change password, and below them, set apart, Sign out

#### Scenario: Appearance shows its choice
- **WHEN** the appearance is set to "Dark"
- **THEN** the Appearance row says "Dark" under its name

#### Scenario: iOS
- **WHEN** the person opens Settings on iOS
- **THEN** there is no Notifications row

#### Scenario: Web with an app to download
- **WHEN** the person opens Settings on the web and the server says an Android app is available
- **THEN** "Download Android app" is listed after "Change password" and before the separation above "Sign out"

## ADDED Requirements

### Requirement: Downloading the Android app from the web
On the web, choosing "Download Android app" SHALL open `/download_latest` of the server the web app uses, which downloads the Android app and leaves the page where it is. The row SHALL be offered only while `GET /health` says `androidDownload` is true, and never on Android or iOS. When the answer cannot be had, or says false, the row SHALL be left out and nothing SHALL be said about it.

#### Scenario: Offered
- **WHEN** `GET /health` says `androidDownload` is true and the person opens Settings on the web
- **THEN** the "Download Android app" row is shown

#### Scenario: Chosen
- **WHEN** the person chooses the row
- **THEN** the browser downloads the app from `/download_latest` and Settings stays open

#### Scenario: Nothing to download
- **WHEN** `GET /health` says `androidDownload` is false, or cannot be reached
- **THEN** there is no "Download Android app" row and no message about it

#### Scenario: Not in the apps
- **WHEN** the person opens Settings on Android or iOS
- **THEN** there is no "Download Android app" row, whatever the server says
