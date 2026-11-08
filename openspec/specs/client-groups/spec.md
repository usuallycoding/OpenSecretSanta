# client-groups Specification

## Purpose

How a participant finds and opens the groups they are in: the Groups tab with each group's state, date, place and size, and a group's page with its details and participants.

## Requirements

### Requirement: Groups tab
The participant view SHALL have a Groups tab that lists the groups the signed-in person participates in, as returned by `GET /groups`, in the order the server returns them. Each entry SHALL show the group's name on one row, with an ellipsis when it is longer, and its state and, when present, its date and time and its location, and the number of participants. The name SHALL NOT open anything by itself: choosing the entry opens the group.

#### Scenario: Listed groups
- **WHEN** the person opens the Groups tab and is in two groups
- **THEN** both are listed with their name, state, date, time, location and number of participants

#### Scenario: Missing details are left out
- **WHEN** a group has no date, time or location
- **THEN** the entry shows only what the group has

#### Scenario: No groups
- **WHEN** the person is in no group
- **THEN** the tab explains that an organiser adds people to groups by username instead of showing an empty list

#### Scenario: Draft groups
- **WHEN** an organiser has named the person only in a group that is still a Draft
- **THEN** that group is not shown, because the server does not return it

#### Scenario: A long name
- **WHEN** a group's name is longer than one row
- **THEN** the entry shows one row with an ellipsis, and choosing it opens the group

### Requirement: Group states
The app SHALL show a group's state as Pending, Active or Archived, with a short note on the detail screen saying what it means: Pending, that the organiser has not drawn the names yet; Active, that the names have been drawn; Archived, that the exchange is over. No note and no other part of the groups screens SHALL say or imply who gives to whom.

#### Scenario: Pending group
- **WHEN** a group is Pending
- **THEN** it is labelled Pending and its note says the names have not been drawn yet

#### Scenario: Active group
- **WHEN** a group is Active
- **THEN** it is labelled Active and its note says the names have been drawn, without naming anyone

#### Scenario: Archived group
- **WHEN** a group is Archived
- **THEN** it is labelled Archived and its note says the exchange is over

### Requirement: Dates and times are shown as the group has them
The app SHALL show a group's date and time as the local values the organiser entered, formatted for reading (for example "24 Dec 2026, 18:30"), without converting them to another timezone.

#### Scenario: Date and time
- **WHEN** a group has the date `2026-12-24` and the time `18:30`
- **THEN** the app shows "24 Dec 2026, 18:30" on every platform and in every timezone

#### Scenario: Date only
- **WHEN** a group has a date but no time
- **THEN** only the date is shown

### Requirement: Loading, errors and refresh
While a groups screen loads, the app SHALL show a progress indicator. When the server cannot be reached or answers with an error, the app SHALL show a message that says so, without technical details, with an action to try again. The groups screens SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and SHALL reload when the app returns to the foreground, keeping what is shown while the new answer loads.

#### Scenario: Loading
- **WHEN** a groups screen is opened
- **THEN** a progress indicator is shown until the answer arrives

#### Scenario: Server unreachable
- **WHEN** the groups cannot be loaded because the server cannot be reached
- **THEN** the screen says so and offers "Try again", which loads them again

#### Scenario: Refresh
- **WHEN** the person refreshes the list after an organiser has activated a group
- **THEN** the list shows the new state

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible groups screen reloads without clearing what it shows

#### Scenario: Session ended
- **WHEN** the server answers 401 while a groups screen loads
- **THEN** the app handles it as for any request: the sign-in screen is shown with the note that the person was signed out

### Requirement: Group page
Choosing a group SHALL open its page, loaded with `GET /groups/{id}`, showing its name, centered horizontally, on one row with an ellipsis when it is longer, then its state and state note, its description, the card with its date and time, location and budget, then the two cards of `client-group-pages`, and at the end every participant's username as a chip, with the person's own chip highlighted. A description the group does not have SHALL be left out; the card shows `TBD` for the other parts it does not have.

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
On the group page the group's date and time, location and budget SHALL be shown together in one card, in that order, below the name, the state with its note and the description card and above the Secret Santa card. The card SHALL always be shown, with a line for each of the three, and a line for a part the group does not have SHALL show `TBD` in place of its value.

#### Scenario: All three present
- **WHEN** a group has a description, the date `2026-12-24`, the time `18:30`, the location "Grandma's house" and the budget 100 in `RON`
- **THEN** the description is in a card of its own above this card, and this card shows the date and time, the location and the budget, in that order

