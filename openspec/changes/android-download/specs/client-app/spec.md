## ADDED Requirements

### Requirement: The web sign-in screen offers the Android app
On the web, the sign-in screen SHALL show a "Download Android app" button below the sign-in action and above the app's version, while `GET /health` says `androidDownload` is true. Choosing it SHALL open `/download_latest` of the server the web app uses, which downloads the Android app and leaves the screen where it is. It SHALL NOT be shown on Android or iOS, nor when the answer says false or cannot be had, and nothing SHALL be said about it then.

#### Scenario: Offered
- **WHEN** the sign-in screen is shown on the web and `GET /health` says `androidDownload` is true
- **THEN** it shows "Download Android app" below the sign-in action and above "Version"

#### Scenario: Chosen
- **WHEN** the person chooses the button
- **THEN** the browser downloads the app and the sign-in screen stays as it was, with what was typed in it

#### Scenario: Nothing to download
- **WHEN** `GET /health` says `androidDownload` is false, or the server cannot be reached
- **THEN** the sign-in screen shows no such button and says nothing about it

#### Scenario: Not in the apps
- **WHEN** the sign-in screen is shown on Android or iOS
- **THEN** there is no "Download Android app" button
