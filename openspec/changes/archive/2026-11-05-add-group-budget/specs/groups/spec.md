# Spec Delta

## MODIFIED Requirements

### Requirement: Group record
A group SHALL have an owner, a name, a description, a location, a budget with its currency, a date, a time, a list of participant usernames and a state. Only the name is required; description, location, budget, date and time MAY be empty. The date and time are local values without a timezone.

#### Scenario: Minimal group
- **WHEN** an admin creates a group with only a name
- **THEN** the group exists in state Draft, owned by that admin, with no participants and empty description, location, budget, date and time

#### Scenario: Invalid date or time
- **WHEN** an admin submits a date that is not a real calendar date or a time that is not a valid time of day
- **THEN** the response is 400 and nothing is saved

#### Scenario: Name required
- **WHEN** an admin submits a blank name, or one longer than 100 characters
- **THEN** the response is 400 naming the `name` field

### Requirement: Edit group details
The owner SHALL be able to change a group's name, description, location, budget, currency, date and time through `PUT /admin/groups/{id}` while the group is Draft, Pending or Active. An Archived group SHALL NOT be editable.

#### Scenario: Edit in Active
- **WHEN** the owner changes the location of an Active group
- **THEN** the response is 200 and the new location is stored

#### Scenario: Archived is read-only
- **WHEN** the owner edits an Archived group
- **THEN** the response is 409 with code `invalid_state` and nothing changes

#### Scenario: Edit the budget
- **WHEN** the owner changes the budget of a Pending group to 200
- **THEN** the response is 200 and the new budget is stored

### Requirement: Participants see their groups
The server SHALL expose `GET /groups` and `GET /groups/{id}` to any authenticated user, returning the name, description, location, budget, currency, date, time, state and participant usernames of the non-Draft groups in which that user participates, and nothing else about the group or the other participants.

#### Scenario: Member sees group
- **WHEN** `alice`, a participant of a Pending group, requests the group
- **THEN** she receives its details and the participants' usernames only

#### Scenario: Draft is invisible
- **WHEN** `alice` is named in a Draft group
- **THEN** that group is not listed for her and requesting it returns 404

#### Scenario: Not a participant
- **WHEN** a user requests a group they do not participate in
- **THEN** the response is 404

#### Scenario: Password change first
- **WHEN** a user who must still change a temporary password requests their groups
- **THEN** the response is 403 with code `password_change_required`

#### Scenario: Budget shown to a member
- **WHEN** `alice`, a participant of a Pending group with the budget 100 and the currency `EUR`, requests the group
- **THEN** she receives the budget 100 and the currency `EUR`

## ADDED Requirements

### Requirement: Group budget
A group MAY have a budget: a whole number from 50 to 10000, with a currency of at most 20 characters, taken without the spaces around it. A budget given with a blank currency SHALL be stored with the currency `RON`. A group without a budget SHALL have no currency, whatever was sent.

#### Scenario: Budget with a currency
- **WHEN** an admin saves a group with the budget 100 and the currency `EUR`
- **THEN** the group has the budget 100 and the currency `EUR`

#### Scenario: Currency left blank
- **WHEN** an admin saves a group with the budget 100 and no currency
- **THEN** the group has the budget 100 and the currency `RON`

#### Scenario: No budget
- **WHEN** an admin saves a group with no budget but the currency `EUR`
- **THEN** the group has no budget and no currency

#### Scenario: Budget out of range
- **WHEN** an admin submits a budget of 49 or of 10001
- **THEN** the response is 400 naming the `budget` field and nothing is saved

#### Scenario: Currency too long
- **WHEN** an admin submits a currency of 21 characters with a budget
- **THEN** the response is 400 naming the `currency` field and nothing is saved

#### Scenario: Existing groups
- **WHEN** a group was created before budgets existed
- **THEN** it has no budget and no currency

