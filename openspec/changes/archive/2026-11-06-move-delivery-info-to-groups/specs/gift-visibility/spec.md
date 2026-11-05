# Spec Delta

## MODIFIED Requirements

### Requirement: Receiver details
The server SHALL expose `GET /groups/{id}/receiver` to a participant of an Active or Archived group, returning the username, full name, email address, address, phone number and delivery information of that participant's own receiver from the receiver's delivery info for that group, as currently stored. Fields the receiver has not set SHALL be empty, and no other account data SHALL be returned.

#### Scenario: Santa reads their receiver
- **WHEN** `alice`, whose receiver is `bob`, requests `/groups/{id}/receiver` for an Active group
- **THEN** the response contains `bob`'s username, full name, email address, address, phone number and delivery information

#### Scenario: Live values
- **WHEN** `bob` changes his address or email in his delivery info after the group became Active and `alice` requests his details again
- **THEN** the response shows the new values

#### Scenario: Unset fields
- **WHEN** the receiver has no phone number or no email address
- **THEN** that field is empty in the response

#### Scenario: Email is shared when set
- **WHEN** the receiver has set an email address
- **THEN** the response contains it, with no need for the receiver to opt in

#### Scenario: No email or credentials
- **WHEN** the receiver's details are returned
- **THEN** they contain no password data or admin flag, and the email address appears only as the receiver's own contact detail

#### Scenario: Still readable after archive
- **WHEN** the group has been Archived
- **THEN** a participant can still read their receiver's details

### Requirement: Santa revealed on archive
The server SHALL expose `GET /groups/{id}/santa`, returning the username and the full name, from the Santa's delivery info for the group, of the participant's own Santa, and only when the group is Archived. It SHALL return nothing else about the Santa.

#### Scenario: After archive
- **WHEN** the group is Archived and `bob`, whose Santa is `alice`, requests `/groups/{id}/santa`
- **THEN** the response contains `alice`'s username and full name and no address, phone number or delivery information

#### Scenario: Before archive
- **WHEN** the group is Active and `bob` requests `/groups/{id}/santa`
- **THEN** the response is 409 with code `invalid_state` and the body does not identify the Santa

