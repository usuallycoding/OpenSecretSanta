# Spec Delta

## MODIFIED Requirements

### Requirement: Loading, errors and refresh
While a groups screen loads, the app SHALL show a progress indicator. When the server cannot be reached or answers with an error, the app SHALL show a message that says so, without technical details, with an action to try again. The groups screens SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and SHALL reload when the app returns to the foreground, keeping what is shown while the new answer loads.

#### Scenario: Loading
- **WHEN** a groups screen is opened
- **THEN** a progress indicator is shown until the answer arrives

#### Scenario: Server unreachable
- **WHEN** the groups cannot be loaded because the server cannot be reached
- **THEN** the screen says so and offers "Try again", which loads them again

#### Scenario: Refresh
- **WHEN** the person refreshes the list after an organiser has activated a group
- **THEN** the list shows the new state

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible groups screen reloads without clearing what it shows

#### Scenario: Session ended
- **WHEN** the server answers 401 while a groups screen loads
- **THEN** the app handles it as for any request: the sign-in screen is shown with the note that the person was signed out
