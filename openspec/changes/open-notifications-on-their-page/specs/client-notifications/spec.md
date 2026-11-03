# Spec Delta

## MODIFIED Requirements

### Requirement: Showing and opening notifications
A notification that arrives SHALL be shown by the system with the app's name and the notification's text, whether the app is open, in the background or closed. Tapping it SHALL open the screen it is about with the group list and the group's page behind it: a question from the person's Santa opens the Receiver page on its questions tab, an answer from the person's receiver opens the Gifter page on its questions tab, a change to the receiver's wishlist opens the Gifter page on its wishlist tab, and any other notification, or one with no known category, opens the group's page. When that group is not available to the person, the screen SHALL say so and offer the way back.

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
