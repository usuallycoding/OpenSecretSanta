## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Deleting a Draft
**Reason**: Any group can now be deleted; the requirement is replaced by "Deleting a group".
**Migration**: Use "Deleting a group".

## ADDED Requirements

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
