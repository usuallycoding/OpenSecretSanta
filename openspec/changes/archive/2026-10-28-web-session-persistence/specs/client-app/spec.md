# Spec Delta

## MODIFIED Requirements

### Requirement: Session storage per platform
On Android the app SHALL keep the access token in encrypted storage backed by the platform keystore, and on iOS in the Keychain, so that the person stays signed in after closing the app. On web the app SHALL keep the token in the browser tab's session storage, so that it survives a refresh of the page and is lost when the tab is closed. The token SHALL NOT be written to logs, to plain preferences, or to browser storage that outlives the tab, such as local storage or cookies.

#### Scenario: Native restart
- **WHEN** a signed-in person closes and reopens the Android or iOS app
- **THEN** the app checks the stored token with the server and, if it is still accepted, shows the first view without asking for the password

#### Scenario: Web refresh
- **WHEN** a signed-in person refreshes the web page
- **THEN** the app checks the stored token with the server and, if it is still accepted, shows the screen named in the address without asking for the password

#### Scenario: Web tab closed
- **WHEN** a signed-in person closes the web tab and opens the app again in a new tab
- **THEN** the sign-in screen is shown

#### Scenario: Web token rejected after a refresh
- **WHEN** a person refreshes the web page and the server no longer accepts the stored token
- **THEN** the sign-in screen is shown and the stored token is gone

#### Scenario: Web sign out
- **WHEN** a signed-in person signs out on the web and refreshes the page
- **THEN** the sign-in screen is shown

#### Scenario: Not in longer-lived storage
- **WHEN** a person is signed in on the web
- **THEN** the token is in the tab's session storage and in neither local storage nor a cookie

#### Scenario: Storage unavailable
- **WHEN** the platform storage cannot be read or written
- **THEN** the app behaves as if there were no stored session and the person can still sign in for the current run
