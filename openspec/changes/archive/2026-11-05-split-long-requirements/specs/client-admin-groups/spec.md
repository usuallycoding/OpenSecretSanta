## MODIFIED Requirements

### Requirement: Group detail shows who takes part
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its name on one row with an ellipsis when it is longer (choosing a name that was cut opens a dialog with the whole name), its details, its state with a sentence on what that state means for the owner, the actions of the group, and at the end who takes part.

#### Scenario: Group not available
- **WHEN** the server answers that the group does not exist for this administrator
- **THEN** the screen says the group is no longer available and offers to go back

#### Scenario: Pending group
- **WHEN** a Pending group's detail is shown
- **THEN** it lists the participants as rows, each username opens the user sheet and each row ends with a gift icon that opens the wishlist

#### Scenario: Draft offers no oversight
- **WHEN** the group is Draft
- **THEN** it lists the participants as chips with no gift icon and no pair list, and choosing a chip does nothing

#### Scenario: Drawn group
- **WHEN** the group is Active or Archived
- **THEN** the detail shows the pair list and no separate participants list, and no Questions button

#### Scenario: Many participants
- **WHEN** a Pending group has 13 participants
- **THEN** the page grows and scrolls as one, the buttons above the list scroll away with it, and no area scrolls inside the page

#### Scenario: A long name
- **WHEN** the group's name is longer than one row and the administrator chooses it
- **THEN** a dialog shows the whole name and can be closed

#### Scenario: Participant without an account in a Pending group
- **WHEN** a Pending group has a participant who has no account
- **THEN** their row shows the username and `*`, and choosing the username or the gift icon does nothing

### Requirement: Marks on participants
In the participants list of a Draft or Pending group a participant without an account, and a participant whose profile lacks a name or an address, SHALL each be followed by a single `*`, the same mark for both, and the list SHALL end with the legend "* no account or profile incomplete", shown only when at least one participant has the mark.

#### Scenario: Participants with marks
- **WHEN** a Draft group has `alice` with a complete profile and `newcomer` without an account
- **THEN** `alice` has no mark, `newcomer` is followed by `*` and the legend is shown

#### Scenario: Incomplete profile
- **WHEN** a participant's account has no address
- **THEN** that participant is followed by `*`, the same as a participant without an account

#### Scenario: Incomplete profile after the draw
- **WHEN** an Active group has a participant whose account has no address
- **THEN** that username is followed by `*` in the pair list

#### Scenario: Nobody marked
- **WHEN** every participant has an account and a complete profile
- **THEN** no `*` and no legend are shown

### Requirement: Duplicating a group
Choosing "Duplicate" on a group SHALL ask for confirmation, naming the group. Confirming SHALL create a Draft with `POST /admin/groups` having the original's description, location, date, time and participants and the name "Copy of " followed by the original's name, and SHALL open the new group.

#### Scenario: Duplicate an Active group
- **WHEN** the administrator confirms "Duplicate" on an Active group "Party" with participants `alice` and `bob`
- **THEN** a Draft "Copy of Party" with the same details and participants `alice` and `bob` is created and its detail opens, and "Party" is still Active

#### Scenario: Cancel
- **WHEN** the administrator cancels the confirmation
- **THEN** nothing is created

#### Scenario: A long name
- **WHEN** the original's name is 100 characters long
- **THEN** the copy's name is 100 characters long, starts with "Copy of " and ends with an ellipsis

#### Scenario: Participants without an account
- **WHEN** the original has a participant who has no account yet
- **THEN** the copy lists that username too

#### Scenario: Failure
- **WHEN** the server cannot be reached, or refuses the new group
- **THEN** the dialog says so, nothing is created and the list stays as it was

### Requirement: Deleting a group
Every group, whatever its state, SHALL offer "Delete" in the row of buttons under its details, after a confirmation, sent with `DELETE /admin/groups/{id}`. Deleting SHALL return to the list.

#### Scenario: Delete a Draft
- **WHEN** the administrator confirms "Delete" on a Draft
- **THEN** the group is gone and the list is shown, with no second dialog

