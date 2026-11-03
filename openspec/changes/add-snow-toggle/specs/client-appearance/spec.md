# Spec Delta

## ADDED Requirements

### Requirement: Make it snow
The Appearance sheet SHALL have a "Make it snow" switch on every platform, off by default, that applies at once. While it is on, snow SHALL fall over the whole app, in front of the content, on every screen including the sign-in screens.

#### Scenario: Off by default
- **WHEN** the app is installed and the Appearance sheet is opened
- **THEN** "Make it snow" is off and nothing falls

#### Scenario: Switched on
- **WHEN** the person switches "Make it snow" on
- **THEN** snow starts falling over the screen at once, including behind the sheet that is open

#### Scenario: Switched off again
- **WHEN** the person switches it off
- **THEN** the snow stops at once and nothing is drawn

#### Scenario: Every screen
- **WHEN** the snow is on and the person moves between the sign-in screen, the groups, a group and Settings
- **THEN** snow falls over each of them

### Requirement: The snow looks like snow
The snow SHALL be small, soft and fairly faint flakes of different sizes and speeds that sway as they fall and start again at the top when they reach the bottom, with smaller flakes slower and fainter than larger ones. The flakes SHALL be white on the dark theme and a pale blue-grey on the light theme, and a large window SHALL have no more flakes than its size calls for.

#### Scenario: Falling
- **WHEN** the snow is on
- **THEN** flakes of different sizes fall at different speeds, sway sideways and reappear at the top after the bottom

#### Scenario: Light and dark
- **WHEN** the snow is on and the app is light, then dark
- **THEN** the flakes can be seen in both, white on the dark theme and a pale blue-grey on the light one

#### Scenario: A large window
- **WHEN** the snow is on in a very large window
- **THEN** it is about as dense as on a phone and the number of flakes stays within a fixed limit

### Requirement: The snow stays out of the way
The snow SHALL NOT take or block any tap, scroll or key, SHALL NOT be presented to a screen reader, and SHALL do no work while the app is not shown. A bottom sheet, dialog or picker that the platform shows in its own window SHALL be shown over the snow.

#### Scenario: Taps pass through
- **WHEN** the snow is on and the person taps a button, scrolls a list or types in a field that a flake is over
- **THEN** the tap, the scroll and the typing work as if there were no snow

#### Scenario: Screen reader
- **WHEN** the snow is on and a screen reader moves through a screen
- **THEN** it reads nothing about the snow

#### Scenario: Not shown, not running
- **WHEN** the snow is on and the app is in the background or its browser tab is hidden
- **THEN** the snow does no work until the app is shown again

#### Scenario: Sheets and dialogs
- **WHEN** the snow is on and a bottom sheet, a dialog or a picker that the platform shows in its own window is open
- **THEN** it is shown over the snow

## MODIFIED Requirements

### Requirement: The choice is remembered on the device
The choice of appearance, the dynamic-colours switch and the "Make it snow" switch SHALL be kept on the device across restarts, signing out and signing in as someone else, SHALL belong to the device and not to an account, and SHALL be read before the first screen is drawn so that the wrong theme is never shown first. When the device's storage cannot be read or written the app SHALL follow the device each run and the snow SHALL be off.

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

#### Scenario: Snow after signing out
- **WHEN** the snow is on and the person signs out and another person signs in on the same device
- **THEN** it is still falling
