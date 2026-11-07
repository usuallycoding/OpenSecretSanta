## ADDED Requirements

### Requirement: Snow falls unless it is switched off
The Appearance sheet SHALL have a "Make it snow" switch on every platform, on by default, that applies at once. While it is on, snow SHALL fall over the whole app, in front of the content, on every screen including the sign-in screens.

#### Scenario: On by default
- **WHEN** the app is installed and opened, before anything has been chosen
- **THEN** snow is already falling, and the Appearance sheet shows "Make it snow" on

#### Scenario: Switched on
- **WHEN** the person switches "Make it snow" on
- **THEN** snow starts falling over the screen at once, including behind the sheet that is open

#### Scenario: Switched off again
- **WHEN** the person switches it off
- **THEN** the snow stops at once and nothing is drawn

#### Scenario: Every screen
- **WHEN** the snow is on and the person moves between the sign-in screen, the groups, a group and Settings
- **THEN** snow falls over each of them

## MODIFIED Requirements

### Requirement: The choice is remembered on the device
The choice of appearance, the dynamic-colours switch and the "Make it snow" switch SHALL be kept on the device across restarts, signing out and signing in as someone else, SHALL belong to the device and not to an account, and SHALL be read before the first screen is drawn so that the wrong theme is never shown first. When the device's storage cannot be read or written the app SHALL follow the device each run and the snow SHALL be on, as it is by default.

#### Scenario: After a restart
- **WHEN** the person chose "Dark", closes the app and opens it again
- **THEN** the first screen is already dark

#### Scenario: After signing out
- **WHEN** the person signs out and another person signs in on the same device
- **THEN** the choice is unchanged

#### Scenario: Storage unavailable
- **WHEN** the device's storage cannot be read or written
- **THEN** the app follows the device and still lets the person choose for the current run

#### Scenario: Snow after a restart
- **WHEN** the person switched "Make it snow" on, closes the app and opens it again
- **THEN** snow is already falling on the first screen

#### Scenario: Snow switched off stays off
- **WHEN** the person switched "Make it snow" off, closes the app and opens it again
- **THEN** no snow falls on the first screen

#### Scenario: Snow after signing out
- **WHEN** the snow is on and the person signs out and another person signs in on the same device
- **THEN** it is still falling

## REMOVED Requirements

### Requirement: Make it snow
**Reason**: The switch is now on by default, so the requirement is restated as "Snow falls unless it is switched off", with the same behaviour for switching it on and off and for every screen.
**Migration**: None; the switch, its label and its effects are unchanged, only its default.
