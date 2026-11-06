# Spec Delta

## MODIFIED Requirements

### Requirement: Tapping a notification opens its screen
Tapping a notification SHALL open the screen it is about with the group list and the group's page behind it: a question from the person's Santa opens the Receiver page on its questions tab, an answer from the person's receiver opens the Gifter page on its questions tab, a change to the receiver's wishlist opens the Gifter page on its wishlist tab, a letter from the person's receiver opens the Inbox page, and any other notification, or one with no known category, opens the group's page.

#### Scenario: A letter
- **WHEN** a person taps the notification of a letter from their receiver
- **THEN** the screens, from the bottom, are the group list, the group's page and the Inbox page

#### Scenario: Order of the screens
- **WHEN** a person taps the notification of a question from their Santa
- **THEN** the screens, from the bottom, are the group list, the group's page and the Receiver page on its questions tab

## ADDED Requirements

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

## REMOVED Requirements

### Requirement: Four toggles
**Reason**: There is now a fifth, for letters from the person's receiver.
**Migration**: See "Five toggles".
