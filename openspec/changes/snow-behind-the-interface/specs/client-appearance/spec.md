## MODIFIED Requirements

### Requirement: Snow falls unless it is switched off
The Appearance sheet SHALL have a "Make it snow" switch on every platform, on by default, that applies at once. While it is on, snow SHALL fall behind the whole app's content, on every screen including the sign-in screens, so that it shows in the open space of the screens and not over what is on them.

#### Scenario: On by default
- **WHEN** the app is installed and opened, before anything has been chosen
- **THEN** snow is already falling, and the Appearance sheet shows "Make it snow" on

#### Scenario: Switched on
- **WHEN** the person switches "Make it snow" on
- **THEN** snow starts falling behind the screen at once, and the sheet that is open is over it

#### Scenario: Switched off again
- **WHEN** the person switches it off
- **THEN** the snow stops at once and nothing is drawn

#### Scenario: Every screen
- **WHEN** the snow is on and the person moves between the sign-in screen, the groups, a group and Settings
- **THEN** snow falls behind each of them

### Requirement: The snow stays out of the way
The snow SHALL be drawn behind everything the app shows, SHALL NOT take or block any tap, scroll or key, SHALL NOT be presented to a screen reader, and SHALL do no work while the app is not shown. A bottom sheet, dialog or picker that the platform shows in its own window SHALL be shown over the snow.

#### Scenario: Behind the content
- **WHEN** the snow is on and a card, a button, a field's text or a line of text is where a flake falls
- **THEN** the flake is not seen over it: the flake passes behind a card and shows only in the open space around it

#### Scenario: Behind the bars
- **WHEN** the snow is on
- **THEN** the top bar, the bottom tab bar and, on a wide window, the sidebar hide the flakes behind them, as a card does

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

## ADDED Requirements

### Requirement: Text fields are filled
Every outlined text field of the app on a screen, single-line and multi-line, SHALL be filled with the theme's background colour, whether it is focused, unfocused, disabled or showing an error, so that the snow behind the interface never shows inside a field or behind what is typed in it. The fill SHALL follow the light and dark themes and dynamic colours. Bottom sheets and dialogs, and the fields inside them, SHALL NOT be given any colour for this: they look as they did.

#### Scenario: Snow behind a field
- **WHEN** the snow is on and a flake falls where a text field on a screen is
- **THEN** the flake is not seen inside the field or behind its text, label or placeholder

#### Scenario: Every kind of field on a screen
- **WHEN** the sign-in fields, a search field, a field of a form, a multi-line field and a password field are shown on a screen
- **THEN** each has the fill

#### Scenario: Both themes
- **WHEN** the app is light and then dark
- **THEN** the fields on screens are filled with that theme's background colour

#### Scenario: Sheets and dialogs are left alone
- **WHEN** a bottom sheet or a dialog with a text field is open
- **THEN** the sheet, the dialog and the field look as they did, with no fill added
