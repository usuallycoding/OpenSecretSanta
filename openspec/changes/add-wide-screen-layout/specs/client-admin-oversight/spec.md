## MODIFIED Requirements

### Requirement: Participant wishlist
Choosing the gift icon at the end of a participant's row in a Pending group SHALL open that person's wishlist for the group, loaded with `GET /admin/groups/{id}/participants/{username}/wishlist`, read-only and in the order the participant chose, showing for each item its title and, when set, its note and link, and when it was added or last changed. On a narrow window it SHALL open in a bottom sheet and on a wide window in a pane beside the group page, which stays in place.

#### Scenario: List shown in order
- **WHEN** a Pending group's participant has three items in a chosen order
- **THEN** the sheet or pane shows the three in that order

#### Scenario: Empty list
- **WHEN** the participant has no items
- **THEN** the sheet or pane says the list is empty

#### Scenario: Read-only
- **WHEN** the wishlist is shown
- **THEN** there is no control that adds, edits, deletes or moves an item

#### Scenario: Link opens
- **WHEN** the owner chooses an item's link
- **THEN** the link dialog opens, and the link opens in the browser only from its "Open in browser" button

#### Scenario: Pane beside the page
- **WHEN** the owner chooses the gift icon of a participant's row on a wide window
- **THEN** the wishlist opens in a pane beside the group page, which keeps its scroll place and can still be used

#### Scenario: Another person's wishlist
- **WHEN** the pane is open and the owner chooses another participant's gift icon
- **THEN** the pane shows that person's wishlist

#### Scenario: Closing the pane
- **WHEN** the owner closes the pane, or goes back
- **THEN** the pane closes and the group page stays open

#### Scenario: Window made narrow
- **WHEN** a window with the pane open is made narrower than 840dp
- **THEN** the same wishlist is shown in a bottom sheet

### Requirement: Pair sheet
Choosing the pair link in a row of the pair list SHALL open a bottom sheet titled with the pair, showing the receiver's wishlist, loaded with `GET /admin/groups/{id}/participants/{receiver}/wishlist` and shown as in "Participant wishlist", and below it the thread of questions the Santa asked the receiver. On a wide window it SHALL open in a pane beside the group page instead, as the participant wishlist does. It SHALL offer no way to ask, answer or change anything.

#### Scenario: Wishlist and thread shown
- **WHEN** the owner chooses the pair link in the row of `alice` and `bob`
- **THEN** the sheet or pane shows `bob`'s wishlist and the questions `alice` asked `bob` with his answers

#### Scenario: Read-only
- **WHEN** the pair sheet is shown, whatever the group's state
- **THEN** there is no control that asks, answers or changes a question or a wishlist item

#### Scenario: Pane beside the page
- **WHEN** the owner chooses a pair link on a wide window
- **THEN** the pair opens in a pane beside the group page, which keeps its scroll place, and choosing another pair shows that pair in the pane

#### Scenario: Closing the pane
- **WHEN** the owner closes the pane, or goes back
- **THEN** the pane closes and the group page stays open
