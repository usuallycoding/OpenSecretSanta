## ADDED Requirements

### Requirement: Group actions are icon buttons
On the administrator's group page, "Edit details" SHALL be an outlined pencil icon button, "Send announcement" a bell, "Get passwords" the app's key icon and "Delete" a trash can, all outlined, the trash can with its outline and icon in the error colour. They SHALL come in that order, each only where its button is offered, and keep the rules of those buttons, including being unavailable while the page is busy. Each SHALL be spoken by its old label.

#### Scenario: Icons in order
- **WHEN** the page of an Active group is shown
- **THEN** the actions are, in order, a pencil, a bell, a key and a trash can, and the step comes after them

#### Scenario: Spoken names
- **WHEN** a screen reader reaches the four icon buttons
- **THEN** it says "Edit details", "Send announcement", "Get passwords" and "Delete"

#### Scenario: Only where offered
- **WHEN** the page of a Draft is shown
- **THEN** it has the pencil, the trash can and the step, and no bell or key

#### Scenario: Busy
- **WHEN** the page is busy with a step or with loading the passwords
- **THEN** the key and the trash can cannot be chosen

## MODIFIED Requirements

### Requirement: Moving a group forward
The detail SHALL offer the next step for the state: "Make pending" for a Draft (`POST /admin/groups/{id}/pending`), "Draw the names" for a Pending group (`POST /admin/groups/{id}/activate`) and "Archive" for an Active one (`POST /admin/groups/{id}/archive`), each after a confirmation that says what it does. The step SHALL be a button, last in the row of actions above the people, after the icon buttons; the row SHALL wrap onto a second line when it does not fit. An Archived group has no step.

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
- **THEN** the pencil, the bell, the key, the trash can and "Draw the names", in that order, are in one row above the participants
