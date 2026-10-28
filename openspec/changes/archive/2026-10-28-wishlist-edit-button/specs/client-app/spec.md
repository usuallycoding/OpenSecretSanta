## MODIFIED Requirements

### Requirement: Long read-only text is cut and opens in full
Read-only text that people wrote (a wishlist note and link, a question and its answer, a group's description, and a user's or receiver's profile fields) SHALL show at most three lines, with an ellipsis when there is more; a wishlist item's title SHALL show on one row, with an ellipsis when it is longer, wherever the item is shown. Choosing text that was cut SHALL open a dialog with its full text, which scrolls when it is long and can be closed. Text that is not cut SHALL do nothing when chosen.

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
- **THEN** the full text opens in a dialog, and nothing else on the item opens it for editing except its edit button

#### Scenario: A long title
- **WHEN** a wishlist item has a title longer than one row
- **THEN** it shows on one row with an ellipsis, and choosing it opens a dialog with the whole title
