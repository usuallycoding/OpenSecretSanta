## MODIFIED Requirements

### Requirement: Session storage per platform
On Android the app SHALL keep the access token in encrypted storage backed by the platform keystore, and on iOS in the Keychain, so that the person stays signed in after closing the app. On web the app SHALL keep the token in the browser's local storage when the person chose to stay signed in, so that it survives closing the tab and the browser, and otherwise in the browser tab's session storage, so that it survives a refresh of the page and is lost when the tab is closed. The token SHALL NOT be written to logs, to plain preferences, or on the web to cookies, and SHALL be in one of the two web storages at a time.

#### Scenario: Native restart
- **WHEN** a signed-in person closes and reopens the Android or iOS app
- **THEN** the app checks the stored token with the server and, if it is still accepted, shows the first view without asking for the password

#### Scenario: Web refresh
- **WHEN** a signed-in person refreshes the web page
- **THEN** the app checks the stored token with the server and, if it is still accepted, shows the screen named in the address without asking for the password

#### Scenario: Web tab closed
- **WHEN** a person who did not stay signed in closes the web tab and opens the app again in a new tab
- **THEN** the sign-in screen is shown

#### Scenario: Web token rejected after a refresh
- **WHEN** a person refreshes the web page and the server no longer accepts the stored token
- **THEN** the sign-in screen is shown and the stored token is gone

#### Scenario: Web sign out
- **WHEN** a signed-in person signs out on the web and refreshes the page
- **THEN** the sign-in screen is shown

#### Scenario: Not in longer-lived storage
- **WHEN** a person signed in on the web with "Stay signed in" unticked
- **THEN** the token is in the tab's session storage and in neither local storage nor a cookie

#### Scenario: Storage unavailable
- **WHEN** the platform storage cannot be read or written
- **THEN** the app behaves as if there were no stored session and the person can still sign in for the current run

## ADDED Requirements

### Requirement: Staying signed in on the web
The web sign-in screen SHALL show a "Stay signed in" checkbox, ticked by default; the Android and iOS sign-in screens SHALL NOT show it. Signing in with it ticked SHALL keep the session across closing and reopening the tab or the browser for as long as the server accepts the token; signing in with it unticked SHALL keep it only for the life of the tab. Signing out, a token the server rejects and a password change that returns a new token SHALL leave no stale token in either storage, and the new token SHALL stay where the old one was.

#### Scenario: Ticked by default
- **WHEN** the web sign-in screen is shown
- **THEN** "Stay signed in" is ticked

#### Scenario: Not shown on phones
- **WHEN** the sign-in screen is shown on Android or iOS
- **THEN** there is no "Stay signed in" checkbox

#### Scenario: Reopening the tab
- **WHEN** a person signed in on the web with "Stay signed in" ticked closes the tab and the browser and opens the app again
- **THEN** the first view is shown without asking for the password, as long as the server still accepts the token

#### Scenario: Unticked
- **WHEN** a person signs in with "Stay signed in" unticked, refreshes and then closes the tab
- **THEN** the refresh keeps them signed in and a new tab shows the sign-in screen

#### Scenario: Signing out forgets it
- **WHEN** a person who stayed signed in signs out and opens the app again in a new tab
- **THEN** the sign-in screen is shown and no token is stored

#### Scenario: Expired token
- **WHEN** the token in local storage is no longer accepted by the server
- **THEN** the sign-in screen is shown and the token is removed

#### Scenario: Changing the password
- **WHEN** a person who stayed signed in changes their password
- **THEN** the new token replaces the old one in the same storage and the person is still signed in after closing and reopening the tab
