# Spec Delta

## ADDED Requirements

### Requirement: Date and time are chosen with pickers
The group form's Date and Time fields SHALL be read-only and open a date picker or a time picker when chosen, and SHALL show the chosen value as `2026-12-24` and `18:30`. A field with a value SHALL show a clear control that empties it. The time picker SHALL be 24-hour on every device. The date and time SHALL be local values: the date chosen is the date saved in every timezone.

#### Scenario: Pick a date
- **WHEN** the administrator chooses the Date field, picks 24 December 2026 and confirms
- **THEN** the field shows `2026-12-24` and saving sends that date

#### Scenario: Pick a time
- **WHEN** the administrator chooses the Time field, picks 18:30 and confirms
- **THEN** the field shows `18:30`, and the picker showed hours 0 to 23

#### Scenario: Clear a date or time
- **WHEN** a field has a value and the administrator chooses its clear control
- **THEN** the field is empty and saving sends the group without that date or time

#### Scenario: Cancelled picker
- **WHEN** the administrator opens a picker and cancels
- **THEN** the field keeps the value it had, and stays empty if it was empty

#### Scenario: No typing
- **WHEN** the administrator uses the Date or Time field
- **THEN** no text can be typed into it and the date picker has no typed-entry mode

#### Scenario: Same date in any timezone
- **WHEN** the administrator picks 24 December 2026 on a device in any timezone
- **THEN** the field shows `2026-12-24` and the saved date is `2026-12-24`

### Requirement: Where the pickers start
When the field is empty, the date picker SHALL start on Christmas Day, 25 December of the current year or of the next year once that day has passed, and the time picker SHALL start on 00:00; neither SHALL fill the field until the administrator confirms. When the field has a value, its picker SHALL start on that value.

#### Scenario: Empty date
- **WHEN** the Date field is empty and it is 3 November 2026
- **THEN** the date picker opens on 25 December 2026 and the field stays empty until confirmed

#### Scenario: After Christmas
- **WHEN** the Date field is empty and it is 26 December 2026
- **THEN** the date picker opens on 25 December 2027

#### Scenario: Empty time
- **WHEN** the Time field is empty
- **THEN** the time picker opens on 00:00 and the field stays empty until confirmed

#### Scenario: Existing value
- **WHEN** the administrator edits a group whose date is `2026-12-24` and time is `18:30` and opens each picker
- **THEN** the date picker starts on 24 December 2026 and the time picker on 18:30

## MODIFIED Requirements

### Requirement: The group form checks what the server checks
Before anything is sent the form SHALL check every field exactly as the server does: a name of 1 to 100 characters, a description of up to 2000, a location of up to 200, a real calendar date and a real time of day, with blanks taken as empty. A failed check SHALL show its message next to the field.

#### Scenario: Name required
- **WHEN** the administrator saves with a blank name
- **THEN** nothing is sent and the form says the name is required

#### Scenario: Invalid date or time
- **WHEN** the form holds a date or time that is not real, such as `2026-13-45` or `25:99`, which no picker can produce
- **THEN** nothing is sent and the form says what format is expected next to that field

#### Scenario: Blank means empty
- **WHEN** the administrator leaves description, location, date and time blank
- **THEN** the group is saved without them
