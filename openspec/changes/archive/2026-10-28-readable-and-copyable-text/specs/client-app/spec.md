# Spec Delta

## ADDED Requirements

### Requirement: Long read-only text is cut and opens in full
Read-only text that people wrote (a wishlist title, note and link, a question and its answer, a group's description, and a user's or receiver's profile fields) SHALL show at most six lines, with an ellipsis when there is more. Choosing text that was cut SHALL open a dialog with its full text, which scrolls when it is long and can be closed. Text that is not cut SHALL do nothing when chosen.

#### Scenario: A long note is cut
- **WHEN** a wishlist item has a note of more than six lines
- **THEN** the list shows the first six lines and an ellipsis

#### Scenario: Open the full text
- **WHEN** the person chooses a note that was cut
- **THEN** a dialog shows the whole note and can be closed

#### Scenario: A short note
- **WHEN** the person chooses a note that fits in six lines
- **THEN** no dialog opens

#### Scenario: Own list
- **WHEN** the person chooses a cut note on their own wishlist
- **THEN** the full text opens in a dialog, and choosing the rest of the item still opens it for editing

### Requirement: Read-only text can be copied
Every read-only value of the kinds named in the previous requirement, and the username headings, a user's temporary password and the Santa reveal, SHALL end with a copy icon that puts the whole text, not only the part shown, on the clipboard. The icon SHALL change to a check mark for about a second afterwards, and SHALL show that the copy failed when the platform refuses it. Labels, screen titles, status words and buttons SHALL NOT have one.

#### Scenario: Copy an address
- **WHEN** the owner chooses the copy icon next to a user's address
- **THEN** the whole address is on the clipboard and the icon shows a check mark for a moment

#### Scenario: Copy a cut note
- **WHEN** the person chooses the copy icon of a note that is cut at six lines
- **THEN** the clipboard holds the whole note

#### Scenario: Copy in the full-text dialog
- **WHEN** the full-text dialog of a note is open
- **THEN** it has the same copy icon

#### Scenario: Copy refused
- **WHEN** the platform refuses access to the clipboard
- **THEN** the icon shows that the copy failed and nothing else changes
