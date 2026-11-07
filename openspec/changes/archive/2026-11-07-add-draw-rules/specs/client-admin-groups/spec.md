## ADDED Requirements

### Requirement: The rules button
The administrator's group page SHALL show a gavel icon button in the row of the group's actions, in Pending, Active and Archived, and not in Draft. It SHALL be named "Draw rules" for a screen reader, and SHALL open the rules page.

#### Scenario: Pending
- **WHEN** the admin views a Pending group
- **THEN** the row shows the gavel button, enabled

#### Scenario: Active and Archived
- **WHEN** the admin views an Active or an Archived group
- **THEN** the row shows the gavel button, and the rules it opens are read-only

#### Scenario: Draft
- **WHEN** the admin views a Draft group
- **THEN** the row shows no gavel button