#### Scenario: Delete in any state
- **WHEN** the group is Pending, Active or Archived
- **THEN** it offers "Delete" like a Draft does

#### Scenario: Second confirmation
- **WHEN** the administrator confirms "Delete" on an Active group that has wishlist items, questions and answers
- **THEN** a second dialog says how many of each will be deleted, with the draw, and that this cannot be undone, and the group is still there

#### Scenario: Confirmed twice
- **WHEN** the administrator confirms the second dialog
- **THEN** the group is deleted and the list is shown

#### Scenario: Cancelled at the second dialog
- **WHEN** the administrator cancels the second dialog
- **THEN** nothing is deleted and the group's page is shown as it was

#### Scenario: Pending without wishlist items
- **WHEN** the administrator confirms "Delete" on a Pending group that has no wishlist items
- **THEN** the group is deleted with one confirmation

#### Scenario: Group gone meanwhile
- **WHEN** the server answers that the group no longer exists while the administrator deletes it
- **THEN** the app says the group is no longer available

## ADDED Requirements

### Requirement: Who takes part depends on the state
For a Draft group who takes part SHALL be the participants as chips; for a Pending group the participants as rows; for an Active or Archived group the pair list of `client-admin-oversight`, which replaces the participants list. The chips of a Draft group SHALL wrap onto as many lines as needed and SHALL NOT be tappable.

#### Scenario: Chips wrap and do not respond
- **WHEN** a Draft group has many participants
- **THEN** the chips wrap onto several lines and tapping one does nothing

### Requirement: Pending participants open sheets
In a Pending group choosing a participant's username SHALL open that person's user sheet of `client-admin-oversight`, and each row SHALL end with a gift icon, named "Read wishlist" for assistive technology, that opens the participant's wishlist.

#### Scenario: Gift icon named for assistive technology
- **WHEN** a screen reader reads the rows of a Pending group
- **THEN** each gift icon is announced as "Read wishlist"

### Requirement: The group page scrolls as one
The whole page SHALL scroll as one: the details, the actions, the search field and the people move together, and no area of the page scrolls inside itself.

#### Scenario: Search field scrolls away
- **WHEN** the page of a group with many participants is scrolled down
- **THEN** the details, the actions and the search field scroll away with the people

### Requirement: Marks in the pair list
In the pair list of an Active or Archived group a username whose profile lacks a name or an address SHALL be followed by `*`, as described in `client-admin-oversight`.

#### Scenario: Pair list mark
- **WHEN** `bob` has no address and the group is Active
- **THEN** `bob` is followed by `*` in the pair list

### Requirement: Marks never block an action
The marks SHALL NOT block any action.

#### Scenario: Marks do not disable controls
- **WHEN** a participant is marked
- **THEN** no button or row of the group is disabled because of the mark

### Requirement: A duplicate's name is cut to 100 characters
A name that would exceed 100 characters with the prefix SHALL be cut, ending in an ellipsis, so that it is exactly 100 characters.

#### Scenario: Exactly 100
- **WHEN** the original's name is 98 characters long
- **THEN** the copy's name is exactly 100 characters and ends in an ellipsis

### Requirement: A duplicate copies nothing of the draw
Nothing of the original's draw, wishlists or questions SHALL be copied, and the original SHALL NOT change.

#### Scenario: Original untouched
- **WHEN** an Active group is duplicated
- **THEN** the copy has no draw, wishlists or questions and the original is unchanged

### Requirement: Deleting a group with content asks again
When the server answers 409 `confirmation_required` the app SHALL show a second dialog naming what would be deleted from the counts it returns (wishlist items, questions, answers and the draw) and saying that this cannot be undone, and SHALL send the request again with `confirm=true` only if that is confirmed.

#### Scenario: The dialog names the counts
- **WHEN** the server answers 409 with counts of 3 wishlist items and 2 questions
- **THEN** the second dialog names those counts and says that this cannot be undone
