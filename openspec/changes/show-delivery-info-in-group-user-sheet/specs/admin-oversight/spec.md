# Spec Delta

## ADDED Requirements

### Requirement: Owner reads a participant's delivery info
The server SHALL expose `GET /admin/groups/{id}/participants/{username}/delivery-info`, usable only by the group's owner while the group is Pending, Active or Archived, returning the full name, email, phone, address and delivery information that participant has set for that group, each empty when not set. It SHALL only read, and nothing about the read SHALL be visible to the participant or recorded.

#### Scenario: Owner reads delivery info
- **WHEN** the owner requests the delivery info of participant `bob`, who set all five fields in an Active group
- **THEN** the response has `bob`'s full name, email, phone, address and delivery information for that group

#### Scenario: Nothing set
- **WHEN** the participant has set nothing for the group
- **THEN** the response is 200 with every field empty

#### Scenario: Pending and Archived
- **WHEN** the owner requests it while the group is Pending, and again once it is Archived
- **THEN** each response has what the participant has set at that time

#### Scenario: Only for this group
- **WHEN** `bob` is in two groups owned by the same admin and set an address in only one of them
- **THEN** the response for the other group has no address

#### Scenario: Draft group
- **WHEN** the owner requests it in a Draft group
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Not a participant
- **WHEN** the owner requests it for a username that is not a participant of the group
- **THEN** the response is 404

#### Scenario: Not the owner
- **WHEN** an admin who does not own the group requests it, or a participant who is not an admin, or no one signed in
- **THEN** the responses are 404, 403 and 401, as for the other oversight endpoints

#### Scenario: Participant sees no change
- **WHEN** the owner reads a participant's delivery info repeatedly
- **THEN** that participant's own delivery info, group view and every other response they get are byte-for-byte unchanged

#### Scenario: No write endpoint
- **WHEN** an owner looks for a way to change a participant's delivery info through admin endpoints
- **THEN** none exists
