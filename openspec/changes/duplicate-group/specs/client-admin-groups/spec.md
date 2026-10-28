## MODIFIED Requirements

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

## ADDED Requirements

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
