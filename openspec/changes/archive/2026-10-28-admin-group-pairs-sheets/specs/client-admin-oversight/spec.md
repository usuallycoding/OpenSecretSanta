# Spec Delta

## MODIFIED Requirements

### Requirement: The draw
The detail of an Active or Archived group SHALL show one list of every Santa and receiver pair, loaded with `GET /admin/groups/{id}/assignments`, in the order the server returns them, each row showing the Santa's username, an arrow and the receiver's username. This list SHALL replace the participants list and any separate draw section. The administrator's own username SHALL be marked "you" wherever it appears, and a username whose profile is incomplete SHALL be followed by `*` wherever it appears, with a legend "* profile incomplete" under the list. A group that is Draft or Pending SHALL show no pair list and the app SHALL NOT request it.

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

### Requirement: Participant wishlist
Choosing "Read wishlist" on a participant of a Pending group SHALL open a bottom sheet with that person's wishlist for the group, loaded with `GET /admin/groups/{id}/participants/{username}/wishlist`, read-only and in the order the participant chose, showing for each item its title and, when set, its note and link, and when it was added or last changed. A link SHALL open in the browser. There SHALL be no wishlist screen, and the sheet SHALL offer no way to add, change, delete or reorder items.

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
- **THEN** it opens in the browser

### Requirement: Reading is not recorded
The oversight sheets and the pair list SHALL only read from the server and SHALL keep nothing of what they show once they are closed.

#### Scenario: Only reads
- **WHEN** the owner opens the pair list, a pair sheet and a wishlist sheet
- **THEN** the app sends only GET requests for them

### Requirement: Oversight errors and refreshing
Each oversight sheet SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached, SHALL keep what it shows while a refresh fails and SHALL reload each time it is opened. When the server answers 404 the sheet SHALL say the group or person is no longer available and offer to close; when it answers 409 `invalid_state` it SHALL say the group is not in a state where this can be read and offer to close.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while a pair sheet loads
- **THEN** the sheet says so and offers "Try again"

#### Scenario: Participant removed
- **WHEN** the server answers 404 for a participant's wishlist
- **THEN** the sheet says the person is no longer available and offers to close

#### Scenario: State changed
- **WHEN** the server answers 409 `invalid_state` because the group is no longer in a readable state
- **THEN** the sheet says so and offers to close

#### Scenario: Reopened
- **WHEN** a sheet is closed and opened again
- **THEN** it loads again and shows what the server now says

## REMOVED Requirements

### Requirement: All question threads
**Reason**: There is no Questions screen any more; one pair's thread is read in the pair sheet.
**Migration**: See "Pair sheet".

## ADDED Requirements

### Requirement: User sheet
Choosing either username in a row of the pair list SHALL open a bottom sheet with that person's details, loaded with `GET /admin/users/{username}` and shown as the user detail of `client-admin-users`: username, the profile fields that are set, and the temporary password while it is still temporary. The sheet SHALL offer "Reset password" with the same confirmation and one-time password dialog as the Users tab, and SHALL offer none for the administrator's own username.

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

### Requirement: Pair sheet
Choosing the arrow in a row of the pair list SHALL open a bottom sheet titled with the pair, showing the receiver's wishlist, loaded with `GET /admin/groups/{id}/participants/{receiver}/wishlist` and shown as in "Participant wishlist", and below it the thread of questions the Santa asked the receiver. The sheet SHALL offer no way to ask, answer or change anything.

#### Scenario: Wishlist and thread shown
- **WHEN** the owner chooses the arrow in `alice -> bob`
- **THEN** the sheet shows `bob`'s wishlist and the questions `alice` asked `bob` with his answers

#### Scenario: Read-only
- **WHEN** the pair sheet is shown, whatever the group's state
- **THEN** there is no control that asks, answers or changes a question or a wishlist item

### Requirement: Pair thread
The thread in the pair sheet SHALL be taken from `GET /admin/groups/{id}/questions`, only the thread of the chosen Santa and receiver, and SHALL list every question in order with its number, text and time asked and, when answered, the answer with when it was first given and when it was last changed, or "Not answered yet", and a note when nothing has been asked.

#### Scenario: Unanswered question
- **WHEN** a question has no answer
- **THEN** it shows "Not answered yet"

#### Scenario: Changed answer
- **WHEN** an answer was changed after it was first given
- **THEN** both the first and the last-changed times are shown

#### Scenario: Nothing asked
- **WHEN** the Santa has asked nothing yet
- **THEN** the thread part says nothing has been asked

#### Scenario: Only that pair
- **WHEN** the group has threads for four pairs
- **THEN** the sheet shows only the thread of the chosen pair
