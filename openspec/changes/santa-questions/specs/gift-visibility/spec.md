# Spec Delta

## Purpose

Defines what a participant of an active Secret Santa group may learn about the exchange: the details of the person they give a gift to, the reveal of who gave them a gift once the group is archived, and the guarantee that a Santa stays hidden until then.

## ADDED Requirements

### Requirement: Receiver details
The server SHALL expose `GET /groups/{id}/receiver` to a participant of an Active or Archived group, returning the username, full name, address, phone number and delivery information of that participant's own receiver as currently stored. Fields the receiver has not set SHALL be empty, and no other account data SHALL be returned.

#### Scenario: Santa reads their receiver
- **WHEN** `alice`, whose receiver is `bob`, requests `/groups/{id}/receiver` for an Active group
- **THEN** the response contains `bob`'s username, full name, address, phone number and delivery information

#### Scenario: Live values
- **WHEN** `bob` changes his address after the group became Active and `alice` requests his details again
- **THEN** the response shows the new address

#### Scenario: Unset fields
- **WHEN** the receiver has no phone number
- **THEN** the phone number is empty in the response

#### Scenario: No email or credentials
- **WHEN** the receiver's details are returned
- **THEN** they contain no email address, password data or admin flag

#### Scenario: Still readable after archive
- **WHEN** the group has been Archived
- **THEN** a participant can still read their receiver's details

### Requirement: No receiver before activation
Before the group is Active there are no assignments, so receiver details SHALL NOT be available to anyone through this endpoint.

#### Scenario: Pending group
- **WHEN** a participant requests `/groups/{id}/receiver` for a Pending group
- **THEN** the response is 409 with code `invalid_state`

### Requirement: Participants only
Only participants of the group SHALL be able to use the receiver, Santa and question endpoints. Anyone else, including admins who are not participants, SHALL be answered as if the group did not exist. A user who must still change a temporary password SHALL be refused with `password_change_required`.

#### Scenario: Not a participant
- **WHEN** a user, or an admin who owns the group, who does not participate requests `/groups/{id}/receiver`
- **THEN** the response is 404

#### Scenario: Draft or unknown group
- **WHEN** a participant requests the receiver of a Draft group or of a group that does not exist
- **THEN** the response is 404

### Requirement: Santa revealed on archive
The server SHALL expose `GET /groups/{id}/santa`, returning the username and full name of the participant's own Santa, and only when the group is Archived. It SHALL return nothing else about the Santa.

#### Scenario: After archive
- **WHEN** the group is Archived and `bob`, whose Santa is `alice`, requests `/groups/{id}/santa`
- **THEN** the response contains `alice`'s username and full name and no address, phone number or delivery information

#### Scenario: Before archive
- **WHEN** the group is Active and `bob` requests `/groups/{id}/santa`
- **THEN** the response is 409 with code `invalid_state` and the body does not identify the Santa

### Requirement: Santa never disclosed before archive
While a group is not Archived, no response to a participant SHALL contain, or let them tell apart, the identity of their Santa, including group views, receiver details, question threads, answers, timestamps, question numbers and error messages.

#### Scenario: Receiver's view of questions
- **WHEN** a receiver reads the questions from their Santa in an Active group
- **THEN** the response contains no username, name or other identifier of the Santa

#### Scenario: Question numbers carry no information
- **WHEN** two Santas in the same group each ask a question
- **THEN** both questions are numbered from 1 within their own thread, so numbers reveal nothing about other threads

#### Scenario: Group view
- **WHEN** a participant views an Active group
- **THEN** the response lists the participants as usual and indicates neither their receiver nor their Santa

### Requirement: Nothing about other people's assignments
A participant SHALL NOT be able to learn who anyone else's receiver or Santa is through these endpoints.

#### Scenario: Only my own receiver
- **WHEN** any participant uses the receiver endpoint
- **THEN** the response concerns only their own receiver, never another participant's
