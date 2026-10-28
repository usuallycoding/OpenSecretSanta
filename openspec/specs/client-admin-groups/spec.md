# client-admin-groups Specification

## Purpose

TBD - created by archiving change admin-groups. Update Purpose after archive.

## Requirements

### Requirement: Admin groups list
The admin view SHALL list the signed-in administrator's own groups, loaded with `GET /admin/groups`, each with its name on one row (with an ellipsis when it is longer, not opening anything by itself), state, date and time, location and number of participants, in the order the server returns them, with a "New group" floating button and, on every group, a "Duplicate" button. When there are none it SHALL say so and offer to create one. A group owned by another administrator SHALL never appear.

#### Scenario: Groups shown
- **WHEN** the administrator owns two groups
- **THEN** both are listed with their name, state and number of participants

#### Scenario: No groups yet
- **WHEN** the administrator owns no group
- **THEN** the screen says so and offers "New group"

#### Scenario: Open a group
- **WHEN** the administrator chooses a group
- **THEN** its detail opens, and going back returns to the list

#### Scenario: New group button
- **WHEN** the administrator is on the groups list
- **THEN** a floating button with a plus icon opens the form for a new group

#### Scenario: Duplicate button on every group
- **WHEN** the list shows a Draft, a Pending, an Active and an Archived group
- **THEN** each has a "Duplicate" button, and choosing it does not open the group

### Requirement: Creating a group
"New group" SHALL open a form with name, description, location, date and time. A successful `POST /admin/groups` SHALL open the new group, which is a Draft. A field the server refuses SHALL show its message next to the field.

#### Scenario: Create a group
- **WHEN** the administrator enters a name and saves
- **THEN** the group is created as a Draft and its detail opens

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while saving
- **THEN** the form keeps what was typed and says so

### Requirement: The group form checks what the server checks
Before anything is sent the form SHALL check every field exactly as the server does: a name of 1 to 100 characters, a description of up to 2000, a location of up to 200, a real calendar date and a real time of day, with blanks taken as empty. Date SHALL be typed as `2026-12-24` and time as `18:30`, each with a hint, and a failed check SHALL show its message next to the field.

#### Scenario: Name required
- **WHEN** the administrator saves with a blank name
- **THEN** nothing is sent and the form says the name is required

#### Scenario: Invalid date or time
- **WHEN** the administrator enters `2026-13-45` as the date or `25:99` as the time
- **THEN** nothing is sent and the form says what format is expected next to that field

#### Scenario: Blank means empty
- **WHEN** the administrator leaves description, location, date and time blank
- **THEN** the group is saved without them

### Requirement: Editing a group's details
While a group is Draft, Pending or Active the administrator SHALL be able to open the same form filled with its details and save changes with `PUT /admin/groups/{id}`, after which the detail shows the new values. An Archived group SHALL show no edit action.

#### Scenario: Edit the location
- **WHEN** the administrator changes the location of an Active group and saves
- **THEN** the detail shows the new location

#### Scenario: Clear a field
- **WHEN** the administrator empties the description and saves
- **THEN** the group has no description

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** there is no way to edit its details

### Requirement: Group detail shows who takes part
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its name on one row with an ellipsis when it is longer (choosing a name that was cut opens a dialog with the whole name), its details, its state with a sentence on what that state means for the owner, the actions of the group, and at the end who takes part. For a Draft group that is the participants as chips; for a Pending group it is the participants as rows; for an Active or Archived group it is the pair list of `client-admin-oversight`, which replaces the participants list. In a Pending group choosing a participant's username SHALL open that person's user sheet of `client-admin-oversight`, and each row SHALL end with a gift icon, named "Read wishlist" for assistive technology, that opens the participant's wishlist. The people SHALL be shown in a box at the end of the page that scrolls inside itself and fills all the height left below the details and the actions, with a minimum height; when the details and actions leave less room than that, they SHALL scroll above it. The chips of a Draft group SHALL wrap onto as many lines as needed and SHALL NOT be tappable.

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
- **THEN** the participants are in a box that fills the rest of the page and scrolls inside itself, and the buttons above it stay in view

#### Scenario: A long name
- **WHEN** the group's name is longer than one row and the administrator chooses it
- **THEN** a dialog shows the whole name and can be closed

#### Scenario: Participant without an account in a Pending group
- **WHEN** a Pending group has a participant who has no account
- **THEN** their row shows the username and `*`, and choosing the username or the gift icon does nothing

### Requirement: Marks on participants
In the participants list of a Draft or Pending group a participant without an account, and a participant whose profile lacks a name or an address, SHALL each be followed by a single `*`, the same mark for both, and the list SHALL end with the legend "* no account or profile incomplete", shown only when at least one participant has the mark. In the pair list of an Active or Archived group a username whose profile lacks a name or an address SHALL be followed by `*`, as described in `client-admin-oversight`. The marks SHALL NOT block any action.

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

### Requirement: Editing participants
While a group is Draft or Pending the administrator SHALL be able to edit the participants in a bottom sheet: add a username, remove a username, then save with `PUT /admin/groups/{id}/participants`, which replaces the whole list. A username is checked before sending as the server checks it (3 to 32 characters of letters, digits, `.`, `_` or `-`, case ignored) and a name already in the list is not added twice. Once the group is Active or Archived the participants SHALL be shown read-only.

