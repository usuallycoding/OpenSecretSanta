# client-notifications Specification

## Purpose
How a person controls push notifications in the Android and web apps: a Settings row and bottom sheet with a switch for the device and the two kinds of notification (announcements and the daily digest), the permission flow, and what happens when a notification arrives or is tapped.

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
The sheet SHALL start with a switch for the current device. Turning it on SHALL ask for the system's notification permission (Android 13 and later, the browser) when it has not been granted, and when granted SHALL register the device with the server. Turning it off SHALL remove the device's registration. The switch SHALL show on only while the permission is granted and the device is registered. On Android, when the permission is refused and the system will not ask again, the sheet SHALL offer a button that opens the app's notification settings.

#### Scenario: Turn on
- **WHEN** the person turns the switch on and grants the permission
- **THEN** the device is registered with the server and the switch stays on

#### Scenario: Permission refused
- **WHEN** the person turns the switch on and refuses the permission
- **THEN** the switch goes back off and the sheet says that notifications must be allowed in the device or browser settings

#### Scenario: Refused, and the system can still ask
- **WHEN** an Android 13 or later device is asked for the first time and the person refuses
- **THEN** the switch goes back off and the sheet says so, with no settings button, because turning the switch on again asks again

#### Scenario: Refused, and the system will not ask again
- **WHEN** the person turns the switch on and Android refuses the permission without asking, because it was refused before until the system stopped asking
- **THEN** the switch goes back off and the sheet says that notifications are blocked for the app, with an "Open settings" button

#### Scenario: Android 12 or older
- **WHEN** the person turns the switch on, on a device that cannot ask for the permission, while the app's notifications are off in the system settings
- **THEN** the switch goes back off and the sheet says that notifications are blocked, with an "Open settings" button

#### Scenario: Open settings
- **WHEN** the person chooses "Open settings"
- **THEN** the system's notification settings of the app open, where notifications can be allowed

#### Scenario: Back from the settings
- **WHEN** the person returns to the app after allowing notifications there
- **THEN** the message and the button are gone, and the switch stays off until the person turns it on

#### Scenario: Back from the settings, still blocked
- **WHEN** the person returns to the app without having allowed notifications
- **THEN** the message and the button are still shown

#### Scenario: The web
- **WHEN** the person turns the switch on in a browser that has blocked notifications for the site
- **THEN** the switch goes back off and the sheet says that notifications must be allowed in the browser settings, with no settings button

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

### Requirement: A notification about a group that is not available
When the group a tapped notification is about is not available to the person, the screen SHALL say so and offer the way back.

#### Scenario: Way back
- **WHEN** the group was deleted and the person taps its notification
- **THEN** the screen says the group is not available and offers the way back to the list

### Requirement: Showing and opening a notification
A notification that arrives SHALL be shown by the system with the app's name and the notification's text, whether the app is open, in the background or closed. Tapping it SHALL open the app on the screen its kind names, as the requirement for tapping describes.

#### Scenario: App closed
- **WHEN** a daily notification arrives while the app is closed
- **THEN** the system shows it

#### Scenario: Signed out
- **WHEN** the person taps a notification while signed out
- **THEN** the app opens on the sign-in screen, as it would for any other launch while signed out

#### Scenario: Back goes up one level
- **WHEN** the person has opened an announcement notification about group 7 and presses Back
- **THEN** the group's page is shown, and Back again shows the group list

#### Scenario: App already open elsewhere
- **WHEN** the app is open on another group or screen and the person taps a notification about group 7
- **THEN** the app opens the notification's screen with the group list and group 7 behind it, and nothing of the earlier screens behind them

#### Scenario: Unknown category
- **WHEN** a notification arrives with a category the app does not know, or none, and it names a group
- **THEN** tapping it opens the group's page

#### Scenario: Group not available
- **WHEN** the person taps a notification about a group they can no longer see
- **THEN** the screen says the group is no longer available and offers the way back

### Requirement: Tapping a notification opens its screen or the group list
Tapping an announcement the owner sent SHALL open the group's Announcements page with the group list and the group's page behind it, and on a wide window the group's page with the Announcements pane open. Tapping any other notification about a group, or one with no known category, SHALL open the group's page. Tapping the daily notification SHALL open the group list, with nothing of the screens that were open before.

#### Scenario: The daily notification
- **WHEN** a person taps the daily notification, whatever the app was showing
- **THEN** the group list is shown and no group page is behind or over it

#### Scenario: An announcement from the owner
- **WHEN** a person taps the notification of an announcement the owner sent
- **THEN** the screens, from the bottom, are the group list, the group's page and the Announcements page, and on a wide window the group's page with the Announcements pane open

#### Scenario: Added to a group, or the names drawn
- **WHEN** a person taps the notification that they were added to a group, or that the names were drawn
- **THEN** the group's page opens, not the Announcements page

### Requirement: Two toggles
Below the device switch the sheet SHALL show one toggle for announcements (messages from the organiser, being added to a group, names drawn) and one for "Daily digest", described as one notification at 20:00 each day when something changed in your groups. A toggle SHALL show the server's current value, SHALL save at once when changed, and SHALL be disabled while the device switch is off.

#### Scenario: Shows the saved choices
- **WHEN** the person has turned "Daily digest" off earlier and opens the sheet
- **THEN** it is off and "Announcements" is on

#### Scenario: The description
- **WHEN** the person opens the sheet
- **THEN** "Daily digest" says it is one notification each day at 20:00 when something changed, without saying what

#### Scenario: Change is saved
- **WHEN** the person turns "Daily digest" off
- **THEN** the server records it and the toggle stays off after the sheet is closed and opened again

#### Scenario: Save fails
- **WHEN** a toggle cannot be saved because the server cannot be reached
- **THEN** the toggle goes back to its previous position and an error is shown

#### Scenario: Device off
- **WHEN** the device switch is off
- **THEN** the two toggles are disabled

### Requirement: Android lists the two kinds
On Android the two kinds of notification SHALL be the two notification channels listed in the system settings of the app, named "Announcements" and "Daily digest", and no channel for the kinds that no longer exist.

#### Scenario: Android channels
- **WHEN** the system's notification settings of the app are opened on Android
- **THEN** they list "Announcements" and "Daily digest"

#### Scenario: Older channels
- **WHEN** the app starts after an update on a device that had the four earlier channels
- **THEN** the channels for wishlist changes, questions and answers are gone from the system settings
