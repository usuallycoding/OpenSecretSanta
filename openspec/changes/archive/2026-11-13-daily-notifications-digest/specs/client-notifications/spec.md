# Spec Delta

## REMOVED Requirements

### Requirement: Showing and opening notifications
**Reason**: Its taps for a question, an answer and a wishlist change no longer exist, since those notifications are replaced by the daily one.
**Migration**: See "Showing and opening a notification".

### Requirement: Tapping a notification opens its screen
**Reason**: Question, answer, wishlist and letter notifications no longer exist; the daily one opens the group list.
**Migration**: See "Tapping a notification opens its screen or the group list".

### Requirement: Five toggles
**Reason**: Wishlist changes, questions, answers and letters share one toggle.
**Migration**: See "Two toggles".

## ADDED Requirements

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
