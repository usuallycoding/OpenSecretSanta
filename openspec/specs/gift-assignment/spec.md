# gift-assignment Specification

## Purpose

Defines how Secret Santa givers and receivers are drawn when a group becomes active, the guarantees about the draw, and who can see the result in this stage.

## Requirements

### Requirement: Valid assignment
When a group is activated, the system SHALL assign every participant exactly one receiver and make every participant the receiver of exactly one other participant, and no participant SHALL be assigned themself.

#### Scenario: Everyone gives once and receives once
- **WHEN** a group of 5 participants is activated
- **THEN** each participant appears exactly once as Santa and exactly once as receiver

#### Scenario: Nobody draws themself
- **WHEN** a group of 3 participants is activated, repeatedly
- **THEN** no assignment ever has the same Santa and receiver

### Requirement: Single cycle
The assignments of a group SHALL form one closed chain through all participants, so that there are no pairs who only give to each other and no separate sub-groups.

#### Scenario: One chain
- **WHEN** the assignments of an activated group are followed from any participant
- **THEN** every participant is visited exactly once before returning to the start

### Requirement: Random draw
The assignment SHALL be drawn using a cryptographically secure source of randomness, so that it cannot be predicted from the order in which participants were listed or added.

#### Scenario: Different outcomes
- **WHEN** the same participants are activated in many separate groups
- **THEN** the resulting assignments vary between groups

### Requirement: Assignments are fixed
Once drawn, assignments SHALL NOT change for the life of the group, and no operation SHALL redraw them.

#### Scenario: No redraw
- **WHEN** the owner edits the group's details or archives it
- **THEN** every participant keeps the same receiver

### Requirement: Owner lists all assignments
The server SHALL expose `GET /admin/groups/{id}/assignments`, usable only by the group's owner, returning every Santa and receiver pair of an Active or Archived group.

#### Scenario: Owner sees all
- **WHEN** the owner requests the assignments of an Active group of 4
- **THEN** the response lists 4 pairs

#### Scenario: Before activation
- **WHEN** the owner requests the assignments of a Draft or Pending group
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Others cannot see
- **WHEN** another admin, or a participant who is not the owner, requests the assignments
- **THEN** the response is 404 for another admin's group and 403 for a user who is not an admin

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
