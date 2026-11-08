# Spec Delta

## ADDED Requirements

### Requirement: The sender has seen what they announce
When an announcement is kept, its sender SHALL be marked as having seen it, if the sender takes part in the group, so that the sender's own announcements are never new to them. A sender who does not take part in the group SHALL have no mark.

#### Scenario: The owner is a participant
- **WHEN** the owner, who is also a participant, announces to the group
- **THEN** the announcement is not counted as new for the owner, and is for the other participants

#### Scenario: After a hand-over
- **WHEN** a group's owner hands the administrator role over and the new owner, a participant, announces
- **THEN** the announcement is new to the previous owner, if they take part, and not to the new one

#### Scenario: The owner is not a participant
- **WHEN** the owner, who does not take part in the group, announces
- **THEN** no mark is kept for the owner

#### Scenario: A refused announcement
- **WHEN** an announcement is refused
- **THEN** the sender's mark is as it was
