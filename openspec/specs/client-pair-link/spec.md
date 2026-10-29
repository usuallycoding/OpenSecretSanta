# client-pair-link Specification

## Purpose
The one control that opens what lies behind a giver and receiver pair: a compact tonal pill holding an arrow, a gift icon and an arrow, pressed as a whole, so that it reads as a button and not as a decoration.

## Requirements

### Requirement: The pair link
Where a row shows a giver and a receiver with something to press between them, the app SHALL show a pair link: an arrow, a gift icon and an arrow, in that order, inside a pill filled with the app's tonal container colour, drawn as one control. Pressing any part of it SHALL do the same thing. In a right-to-left language the arrows SHALL point the other way, so that they still lead from the giver to the receiver.

#### Scenario: Three icons in one pill
- **WHEN** a pair link is shown
- **THEN** it shows an arrow, a gift icon and an arrow in that order, on one tonal pill

#### Scenario: Pressed anywhere
- **WHEN** the person presses the gift icon, or either arrow, or the pill beside them
- **THEN** the same thing opens each time

#### Scenario: Both themes
- **WHEN** the app is in its light theme and in its dark theme
- **THEN** the pill and its icons are drawn in that theme's tonal container colours and are readable in both

#### Scenario: Right to left
- **WHEN** the app is shown in a right-to-left language
- **THEN** the arrows point from the giver, at the start of the row, to the receiver, at the end of it

### Requirement: A touch target that shows it is pressed
The whole pair link SHALL be a touch target at least 48 density-independent pixels high and wide, and SHALL show that it has been pressed while it is.

#### Scenario: Size
- **WHEN** a pair link is shown
- **THEN** the area that responds to a press is at least 48 by 48 density-independent pixels

#### Scenario: Pressed
- **WHEN** the person presses it
- **THEN** it shows the press, and what it opens opens when the press ends on it

### Requirement: Announced once
A screen reader SHALL announce a pair link as a single button with a label that says what it opens, and SHALL NOT announce its icons separately. The label SHALL be the one the place already had for its arrow: "Gifter" for the first row of a participant's group page, "Receiver" for the second, and "<santa> gives to <receiver>" for a row of an administrator's pair list.

#### Scenario: One announcement
- **WHEN** a screen reader reaches a pair link in a participant's first row
- **THEN** it announces one button, "Gifter", and not an arrow, a gift and an arrow

#### Scenario: Admin row
- **WHEN** a screen reader reaches the pair link in the row `alice -> bob` of an administrator's list
- **THEN** it announces one button, "alice gives to bob"

### Requirement: Where the pair link is used
The pair link SHALL replace the arrow in the participant's two rows on a group page and in every row of an administrator's pair list, and SHALL be the same control in all three. It SHALL NOT be used elsewhere: the gift icon alone in a participant's Pending row and the gift icon beside each participant in an administrator's list SHALL stay as they are.

#### Scenario: Participant rows
- **WHEN** a group is Active or Archived
- **THEN** both pair rows on the person's group page show a pair link between the two names

#### Scenario: Administrator's list
- **WHEN** an administrator opens an Active group with three pairs
- **THEN** each of the three rows shows a pair link between the Santa and the receiver

#### Scenario: Lone gift icons
- **WHEN** a group is Pending, and when an administrator reads a Pending group's participants
- **THEN** the gift icons shown there are single icons, not pair links

### Requirement: Names keep their room
In a row with a pair link, the first name SHALL stay at the start of the row and the second at the end, with the pair link between them. A name too long for its share of the row SHALL wrap onto another line, and SHALL NOT overlap the pair link or push it out of the row.

#### Scenario: Normal names
- **WHEN** a row has `alice` and `bob` on a phone-width screen
- **THEN** `alice` is at the start, `bob` at the end and the pair link between them, each on one line

#### Scenario: A long name
- **WHEN** a username with a mark such as "(you) *" is too long for half the row
- **THEN** it wraps onto a second line and the pair link keeps its size and its place between the names
