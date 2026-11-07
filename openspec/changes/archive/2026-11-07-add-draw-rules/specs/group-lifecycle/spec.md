## ADDED Requirements

### Requirement: Activation can be refused by the rules
Moving a Pending group to Active SHALL be refused with 409 and code `rules_conflict` when no draw satisfies the group's rules for its participants as they are at that moment. The group SHALL stay Pending, with its participants and rules unchanged, and no assignment SHALL be created.

#### Scenario: Rules that no longer fit
- **WHEN** the owner activates a Pending group whose rules cannot be satisfied by its current participants
- **THEN** the response is 409 with code `rules_conflict`, the group stays Pending and it has no assignments

#### Scenario: Rules that fit
- **WHEN** the owner activates a Pending group whose rules can be satisfied
- **THEN** the group becomes Active with assignments that satisfy them
