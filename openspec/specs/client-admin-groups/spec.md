# client-admin-groups Specification

## Purpose

TBD - created by archiving change admin-groups. Update Purpose after archive.

## Requirements

### Requirement: Admin groups list
The admin view SHALL list the signed-in administrator's own groups, loaded with `GET /admin/groups`, each with its name, state, date and time, location and number of participants, in the order the server returns them, with a "New group" action. When there are none it SHALL say so and offer to create one. A group owned by another administrator SHALL never appear.

#### Scenario: Groups shown
- **WHEN** the administrator owns two groups
- **THEN** both are listed with their name, state and number of participants

#### Scenario: No groups yet
- **WHEN** the administrator owns no group
- **THEN** the screen says so and offers "New group"

#### Scenario: Open a group
- **WHEN** the administrator chooses a group
- **THEN** its detail opens, and going back returns to the list

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

### Requirement: Admin group detail
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its details, its state with a sentence on what that state means for the owner, and its participants. Where the state allows, the detail SHALL also offer the owner's oversight: choosing a participant opens their wishlist (Pending, Active and Archived), and an Active or Archived group shows the draw and a way to the Questions screen.

#### Scenario: Group not available
- **WHEN** the server answers that the group does not exist for this administrator
- **THEN** the screen says the group is no longer available and offers to go back

#### Scenario: Participant wishlist offered
- **WHEN** a Pending, Active or Archived group's detail is shown
- **THEN** choosing a participant opens that person's wishlist

#### Scenario: Draft offers no oversight
- **WHEN** the group is Draft
- **THEN** choosing a participant does not open a wishlist and there is no draw or Questions entry

#### Scenario: Questions offered
- **WHEN** the group is Active or Archived
- **THEN** the detail offers the Questions screen

### Requirement: Marks on participants
In the detail a participant without an account SHALL be marked "no account yet" and a participant whose profile lacks a name or an address SHALL be marked "profile incomplete". The marks SHALL NOT block any action.

#### Scenario: Participants with marks
- **WHEN** a Draft group has `alice` with a complete profile and `newcomer` without an account
- **THEN** `alice` has no mark and `newcomer` is marked "no account yet"

#### Scenario: Incomplete profile
- **WHEN** a participant's account has no address
- **THEN** that participant is marked "profile incomplete"

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
The detail SHALL offer the next step for the state: "Make pending" for a Draft (`POST /admin/groups/{id}/pending`), "Draw the names" for a Pending group (`POST /admin/groups/{id}/activate`) and "Archive" for an Active one (`POST /admin/groups/{id}/archive`), each after a confirmation that says what it does. An Archived group has no step.

#### Scenario: Make pending
- **WHEN** the administrator confirms "Make pending" on a Draft
- **THEN** the group shows as Pending

#### Scenario: Draw the names
- **WHEN** the administrator confirms "Draw the names" on a Pending group with 4 or more participants
- **THEN** the group shows as Active and its participants are frozen

#### Scenario: Archive
- **WHEN** the administrator confirms "Archive" on an Active group
- **THEN** the group shows as Archived and no action remains except reading

#### Scenario: Cancel a confirmation
- **WHEN** the administrator cancels any confirmation
- **THEN** nothing is sent

#### Scenario: Changed behind the screen
- **WHEN** the server refuses a step because the group is no longer in the state the screen showed
- **THEN** the detail reloads and says what happened

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

### Requirement: Deleting a Draft
A Draft SHALL also offer "Delete", after a confirmation, with `DELETE /admin/groups/{id}`, and deleting SHALL return to the list. A group that is not a Draft SHALL offer no delete.

#### Scenario: Delete a Draft
- **WHEN** the administrator confirms "Delete" on a Draft
- **THEN** the group is gone and the list is shown

#### Scenario: Not a Draft
- **WHEN** the group is Pending, Active or Archived
- **THEN** there is no delete action

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
Every admin group screen SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached or answers with an error, and SHALL keep what it already shows while a refresh fails. Each screen SHALL offer Refresh and SHALL reload when the app returns to the foreground.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while the list loads
- **THEN** the screen says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while the list is shown
- **THEN** the list stays on screen with the error message
