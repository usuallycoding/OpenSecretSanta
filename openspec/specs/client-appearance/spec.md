# client-appearance Specification

## Purpose
How the app looks: its own palette and type, following the device's light or dark setting by default, a way to choose light or dark, and Android's dynamic colours where the device has them.

## Requirements

### Requirement: The app uses its own palette and follows the device
The app SHALL draw with its own colour palette and typography on every platform, in a light and a dark variant, and by default SHALL show the variant that matches the device's light or dark setting, following the device when it changes while the app is open.

#### Scenario: Device in dark mode
- **WHEN** the device is set to dark and the person has not chosen otherwise
- **THEN** the app shows the dark variant of its palette

#### Scenario: Device in light mode
- **WHEN** the device is set to light and the person has not chosen otherwise
- **THEN** the app shows the light variant of its palette

#### Scenario: The device changes meanwhile
- **WHEN** the device switches between light and dark while the app is open and following the device
- **THEN** the app switches with it

#### Scenario: Same palette everywhere
- **WHEN** the app runs on Android, iOS or the web with dynamic colours off
- **THEN** every screen uses the same palette, not Material's stock colours

### Requirement: Choosing the appearance
The Settings page, in both the participant and the admin view, SHALL have an "Appearance" row that opens a bottom sheet with the choices "Follow device" (the default), "Light" and "Dark". Choosing one SHALL apply it at once, to every screen including the sign-in screen, and keep it until it is changed.

#### Scenario: Choose dark
- **WHEN** the person chooses "Dark" while the device is light
- **THEN** the app turns dark at once and stays dark

#### Scenario: Choose light
- **WHEN** the person chooses "Light" while the device is dark
- **THEN** the app turns light at once and stays light

#### Scenario: Back to the device
- **WHEN** the person chooses "Follow device"
- **THEN** the app shows whatever the device is set to

#### Scenario: Both views
- **WHEN** an administrator opens Settings in the admin view and in the participant view
- **THEN** "Appearance" is in both

#### Scenario: Current choice shown
- **WHEN** the sheet is opened
- **THEN** it shows which choice is in force

### Requirement: Dynamic colours on Android 12 and later
On Android 12 or later the Appearance sheet SHALL offer a "Dynamic colours" switch, off by default, that makes the app use the colours of the device's wallpaper in place of its own palette, in the light or dark variant in force. On every other platform and on earlier Android versions the switch SHALL NOT be shown and the app's palette SHALL always be used.

#### Scenario: Off by default
- **WHEN** the app is installed on Android 12 or later
- **THEN** it uses its own palette and the switch is off

#### Scenario: Switched on
- **WHEN** the person switches "Dynamic colours" on
- **THEN** the app takes its colours from the wallpaper at once, in the light or dark variant chosen

#### Scenario: Switched off again
- **WHEN** the person switches it off
- **THEN** the app's own palette returns

#### Scenario: Not offered elsewhere
- **WHEN** the sheet is opened on iOS, on the web or on Android 11 or earlier
- **THEN** there is no "Dynamic colours" switch

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
