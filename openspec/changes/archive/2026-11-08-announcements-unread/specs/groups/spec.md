# Spec Delta

## MODIFIED Requirements

### Requirement: Participants see their groups
The server SHALL expose `GET /groups` and `GET /groups/{id}` to any authenticated user, returning the name, description, location, budget, currency, date, time, state and participant usernames of the non-Draft groups in which that user participates, and the number of the group's announcements that user has not seen, and nothing else about the group or the other participants.

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

#### Scenario: New announcements counted
- **WHEN** the owner has sent three announcements to two groups of `alice`, two to one and one to the other, and she has seen none
- **THEN** her list has the number 2 for the first group and 1 for the other, and requesting either group gives its own number

#### Scenario: Seen announcements are not counted
- **WHEN** `alice` has marked the time of the newest announcement of a group as seen
- **THEN** the group's number is 0 for her and unchanged for `bob`, who has not

#### Scenario: A group never announced to
- **WHEN** `alice` requests a group to which the owner has never sent an announcement
- **THEN** the number is 0
