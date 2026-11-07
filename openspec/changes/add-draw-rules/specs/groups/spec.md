## ADDED Requirements

### Requirement: Rules stay with the group
A group's draw rules SHALL be kept with the group while it is Pending, Active or Archived, SHALL be listed in its deletion impact only through the group going away with them, and SHALL be deleted when the group is deleted.

#### Scenario: Deleting a group
- **WHEN** the owner deletes a group that has rules
- **THEN** the group and its rules are gone

#### Scenario: Other changes keep them
- **WHEN** the owner edits the group's details, activates it or archives it
- **THEN** its rules are unchanged
