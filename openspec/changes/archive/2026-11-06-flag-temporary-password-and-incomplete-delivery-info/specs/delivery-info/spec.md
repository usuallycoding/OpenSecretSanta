# Spec Delta

## MODIFIED Requirements

### Requirement: Delivery info belongs to a group
A participant SHALL have, for each group they take part in, delivery info: a full name, an email, a phone number, an address and delivery information, each initially empty. It SHALL belong to that group alone and SHALL NOT be shared with the person's other groups. The first four are required whenever it is saved, as `Edit own delivery info` says, and the delivery information is optional.

#### Scenario: Nothing set yet
- **WHEN** a person becomes a participant of a group
- **THEN** their delivery info for it has no full name, email, phone, address or delivery information

#### Scenario: One group at a time
- **WHEN** `alice` sets her address in group 1 and is also a participant of group 2
- **THEN** her delivery info for group 2 still has no address

#### Scenario: Gone with the group
- **WHEN** a group is deleted
- **THEN** the delivery info of its participants for it is gone

### Requirement: Edit own delivery info
The server SHALL expose `PUT /groups/{id}/delivery-info` letting a participant replace their full name, email, phone, address and delivery information for the group while it is Pending or Active. The full name, email, phone and address SHALL each be given; the delivery information MAY be cleared. The email SHALL be a syntactically valid address and each text field at most 500 characters. An Archived group SHALL NOT be changed.

#### Scenario: Updated
- **WHEN** `alice` submits new values in a Pending group
- **THEN** the response is 200 and a later `GET` of the route shows them

#### Scenario: Field cleared
- **WHEN** a participant submits a null or blank delivery information and the other four fields
- **THEN** the delivery information becomes empty and the response is 200

#### Scenario: Required field missing
- **WHEN** a participant submits a null or blank full name, email, phone or address
- **THEN** the response is 400 with code `validation_failed` naming that field and nothing changes

#### Scenario: Incomplete info stays readable
- **WHEN** a participant whose saved delivery info lacks a phone reads it with `GET /groups/{id}/delivery-info`
- **THEN** the response is 200 with what is saved, and a save succeeds only once all four required fields are given

#### Scenario: Invalid email
- **WHEN** a participant submits an email that is not a valid address
- **THEN** the response is 400 naming the `email` field and nothing changes

#### Scenario: Too long
- **WHEN** a participant submits an address of 501 characters
- **THEN** the response is 400 naming the `address` field and nothing changes

#### Scenario: Archived
- **WHEN** a participant edits their delivery info in an Archived group
- **THEN** the response is 409 with code `invalid_state` and nothing changes

#### Scenario: Only their own
- **WHEN** `alice` calls the route
- **THEN** only her own delivery info for that group changes
