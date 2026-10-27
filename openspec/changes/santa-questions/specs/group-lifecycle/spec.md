# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Pending to Active needs three participants`
- TO: `### Requirement: Pending to Active needs four participants`

## MODIFIED Requirements

### Requirement: Pending to Active needs four participants
Moving a Pending group to Active SHALL be refused unless it has at least 4 participants, because with fewer a participant could work out who their Santa is. Incomplete participant profiles SHALL NOT prevent activation.

#### Scenario: Too few participants
- **WHEN** the owner activates a Pending group with 3 participants
- **THEN** the response is 400 naming `participants` and the group stays Pending

#### Scenario: Exactly four
- **WHEN** the owner activates a Pending group with 4 participants
- **THEN** the group becomes Active

#### Scenario: Incomplete profiles
- **WHEN** the owner activates a group with 4 participants of whom one has no address
- **THEN** the group becomes Active

#### Scenario: Flags in the response
- **WHEN** a group is activated
- **THEN** the response shows each participant's profile-complete flag
