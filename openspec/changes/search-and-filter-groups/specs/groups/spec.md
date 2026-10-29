# Spec Delta

## ADDED Requirements

### Requirement: Group lists are newest first
`GET /admin/groups` and `GET /groups` SHALL return their groups newest first, by the time each group was created, and a group created later SHALL come first among groups created in the same second. Changing a group's details, its participants or its state SHALL NOT change its place.

#### Scenario: Administrator's list
- **WHEN** an administrator creates "First" and then "Second" and lists their groups
- **THEN** "Second" comes before "First"

#### Scenario: Participant's list
- **WHEN** `alice` takes part in a Pending group created earlier and an Active group created later
- **THEN** her list has the later group first

#### Scenario: A copy comes first
- **WHEN** an administrator duplicates an old group
- **THEN** the copy is the first group of their list

#### Scenario: Changes do not reorder
- **WHEN** the oldest group of a list is edited, moved to Pending and drawn
- **THEN** it is still the last group of the list

#### Scenario: Same second
- **WHEN** two groups are created within the same second
- **THEN** the one created last is returned first
