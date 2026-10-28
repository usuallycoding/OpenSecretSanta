# Spec Delta

## MODIFIED Requirements

### Requirement: Group detail shows who takes part
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its details, its state with a sentence on what that state means for the owner, the actions of the group, and at the end who takes part. For a Draft or Pending group that is the participants list; for an Active or Archived group it is the pair list of `client-admin-oversight`, which replaces the participants list. In a Pending group each participant SHALL offer "Read wishlist". The people SHALL be shown in a box at the end of the page that scrolls inside itself and fills all the height left below the details and the actions, with a minimum height; when the details and actions leave less room than that, they SHALL scroll above it.

#### Scenario: Group not available
- **WHEN** the server answers that the group does not exist for this administrator
- **THEN** the screen says the group is no longer available and offers to go back

#### Scenario: Pending group
- **WHEN** a Pending group's detail is shown
- **THEN** it lists the participants and each offers "Read wishlist"

#### Scenario: Draft offers no oversight
- **WHEN** the group is Draft
- **THEN** it lists the participants with no "Read wishlist" and no pair list

#### Scenario: Drawn group
- **WHEN** the group is Active or Archived
- **THEN** the detail shows the pair list and no separate participants list, and no Questions button

#### Scenario: Many participants
- **WHEN** a Pending group has 13 participants
- **THEN** the participants are in a box that fills the rest of the page and scrolls inside itself, and the buttons above it stay in view

### Requirement: Moving a group forward
The detail SHALL offer the next step for the state: "Make pending" for a Draft (`POST /admin/groups/{id}/pending`), "Draw the names" for a Pending group (`POST /admin/groups/{id}/activate`) and "Archive" for an Active one (`POST /admin/groups/{id}/archive`), each after a confirmation that says what it does. The step SHALL be a button in the same row as "Edit details" (and, for a Draft, "Delete"), above the people; the row SHALL wrap onto a second line when it does not fit. An Archived group has no step.

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

#### Scenario: Buttons together
- **WHEN** the detail of a Pending group is shown
- **THEN** "Edit details" and "Draw the names" are in one row above the participants

### Requirement: Deleting a Draft
A Draft SHALL also offer "Delete", after a confirmation, with `DELETE /admin/groups/{id}`, in the same row of buttons as "Edit details" and "Make pending", and deleting SHALL return to the list. A group that is not a Draft SHALL offer no delete.

#### Scenario: Delete a Draft
- **WHEN** the administrator confirms "Delete" on a Draft
- **THEN** the group is gone and the list is shown

#### Scenario: Not a Draft
- **WHEN** the group is Pending, Active or Archived
- **THEN** there is no delete action
