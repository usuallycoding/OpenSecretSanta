## ADDED Requirements

### Requirement: The sign-in screen shows the app's version
The sign-in screen SHALL show the label "Version " followed by the app's version at its bottom, in small, muted text that cannot be pressed. The version SHALL be the one number the project keeps for the app, the same on Android, iOS and the web.

#### Scenario: Shown when signed out
- **WHEN** the sign-in screen is shown and the app's version is `1.0.0`
- **THEN** it shows "Version 1.0.0" at its bottom, below the sign-in action

#### Scenario: Same on every platform
- **WHEN** the sign-in screen is opened on Android, iOS and the web from the same version of the project
- **THEN** all three show the same version
