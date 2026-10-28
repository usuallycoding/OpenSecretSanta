## MODIFIED Requirements

### Requirement: Group page
Choosing a group SHALL open its page, loaded with `GET /groups/{id}`, showing its name on one row with an ellipsis when it is longer, then its state and state note, description, date, time and location, then the pair rows of `client-group-pages`, and at the end every participant's username as a chip, with the person's own chip highlighted. Choosing a name that was cut SHALL open a dialog with the whole name and the copy icon. The chips SHALL be part of the page itself, not in a box of their own: they wrap onto as many lines as needed, and the whole page (details, pair rows and chips) scrolls together and is refreshed by pulling it where the platform has the gesture. The chips SHALL NOT be tappable. Parts the group does not have SHALL be left out.

#### Scenario: Opening a group
- **WHEN** the person chooses a group in the list
- **THEN** the page shows its details, the pair rows and, at the end, a chip for every participant's username with their own highlighted

#### Scenario: Back to the list
- **WHEN** the person goes back from the group page
- **THEN** the list is shown again

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist, for example because the person was removed from it
- **THEN** the page says the group is no longer available and offers to go back

#### Scenario: Many participants
- **WHEN** the group has more participants than fit on the screen
- **THEN** the chips wrap onto as many lines as needed, the page grows and scrolls as one, and no area scrolls inside the page

#### Scenario: Few participants
- **WHEN** the group has four participants on a tall screen
- **THEN** the chips follow the pair rows directly, with no empty box below them

#### Scenario: A long name
- **WHEN** the group's name is longer than one row and the person chooses it
- **THEN** a dialog shows the whole name and can be closed

#### Scenario: Own chip
- **WHEN** the person `alice` is a participant
- **THEN** her chip is highlighted and has no "you" text, and the other chips are not highlighted
