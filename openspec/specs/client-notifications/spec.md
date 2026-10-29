# client-notifications Specification

## Purpose
How a person controls push notifications in the Android and web apps: a Settings row and bottom sheet with a switch for the device and the four kinds of notification, the permission flow, and what happens when a notification arrives or is tapped.

## Requirements

### Requirement: Notifications in the top-bar menu
On Android and the web, the top-bar menu, in both the participant and the admin view, SHALL have a "Notifications" item that opens a bottom sheet. On iOS the item SHALL NOT be offered.

#### Scenario: Participant and admin
- **WHEN** a participant or an administrator opens the menu on Android or the web
- **THEN** it has a "Notifications" item that opens the sheet

#### Scenario: iOS
- **WHEN** the menu is opened on iOS
- **THEN** it has no "Notifications" item

### Requirement: A switch for this device
The sheet SHALL start with a switch for the current device. Turning it on SHALL ask for the system's notification permission (Android 13 and later, the browser) when it has not been granted, and when granted SHALL register the device with the server. Turning it off SHALL remove the device's registration. The switch SHALL show on only while the permission is granted and the device is registered.

#### Scenario: Turn on
- **WHEN** the person turns the switch on and grants the permission
- **THEN** the device is registered with the server and the switch stays on

#### Scenario: Permission refused
- **WHEN** the person turns the switch on and refuses the permission
- **THEN** the switch goes back off and the sheet says that notifications must be allowed in the device or browser settings

#### Scenario: Turn off
- **WHEN** the person turns the switch off
- **THEN** the device is removed from the server and receives nothing more

#### Scenario: Permission taken away
- **WHEN** the person withdraws the permission in the system settings and then opens the sheet
- **THEN** the switch is off

#### Scenario: Only on request
- **WHEN** the app starts or the person signs in
- **THEN** no permission is asked for until the person turns the switch on

### Requirement: Four toggles
Below the device switch the sheet SHALL show one toggle each for announcements (added to a group, names drawn), wishlist changes, questions from your Santa and answers from your receiver, each with a short description. A toggle SHALL show the server's current value, SHALL save at once when changed, and SHALL be disabled while the device switch is off.

#### Scenario: Shows the saved choices
- **WHEN** the person has turned "questions" off earlier and opens the sheet
- **THEN** "questions" is off and the other three are on

#### Scenario: Change is saved
- **WHEN** the person turns "wishlist changes" off
- **THEN** the server records it and the toggle stays off after the sheet is closed and opened again

#### Scenario: Save fails
- **WHEN** a toggle cannot be saved because the server cannot be reached
- **THEN** the toggle goes back to its previous position and an error is shown

#### Scenario: Device off
- **WHEN** the device switch is off
- **THEN** the four toggles are disabled

### Requirement: When notifications cannot be offered
When the server reports that push is not available, or the browser does not support notifications, the sheet SHALL say so and SHALL NOT let the device switch be turned on.

#### Scenario: Server without push
- **WHEN** the server has no push key configured and the person opens the sheet
- **THEN** the sheet says notifications are not available and the device switch is disabled

#### Scenario: Unsupported browser
- **WHEN** the browser has no notification support
- **THEN** the sheet says so and the device switch is disabled

### Requirement: Signing out turns the device off
Signing out SHALL remove the device's registration from the server and forget that notifications were on, so that the next person to sign in on that device receives nothing until they turn the switch on themselves.

#### Scenario: Shared device
- **WHEN** `alice` signs out and `bob` signs in on the same phone
- **THEN** the phone receives nothing for `alice` and the sheet shows the device switch off for `bob`

#### Scenario: Offline sign-out
- **WHEN** the person signs out while the server cannot be reached
- **THEN** the sign-out still completes

### Requirement: A changed token is registered again
When the push service gives the device a new token while the device switch is on, the app SHALL register the new token and the old one SHALL stop receiving notifications.

#### Scenario: Token refresh
- **WHEN** the device's token changes
- **THEN** the server holds the new token for the signed-in person

### Requirement: Showing and opening notifications
A notification that arrives SHALL be shown by the system with the app's name and the notification's text, whether the app is open, in the background or closed. Tapping it SHALL open the app on the group it is about, or on the group list when that group is not available to the person.

#### Scenario: App closed
- **WHEN** a question notification arrives while the app is closed
- **THEN** the system shows it

#### Scenario: Tap
- **WHEN** the person taps a notification about group 7
- **THEN** the app opens on group 7

#### Scenario: Signed out
- **WHEN** the person taps a notification while signed out
- **THEN** the app opens on the sign-in screen, as it would for any other launch while signed out
