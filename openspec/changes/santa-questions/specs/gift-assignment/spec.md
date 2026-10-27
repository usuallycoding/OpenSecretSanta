# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Participants cannot see assignments yet`
- TO: `### Requirement: Participants see only their own receiver`

## MODIFIED Requirements

### Requirement: Participants see only their own receiver
No endpoint SHALL reveal to a participant any assignment other than their own receiver, and none SHALL reveal who their Santa is before the group is Archived. The participant view of a group SHALL contain no assignment data.

#### Scenario: Participant view has no assignment data
- **WHEN** a participant views an Active group they belong to
- **THEN** the response contains no information about any assignment

#### Scenario: Own receiver only
- **WHEN** a participant asks for their receiver
- **THEN** they learn who that one person is and nothing about any other pair

#### Scenario: Santa hidden until archive
- **WHEN** the group is Active
- **THEN** no response to the receiver identifies their Santa
