# Spec Delta

## MODIFIED Requirements

### Requirement: Participants see only their own receiver
No participant endpoint SHALL reveal to a participant any assignment other than their own receiver, and none SHALL reveal who their Santa is before the group is Archived. The participant view of a group SHALL contain no assignment data. The group owner's admin endpoints (the assignments list and the admin-oversight endpoints) are the only exception and are usable by an owner who also participates.

#### Scenario: Participant view has no assignment data
- **WHEN** a participant views an Active group they belong to
- **THEN** the response contains no information about any assignment

#### Scenario: Own receiver only
- **WHEN** a participant asks for their receiver
- **THEN** they learn who that one person is and nothing about any other pair

#### Scenario: Santa hidden until archive
- **WHEN** the group is Active
- **THEN** no response to the receiver on a participant endpoint identifies their Santa

#### Scenario: Owner who participates
- **WHEN** the owner of an Active group also participates and requests `/admin/groups/{id}/assignments`
- **THEN** the response lists every pair, including their own