#### Scenario: Add a participant
- **WHEN** the administrator adds `dave` in a Draft group and saves
- **THEN** `dave` appears among the participants

#### Scenario: Remove a participant
- **WHEN** the administrator removes `bob` and saves
- **THEN** `bob` is no longer listed

#### Scenario: Invalid or repeated username
- **WHEN** the administrator adds `x` or `Alice` while `alice` is already listed
- **THEN** the sheet says what is wrong and the list is unchanged

#### Scenario: Roster frozen
- **WHEN** the group is Active or Archived
- **THEN** there is no way to add or remove a participant

#### Scenario: Roster frozen elsewhere
- **WHEN** the server refuses a save because the roster is frozen
- **THEN** the sheet closes, the detail reloads and says the participants can no longer change

### Requirement: Moving a group forward
The detail SHALL offer the next step for the state: "Make pending" for a Draft (`POST /admin/groups/{id}/pending`), "Draw the names" for a Pending group (`POST /admin/groups/{id}/activate`) and "Archive" for an Active one (`POST /admin/groups/{id}/archive`), each after a confirmation that says what it does. The step SHALL be a button in the same row as "Edit details" and "Delete", above the people; the row SHALL wrap onto a second line when it does not fit. An Archived group has no step.

#### Scenario: Make pending
- **WHEN** the administrator confirms "Make pending" on a Draft
- **THEN** the group shows as Pending

#### Scenario: Draw the names
- **WHEN** the administrator confirms "Draw the names" on a Pending group with 4 or more participants
- **THEN** the group shows as Active and its participants are frozen

#### Scenario: Archive
- **WHEN** the administrator confirms "Archive" on an Active group
- **THEN** the group shows as Archived and no action remains except reading and deleting

#### Scenario: Cancel a confirmation
- **WHEN** the administrator cancels any confirmation
- **THEN** nothing is sent

#### Scenario: Changed behind the screen
- **WHEN** the server refuses a step because the group is no longer in the state the screen showed
- **THEN** the detail reloads and says what happened

#### Scenario: Buttons together
- **WHEN** the detail of a Pending group is shown
- **THEN** "Edit details", "Draw the names" and "Delete" are in one row above the participants

### Requirement: Confirmations say what a step does
The confirmation for "Make pending" SHALL say that accounts are created for people who have none, the one for "Draw the names" that the names are drawn and the participants frozen, and the one for "Archive" that the exchange is finished and becomes read-only.

#### Scenario: Wording
- **WHEN** the administrator chooses "Draw the names"
- **THEN** the confirmation says the names will be drawn and the participants can no longer change

### Requirement: Drawing the names needs four participants
"Draw the names" SHALL be unavailable while a Pending group has fewer than 4 participants and SHALL say that at least 4 are needed, instead of sending a request the server would refuse.

#### Scenario: Too few participants
- **WHEN** a Pending group has 3 participants
- **THEN** "Draw the names" is unavailable and says at least 4 are needed

### Requirement: Temporary passwords are shown once
When making a group Pending, or saving a Pending group's participants, creates accounts, the app SHALL show each created username with its temporary password in a dialog, selectable so that it can be copied, and SHALL say that each person must change it when first signing in and that an administrator can look it up under Users until then. The dialog SHALL be shown only when accounts were created. Once it is closed the app SHALL keep none of the passwords.

#### Scenario: Accounts created
- **WHEN** making a group Pending creates accounts for `bob` and `carol`
- **THEN** a dialog lists `bob` and `carol` with their temporary passwords

#### Scenario: No accounts created
- **WHEN** every participant already has an account
- **THEN** no dialog is shown

#### Scenario: Closing the dialog
- **WHEN** the dialog is closed
- **THEN** the passwords are no longer anywhere in the app

### Requirement: Loading, errors and refreshing
Every admin group screen SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached or answers with an error, and SHALL keep what it already shows while a refresh fails. Each screen SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and SHALL reload when the app returns to the foreground.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while the list loads
- **THEN** the screen says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while the list is shown
- **THEN** the list stays on screen with the error message

### Requirement: Duplicating a group
Choosing "Duplicate" on a group SHALL ask for confirmation, naming the group. Confirming SHALL create a Draft with `POST /admin/groups` having the original's description, location, date, time and participants and the name "Copy of " followed by the original's name, and SHALL open the new group. A name that would exceed 100 characters with the prefix SHALL be cut, ending in an ellipsis, so that it is exactly 100 characters. Nothing of the original's draw, wishlists or questions SHALL be copied, and the original SHALL NOT change.

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
Every group, whatever its state, SHALL offer "Delete" in the row of buttons under its details, after a confirmation, sent with `DELETE /admin/groups/{id}`. When the server answers 409 `confirmation_required` the app SHALL show a second dialog naming what would be deleted from the counts it returns (wishlist items, questions, answers and the draw) and saying that this cannot be undone, and SHALL send the request again with `confirm=true` only if that is confirmed. Deleting SHALL return to the list.

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
