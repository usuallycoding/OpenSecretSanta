# Spec Delta

## ADDED Requirements

### Requirement: Pulling a screen refreshes it
On Android and iOS a screen that shows data loaded from the server SHALL reload when the person pulls its content down, SHALL show the standard pull indicator until the answer arrives, and SHALL NOT offer a Refresh button. A screen with little or nothing to show SHALL still respond to the pull. A reload SHALL keep what the screen shows while it runs and when it fails.

#### Scenario: Pull a list
- **WHEN** a person pulls the Groups list down on Android or iOS
- **THEN** the pull indicator is shown, the list reloads and the indicator goes away when the answer arrives

#### Scenario: Pull a short screen
- **WHEN** a person pulls down a screen that shows only "You have not added anything yet"
- **THEN** it reloads in the same way

#### Scenario: No button
- **WHEN** a screen that loads data is shown on Android or iOS
- **THEN** it has no Refresh button

#### Scenario: Pull fails
- **WHEN** a pull cannot reach the server
- **THEN** the pull indicator goes away, what was shown stays, and the message with "Try again" is shown

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible screen reloads quietly, with the thin progress bar and without the pull indicator

### Requirement: The web has no refresh control
On the web a screen SHALL offer no refresh control, because the browser's own refresh reloads the page and keeps the person signed in on the same screen.

#### Scenario: No button on the web
- **WHEN** a screen that loads data is shown in the web app
- **THEN** it has no Refresh button and no pull gesture

#### Scenario: Browser refresh
- **WHEN** a signed-in person refreshes the web page on a group's screen
- **THEN** the same screen is shown with current data and the person is still signed in
