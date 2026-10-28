## MODIFIED Requirements

### Requirement: The draw
The detail of an Active or Archived group SHALL show one list of every Santa and receiver pair, loaded with `GET /admin/groups/{id}/assignments`, in the order the server returns them, each row showing the Santa's username, an arrow and the receiver's username, the Santa's username at the start of the row and the receiver's username at the end of the row. This list SHALL replace the participants list and any separate draw section, and SHALL be shown in the box at the end of the page described by `client-admin-groups`, with its legend inside the box. The administrator's own username SHALL be marked "you" wherever it appears, and a username whose profile is incomplete SHALL be followed by `*` wherever it appears, with a legend "* profile incomplete" under the list. A group that is Draft or Pending SHALL show no pair list and the app SHALL NOT request it.

#### Scenario: Pairs shown
- **WHEN** an Active group has `alice` giving to `bob` and `bob` giving to `carol`
- **THEN** one list shows `alice -> bob` and `bob -> carol` and no separate participants list

#### Scenario: Archived group
- **WHEN** a group is Archived
- **THEN** it shows the same pair list

#### Scenario: Owner's own pair
- **WHEN** the administrator is a participant in the group
- **THEN** their username is marked "you" in the pair where they are the Santa and in the pair where they are the receiver

#### Scenario: Incomplete profile
- **WHEN** `bob` has no address and gives to `carol` and receives from `alice`
- **THEN** `bob` is followed by `*` in both rows and the legend "* profile incomplete" is shown

#### Scenario: Not yet drawn
- **WHEN** the group is Pending
- **THEN** there is no pair list and no assignments request is made

#### Scenario: Draw cannot be loaded
- **WHEN** the assignments request fails while the group detail is shown
- **THEN** the list area shows the problem with "Try again" and the rest of the detail stays

#### Scenario: Long list
- **WHEN** the draw has more pairs than fit in the box
- **THEN** the list scrolls inside the box and the buttons above it stay in view

#### Scenario: Alignment
- **WHEN** an Active group has `alice` giving to `bob`
- **THEN** `alice` is at the start of the row and `bob` is at the end of the row, with the arrow between them

### Requirement: Participant wishlist
Choosing the gift icon at the end of a participant's row in a Pending group SHALL open a bottom sheet with that person's wishlist for the group, loaded with `GET /admin/groups/{id}/participants/{username}/wishlist`, read-only and in the order the participant chose, showing for each item its title and, when set, its note and link, and when it was added or last changed. Choosing a link SHALL open the link dialog of `client-wishlist`. There SHALL be no wishlist screen, and the sheet SHALL offer no way to add, change, delete or reorder items.

#### Scenario: List shown in order
- **WHEN** a Pending group's participant has three items in a chosen order
- **THEN** the sheet shows the three in that order

#### Scenario: Empty list
- **WHEN** the participant has no items
- **THEN** the sheet says the list is empty

#### Scenario: Read-only
- **WHEN** the wishlist sheet is shown
- **THEN** there is no control that adds, edits, deletes or moves an item

#### Scenario: Link opens
- **WHEN** the owner chooses an item's link
- **THEN** the link dialog opens, and the link opens in the browser only from its "Open in browser" button

### Requirement: User sheet
Choosing either username in a row of the pair list, or the username of a participant with an account in a Pending group, SHALL open a bottom sheet with that person's details, loaded with `GET /admin/users/{username}` and shown as the user detail of `client-admin-users`: username, the profile fields that are set, and the temporary password while it is still temporary. The sheet SHALL offer "Reset password" with the same confirmation and one-time password dialog as the Users tab, and SHALL offer none for the administrator's own username.

#### Scenario: Open from the Santa
- **WHEN** the owner chooses `alice` in the row `alice -> bob`
- **THEN** a sheet shows `alice`'s details

#### Scenario: Open from the receiver
- **WHEN** the owner chooses `bob` in the row `alice -> bob`
- **THEN** a sheet shows `bob`'s details

#### Scenario: Reset from the sheet
- **WHEN** the owner confirms "Reset password" for `alice` in her sheet
- **THEN** a dialog shows the new temporary password once and the sheet then shows `alice` as having to change it

#### Scenario: Own account
- **WHEN** the owner opens the sheet of their own username
- **THEN** there is no "Reset password"

#### Scenario: Person no longer available
- **WHEN** the server answers that the user does not exist for this administrator
- **THEN** the sheet says the user is no longer available and offers to close

#### Scenario: Open from a Pending participant
- **WHEN** the owner chooses the username of a participant with an account in a Pending group
- **THEN** a sheet shows that person's details
