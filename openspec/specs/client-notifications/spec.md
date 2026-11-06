# client-notifications Specification

## Purpose
How a person controls push notifications in the Android and web apps: a Settings row and bottom sheet with a switch for the device and the four kinds of notification, the permission flow, and what happens when a notification arrives or is tapped.

## Requirements

### Requirement: Notifications in Settings
On Android and the web, the Settings page, in both the participant and the admin view, SHALL have a "Notifications" row that opens a bottom sheet. On iOS the row SHALL NOT be offered.

#### Scenario: Participant and admin
- **WHEN** a participant or an administrator opens Settings on Android or the web
- **THEN** it has a "Notifications" row that opens the sheet

#### Scenario: iOS
- **WHEN** Settings is opened on iOS
- **THEN** it has no "Notifications" row

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
A notification that arrives SHALL be shown by the system with the app's name and the notification's text, whether the app is open, in the background or closed.

#### Scenario: App closed
- **WHEN** a question notification arrives while the app is closed
- **THEN** the system shows it

#### Scenario: Tap
- **WHEN** the person taps an announcement about group 7, such as a message from the organiser
- **THEN** the app opens on group 7

#### Scenario: Signed out
- **WHEN** the person taps a notification while signed out
- **THEN** the app opens on the sign-in screen, as it would for any other launch while signed out

#### Scenario: A question from the Santa
- **WHEN** the person taps a question notification about group 7
- **THEN** the app opens group 7's Receiver page on its "Respond to Secret Santa" tab

#### Scenario: An answer from the receiver
- **WHEN** the person taps an answer notification about group 7
- **THEN** the app opens group 7's Gifter page on its "Questions for" tab

#### Scenario: A change to the receiver's wishlist
- **WHEN** the person taps a wishlist notification about group 7
- **THEN** the app opens group 7's Gifter page on its wishlist tab

#### Scenario: Back goes up one level
- **WHEN** the person has opened a question notification and presses Back
- **THEN** the group's page is shown, and Back again shows the group list

#### Scenario: App already open elsewhere
- **WHEN** the app is open on another group or screen and the person taps a notification about group 7
- **THEN** the app opens the notification's screen with the group list and group 7 behind it, and nothing of the earlier screens behind them

#### Scenario: Group not drawn any more
- **WHEN** the person taps a question notification about a group that is now Archived
- **THEN** the Receiver page opens on its questions tab, which shows them read-only

#### Scenario: Unknown category
- **WHEN** a notification arrives with a category the app does not know, or none
- **THEN** tapping it opens the group's page

#### Scenario: Group not available
- **WHEN** the person taps a notification about a group they can no longer see
- **THEN** the screen says the group is no longer available and offers the way back

### Requirement: Tapping a notification opens its screen
Tapping a notification SHALL open the screen it is about with the group list and the group's page behind it: a question from the person's Santa opens the Receiver page on its questions tab, an answer from the person's receiver opens the Gifter page on its questions tab, a change to the receiver's wishlist opens the Gifter page on its wishlist tab, a letter from the person's receiver opens the Inbox page, and any other notification, or one with no known category, opens the group's page.

#### Scenario: A letter
- **WHEN** a person taps the notification of a letter from their receiver
- **THEN** the screens, from the bottom, are the group list, the group's page and the Inbox page

#### Scenario: Order of the screens
- **WHEN** a person taps the notification of a question from their Santa
- **THEN** the screens, from the bottom, are the group list, the group's page and the Receiver page on its questions tab

### Requirement: A notification about a group that is not available
When the group a tapped notification is about is not available to the person, the screen SHALL say so and offer the way back.

#### Scenario: Way back
- **WHEN** the group was deleted and the person taps its notification
- **THEN** the screen says the group is not available and offers the way back to the list

### Requirement: Five toggles
Below the device switch the sheet SHALL show one toggle each for announcements (messages from the organiser, being added to a group, names drawn), wishlist changes, questions from your Santa, answers from your receiver and letters from your receiver, each with a short description. A toggle SHALL show the server's current value, SHALL save at once when changed, and SHALL be disabled while the device switch is off.

#### Scenario: Shows the saved choices
- **WHEN** the person has turned "questions" off earlier and opens the sheet
- **THEN** "questions" is off and the other four are on

#### Scenario: The letters toggle
- **WHEN** the person opens the sheet
- **THEN** it has a toggle named "Letters from your receiver", on unless they turned it off

#### Scenario: Change is saved
- **WHEN** the person turns "wishlist changes" off
- **THEN** the server records it and the toggle stays off after the sheet is closed and opened again

#### Scenario: Save fails
- **WHEN** a toggle cannot be saved because the server cannot be reached
- **THEN** the toggle goes back to its previous position and an error is shown

#### Scenario: Device off
- **WHEN** the device switch is off
- **THEN** the five toggles are disabled
