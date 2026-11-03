## ADDED Requirements

### Requirement: Swiping outside the keyboard only hides it
While the keyboard is open in a bottom sheet, a swipe or drag in any direction on the sheet that nothing on the sheet uses SHALL hide the keyboard and SHALL NOT move or close the sheet.

#### Scenario: Swipe down on a short sheet
- **WHEN** the keyboard is open in a sheet whose content does not scroll and the person swipes down on the sheet
- **THEN** the keyboard hides and the sheet stays open and in place

#### Scenario: Swipe in another direction
- **WHEN** the keyboard is open in a sheet and the person swipes sideways or up on the sheet
- **THEN** the keyboard hides and the sheet is not moved

#### Scenario: Drag inside a field
- **WHEN** the keyboard is open and the person drags inside a multi-line field that scrolls or selects its own text
- **THEN** the field uses the drag and the keyboard stays open

#### Scenario: Swipe again
- **WHEN** the keyboard has been hidden by a swipe and the person swipes down on the sheet
- **THEN** the sheet is dragged and can be closed as usual

### Requirement: Tapping outside the sheet hides the keyboard first
While the keyboard is open in a bottom sheet, a tap on the dimmed area outside the sheet SHALL hide the keyboard and SHALL NOT close the sheet. With the keyboard closed, the same tap SHALL close the sheet.

#### Scenario: Tap with the keyboard open
- **WHEN** the keyboard is open in a sheet and the person taps the dimmed area
- **THEN** the keyboard hides and the sheet stays open

#### Scenario: Tap with the keyboard closed
- **WHEN** the keyboard is closed and the person taps the dimmed area
- **THEN** the sheet closes

### Requirement: Sheets without text are unchanged
A bottom sheet in which the keyboard is not open SHALL be dragged, scrolled and closed as before, including the read-only sheets, in which the keyboard never opens.

#### Scenario: Read-only sheet
- **WHEN** a read-only sheet is open and the person swipes down on it
- **THEN** the sheet is dragged and closes as before

#### Scenario: Scrolling sheet with the keyboard open
- **WHEN** the keyboard is open in a sheet that scrolls and the person drags it so that it scrolls
- **THEN** the content scrolls and the keyboard closes, as the dragging requirement says, and the sheet is not moved
