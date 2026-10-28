# Spec Delta

## ADDED Requirements

### Requirement: Multi-line fields stop growing at three rows
Every multi-line text field SHALL keep its minimum height while its text is short, grow with its text up to three rows and then stop growing and scroll inside itself.

#### Scenario: Short text
- **WHEN** a multi-line field holds one line
- **THEN** it keeps its minimum height

#### Scenario: Long text
- **WHEN** the text of a multi-line field is longer than three rows
- **THEN** the field is three rows tall and its text scrolls inside it

#### Scenario: Same everywhere
- **WHEN** the address, the delivery information, a wishlist note, a question, an answer and a group description are edited
- **THEN** each stops growing at three rows

### Requirement: Actions that add are floating buttons
Adding a wishlist item, asking a question and creating a group (the owner's "New group") SHALL each be a floating action button at the bottom end of its screen, showing an icon and offering the action's name to assistive technology, shown only while the action is available. When the action is not available the screen SHALL keep saying why in text, as before. The button SHALL NOT cover the last row of the content.

#### Scenario: Add item
- **WHEN** the person's wishlist can be changed and has room
- **THEN** a floating button with a plus icon opens the add sheet

#### Scenario: Not available
- **WHEN** the wishlist is full or closed
- **THEN** there is no button and the screen says so in text

#### Scenario: Last row
- **WHEN** a list is scrolled to its end
- **THEN** its last item is fully visible above the button

## MODIFIED Requirements

### Requirement: Long read-only text is cut and opens in full
Read-only text that people wrote (a wishlist title, note and link, a question and its answer, a group's description, and a user's or receiver's profile fields) SHALL show at most three lines, with an ellipsis when there is more. Choosing text that was cut SHALL open a dialog with its full text, which scrolls when it is long and can be closed. Text that is not cut SHALL do nothing when chosen.

#### Scenario: A long note is cut
- **WHEN** a wishlist item has a note of more than three lines
- **THEN** the list shows the first three lines and an ellipsis

#### Scenario: Open the full text
- **WHEN** the person chooses a note that was cut
- **THEN** a dialog shows the whole note and can be closed

#### Scenario: A short note
- **WHEN** the person chooses a note that fits in three lines
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
- **WHEN** the person chooses the copy icon of a note that is cut at three lines
- **THEN** the clipboard holds the whole note

#### Scenario: Copy in the full-text dialog
- **WHEN** the full-text dialog of a note is open
- **THEN** it has the same copy icon

#### Scenario: Copy refused
- **WHEN** the platform refuses access to the clipboard
- **THEN** the icon shows that the copy failed and nothing else changes

## REMOVED Requirements

### Requirement: Multi-line fields stop growing at six rows
**Reason**: The limit is now three rows (`Multi-line fields stop growing at three rows`).
**Migration**: None; the fields behave the same with the smaller limit.
