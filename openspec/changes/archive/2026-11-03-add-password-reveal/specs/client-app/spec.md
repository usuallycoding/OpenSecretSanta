## ADDED Requirements

### Requirement: Password fields can be revealed
Every field in which the person types a password, the one on the sign-in screen and the three of the password-change form, SHALL have a button at its end that shows the password as text and, when chosen again, hides it. Each field SHALL have its own button and SHALL start with its password hidden.

#### Scenario: Show a password
- **WHEN** the person chooses the button at the end of a hidden password field
- **THEN** that field shows its text and the button changes to the one that hides it

#### Scenario: Hide it again
- **WHEN** the person chooses the button of a field that shows its password
- **THEN** the field hides its text again

#### Scenario: Fields are independent
- **WHEN** the person shows the new password on the password-change form
- **THEN** the current password and the repeat stay hidden

#### Scenario: Starts hidden
- **WHEN** a password field is shown, including after the screen was left and opened again
- **THEN** its password is hidden

#### Scenario: Typing keeps working
- **WHEN** the person shows a password and goes on typing
- **THEN** the text they type is added to the password and the field keeps its place in the form

### Requirement: Shown passwords are hidden when the app leaves the foreground
A password that is shown SHALL be hidden again when the app goes to the background.

#### Scenario: App goes to the background
- **WHEN** a password field shows its password and the app goes to the background
- **THEN** the password is hidden when the person comes back

### Requirement: The reveal button is named for screen readers
The button SHALL be named "Show password" while the password is hidden and "Hide password" while it is shown.

#### Scenario: Names
- **WHEN** a screen reader reaches the button of a hidden and then of a shown password
- **THEN** it announces "Show password" and then "Hide password"
