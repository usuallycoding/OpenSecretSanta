# Spec Delta

## MODIFIED Requirements

### Requirement: Four toggles
Below the device switch the sheet SHALL show one toggle each for announcements (messages from the organiser, being added to a group, names drawn), wishlist changes, questions from your Santa and answers from your receiver, each with a short description. A toggle SHALL show the server's current value, SHALL save at once when changed, and SHALL be disabled while the device switch is off.

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
