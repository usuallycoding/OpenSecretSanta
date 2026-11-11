## MODIFIED Requirements

### Requirement: Participant wishlist
Choosing the gift icon at the end of a participant's row in a Pending group SHALL open that person's wishlist for the group, loaded with `GET /admin/groups/{id}/participants/{username}/wishlist`, read-only and in the order the participant chose, showing for each item its title and, when set, its note and every link, and when it was added or last changed. On a narrow window it SHALL open in a bottom sheet and on a wide window in a pane beside the group page, which stays in place.

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

#### Scenario: Several links
- **WHEN** an item has three links
- **THEN** the sheet or pane shows the three one under another in their order, each opening the link dialog

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

### Requirement: Wishlist links open the link dialog
Choosing any link in the participant's wishlist sheet SHALL open the link dialog of `client-wishlist` for that link.

#### Scenario: The same dialog as the person's own list
- **WHEN** the owner chooses a link in the wishlist sheet
- **THEN** the link dialog that the person's own wishlist uses opens
