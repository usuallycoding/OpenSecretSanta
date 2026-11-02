## ADDED Requirements

### Requirement: The Settings page shows the app's version
The Settings page SHALL show the label "Version " followed by the app's version after "Sign out", at the end of the page, in small, muted text that cannot be pressed. It SHALL be the same version the sign-in screen shows.

#### Scenario: At the end of the page
- **WHEN** a participant or an administrator opens Settings and the app's version is `1.0.0`
- **THEN** "Version 1.0.0" is the last thing on the page, after "Sign out"

#### Scenario: Reached by scrolling
- **WHEN** the page is taller than the screen
- **THEN** the label is reached by scrolling to the end of the page