#### Scenario: Parts left out
- **WHEN** a group has a budget and nothing else of the three
- **THEN** the card shows the date and time line and the location line with `TBD` and the budget line with the budget

#### Scenario: Nothing to show
- **WHEN** a group has no date, no time, no location and no budget, with or without a description
- **THEN** the card is shown with three lines, each with its icon and `TBD`

### Requirement: The card marks the date and the location with icons
In the card the date and time SHALL be on a line that starts with a calendar icon, followed by the date and time as `Dates and times are shown as the group has them` describes, or `TBD` when the group has neither, and the location on a line that starts with a map pin icon, followed by the location, or `TBD` when the group has none. The icons SHALL take the place of the words "When?" and "Where?", which SHALL NOT be shown.

#### Scenario: Date without a time
- **WHEN** a group has a date and no time
- **THEN** the card's date line is the calendar icon followed by the date alone

#### Scenario: No date and no time
- **WHEN** a group has neither a date nor a time
- **THEN** the card's date line is the calendar icon followed by `TBD`

#### Scenario: No location
- **WHEN** a group has no location
- **THEN** the card's location line is the map pin icon followed by `TBD`

#### Scenario: No words
- **WHEN** a group has a date and a location
- **THEN** the card shows no "When?" or "Where?" text, only the icons and the values

#### Scenario: Screen reader
- **WHEN** a screen reader reads the card
- **THEN** it reads the lines as "When? - " and "Where? - " followed by their values, or by `TBD`, and the icons announce nothing of their own

### Requirement: Only the information is in the card
The name, the state and its note, the description, the Secret Santa and Your stuff cards and the participant chips SHALL NOT be in the card. The card SHALL hold the three information lines and, at its end, the Group Notifications icon, and nothing else.

#### Scenario: Name and state stay outside
- **WHEN** a group page is shown
- **THEN** the name, the state and its note and the description are above the card and the two cards and the chips are below it, none of them inside it

#### Scenario: The icon is inside
- **WHEN** a Pending, an Active or an Archived group's page is shown
- **THEN** the Group Notifications icon is in the card, at its end, and the three lines are in it too

### Requirement: A cut group name opens in full
Choosing a name that was cut SHALL open a dialog with the whole name and the copy icon.

#### Scenario: Copy from the dialog
- **WHEN** a person chooses a name that was cut
- **THEN** a dialog shows the whole name with the copy icon

### Requirement: The chips are part of the page
The chips SHALL be part of the page itself, not in a box of their own: they wrap onto as many lines as needed, and the whole page (details, cards and chips) scrolls together and is refreshed by pulling it where the platform has the gesture.

#### Scenario: One scroll
- **WHEN** a group has more participants than fit on the screen
- **THEN** the page grows and scrolls as one and no area scrolls inside the page

### Requirement: The chips are not tappable
The chips SHALL NOT be tappable.

#### Scenario: Tapping a chip
- **WHEN** a person taps a participant's chip
- **THEN** nothing happens

### Requirement: The card marks the budget with an icon
In the card the budget SHALL be on a line that starts with a bank icon, followed by the budget, a space and the currency, below the location line. The icon SHALL take the place of the word "Budget?", which SHALL NOT be shown. A group without a budget SHALL show `TBD` after the icon.

#### Scenario: Budget line
- **WHEN** a group has the budget 100 and the currency `EUR`
- **THEN** the card's budget line is the bank icon followed by "100 EUR"

#### Scenario: No budget
- **WHEN** a group has no budget
- **THEN** the card's budget line is the bank icon followed by `TBD`

#### Scenario: Screen reader
- **WHEN** a screen reader reads the card
- **THEN** it reads the budget line as "Budget? - " followed by the budget and the currency, or by `TBD`, and the icon announces nothing of its own

### Requirement: The description is shown in a card of its own
The group's description SHALL be shown in a card of its own, above the card with the date and time, location and budget and under the name and the state with its note, labelled "Description" for screen readers. A group without a description SHALL show no description card and nothing in its place.

#### Scenario: Description card above the information card
- **WHEN** a group has a description and a location
- **THEN** the description is in a card of its own above the card with the location, and not inside that card

#### Scenario: Description alone
- **WHEN** a group has a description and no date, time, location or budget
- **THEN** the description card is shown, and the information card below it shows its three lines with `TBD`

#### Scenario: No description
- **WHEN** a group has no description
- **THEN** no description card is shown and nothing is shown between the state and the information card
