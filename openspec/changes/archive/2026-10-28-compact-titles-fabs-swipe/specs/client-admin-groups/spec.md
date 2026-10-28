# Spec Delta

## MODIFIED Requirements

### Requirement: Admin groups list
The admin view SHALL list the signed-in administrator's own groups, loaded with `GET /admin/groups`, each with its name on one row (with an ellipsis when it is longer, not opening anything by itself), state, date and time, location and number of participants, in the order the server returns them, with a "New group" floating button. When there are none it SHALL say so and offer to create one. A group owned by another administrator SHALL never appear.

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

### Requirement: Group detail shows who takes part
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its name on one row with an ellipsis when it is longer (choosing a name that was cut opens a dialog with the whole name), its details, its state with a sentence on what that state means for the owner, the actions of the group, and at the end who takes part. For a Draft or Pending group that is the participants list; for an Active or Archived group it is the pair list of `client-admin-oversight`, which replaces the participants list. In a Pending group each participant SHALL offer "Read wishlist". The people SHALL be shown in a box at the end of the page that scrolls inside itself and fills all the height left below the details and the actions, with a minimum height; when the details and actions leave less room than that, they SHALL scroll above it.

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

#### Scenario: A long name
- **WHEN** the group's name is longer than one row and the administrator chooses it
- **THEN** a dialog shows the whole name and can be closed
