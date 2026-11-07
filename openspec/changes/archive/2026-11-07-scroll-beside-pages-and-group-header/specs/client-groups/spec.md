## ADDED Requirements

### Requirement: The description is shown in a card of its own
The group's description SHALL be shown in a card of its own, above the card with the date and time, location and budget and under the name and the state with its note, labelled "Description" for screen readers. A group without a description SHALL show no description card and nothing in its place.

#### Scenario: Description card above the information card
- **WHEN** a group has a description and a location
- **THEN** the description is in a card of its own above the card with the location, and not inside that card

#### Scenario: Description alone
- **WHEN** a group has a description and no date, time, location or budget
- **THEN** the description card is shown and no information card is shown

#### Scenario: No description
- **WHEN** a group has no description
- **THEN** no description card is shown and nothing is shown between the state and the information card

## MODIFIED Requirements

### Requirement: Group page
Choosing a group SHALL open its page, loaded with `GET /groups/{id}`, showing its name, centered horizontally, on one row with an ellipsis when it is longer, then its state and state note, description, date, time and location, then the two cards of `client-group-pages`, and at the end every participant's username as a chip, with the person's own chip highlighted. Parts the group does not have SHALL be left out.

#### Scenario: Opening a group
- **WHEN** the person chooses a group in the list
- **THEN** the page shows its name centered, its details, the two cards and, at the end, a chip for every participant's username with their own highlighted

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
- **THEN** the chips follow the cards directly, with no empty box below them

#### Scenario: A long name
- **WHEN** the group's name is longer than one row and the person chooses it
- **THEN** a dialog shows the whole name and can be closed

#### Scenario: Own chip
- **WHEN** the person `alice` is a participant
- **THEN** her chip is highlighted and has no "you" text, and the other chips are not highlighted

### Requirement: Group information is shown in a card
On the group page the group's date and time, location and budget SHALL be shown together in one card, in that order, below the name, the state with its note and the description card and above the Secret Santa card. Parts the group does not have SHALL be left out, and a group with none of them SHALL show no card.

#### Scenario: All three present
- **WHEN** a group has a description, the date `2026-12-24`, the time `18:30`, the location "Grandma's house" and the budget 100 in `RON`
- **THEN** the description is in a card of its own above this card, and this card shows the date and time, the location and the budget, in that order

#### Scenario: Parts left out
- **WHEN** a group has a budget and nothing else of the three
- **THEN** the card shows only the budget line

#### Scenario: Nothing to show
- **WHEN** a group has no date, no time, no location and no budget, with or without a description
- **THEN** no card for these is shown (a description has its own card)

## REMOVED Requirements

### Requirement: The description is shown outside the card
**Reason**: The description now has a card of its own, so it is no longer plain text outside the cards; the new requirement "The description is shown in a card of its own" replaces it.
**Migration**: None; the text, its label for screen readers and its place above the information card are unchanged.
