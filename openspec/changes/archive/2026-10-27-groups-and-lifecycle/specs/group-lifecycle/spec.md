# Spec Delta

## Purpose

Defines the states a Secret Santa group moves through, who moves it, what must hold for each move, and what the system does on the way so that no move leaves a group half-changed.

## ADDED Requirements

### Requirement: Group states and order
A group SHALL always be in exactly one of the states Draft, Pending, Active or Archived, and SHALL only ever move forward one step at a time in that order. No move backwards or sideways, and no skipping a state, SHALL be possible.

#### Scenario: Forward only
- **WHEN** the owner tries to move a Pending group to Draft, a Draft group to Active, or an Active group to Pending
- **THEN** the response is 409 with code `invalid_state` and the state is unchanged

#### Scenario: Repeating a move
- **WHEN** the owner moves a group to Pending a second time
- **THEN** the response is 409 with code `invalid_state`

### Requirement: Only the owner changes state
The server SHALL expose `POST /admin/groups/{id}/pending`, `/activate` and `/archive`, usable only by the group's owner; anyone else SHALL be answered as if the group did not exist.

#### Scenario: Other admin
- **WHEN** admin B calls `/activate` on admin A's group
- **THEN** the response is 404 and the group is unchanged

### Requirement: Draft to Pending creates accounts
Moving a Draft group to Pending SHALL create an account with a generated temporary password for every participant username that has none, leave existing accounts untouched, and return the newly created usernames with their temporary passwords.

#### Scenario: Mixed participants
- **WHEN** the owner moves a Draft group with participants `alice` (has an account) and `bob` (none) to Pending
- **THEN** `bob` gets an account that must change its temporary password, `alice` is unchanged, and the response lists `bob` with his temporary password only

#### Scenario: Participants gain visibility
- **WHEN** the group is Pending
- **THEN** its participants can see it, and the owner can view and reset the participants' accounts

#### Scenario: Empty group
- **WHEN** the owner moves a Draft group with no participants to Pending
- **THEN** the group becomes Pending and no account is created

### Requirement: Pending to Active needs three participants
Moving a Pending group to Active SHALL be refused unless it has at least 3 participants. Incomplete participant profiles SHALL NOT prevent activation.

#### Scenario: Too few participants
- **WHEN** the owner activates a Pending group with 2 participants
- **THEN** the response is 400 naming `participants` and the group stays Pending

#### Scenario: Incomplete profiles
- **WHEN** the owner activates a group with 3 participants of whom one has no address
- **THEN** the group becomes Active

#### Scenario: Flags in the response
- **WHEN** a group is activated
- **THEN** the response shows each participant's profile-complete flag

### Requirement: Activation draws assignments
Moving a group to Active SHALL create the assignments for all its participants as part of the same step, so that an Active group always has a complete set of assignments.

#### Scenario: Assignments exist after activation
- **WHEN** a Pending group with 4 participants becomes Active
- **THEN** exactly 4 assignments exist for that group

#### Scenario: None before activation
- **WHEN** a group is Draft or Pending
- **THEN** it has no assignments

### Requirement: Archive
Moving an Active group to Archived SHALL keep its participants and assignments unchanged and make the group's details read-only.

#### Scenario: Archive
- **WHEN** the owner archives an Active group
- **THEN** the group is Archived, and its assignments are still listed to the owner

### Requirement: Transitions are all-or-nothing
Each state change, with its account creation or assignment drawing, SHALL either complete entirely or leave the group, the accounts and the assignments exactly as they were. Concurrent attempts to make the same move SHALL succeed at most once.

#### Scenario: Failure leaves no trace
- **WHEN** a failure occurs while moving a group to Pending or Active
- **THEN** the group keeps its previous state and no account or assignment from that attempt exists

#### Scenario: Double activation
- **WHEN** two activation requests for the same Pending group arrive at the same time
- **THEN** one succeeds, the other gets 409 with code `invalid_state`, and the group has exactly one set of assignments
