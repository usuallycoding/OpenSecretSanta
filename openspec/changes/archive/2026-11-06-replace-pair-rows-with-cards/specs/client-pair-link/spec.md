# Spec Delta

## MODIFIED Requirements

### Requirement: Announced once
A screen reader SHALL announce a pair link as a single button with a label that says what it opens, and SHALL NOT announce its icons separately. The label SHALL be "<santa> gives to <receiver>" for a row of an administrator's pair list.

#### Scenario: One announcement
- **WHEN** a screen reader reaches a pair link
- **THEN** it announces one button, and not an arrow, a gift and an arrow

#### Scenario: Admin row
- **WHEN** a screen reader reaches the pair link in the row `alice -> bob` of an administrator's list
- **THEN** it announces one button, "alice gives to bob"

### Requirement: Where the pair link is used
The pair link SHALL be in every row of an administrator's pair list. It SHALL NOT be used elsewhere: the participant's group page has no pair link, and the gift icon beside each participant in an administrator's list SHALL stay as it is.

#### Scenario: Participant rows
- **WHEN** a group is Active or Archived
- **THEN** the person's group page shows no pair link

#### Scenario: Administrator's list
- **WHEN** an administrator opens an Active group with three pairs
- **THEN** each of the three rows shows a pair link between the Santa and the receiver

#### Scenario: Lone gift icons
- **WHEN** an administrator reads a Pending group's participants
- **THEN** the gift icons shown there are single icons, not pair links
