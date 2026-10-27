# gift-visibility Specification

## Purpose

Defines what a participant of an active Secret Santa group may learn about the exchange: the details of the person they give a gift to, the reveal of who gave them a gift once the group is archived, and the guarantee that a Santa stays hidden until then.

## Requirements

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

### Requirement: Receiver wishlist
The server SHALL expose `GET /groups/{id}/receiver/wishlist` to a participant of an Active or Archived group, returning the wishlist that the participant's own receiver keeps for that group, in the receiver's chosen order, with each item's title, note, link, creation time and last-changed time and without item ids. The receiver's details response SHALL stay as it is.

#### Scenario: Santa reads the list
- **WHEN** `alice`, whose receiver is `bob`, requests `/groups/{id}/receiver/wishlist` for an Active group
- **THEN** the response lists `bob`'s items for that group in his order with title, note, link and times, and no ids

#### Scenario: Live and ordered
- **WHEN** `bob` adds, edits or reorders items in that group after it became Active
- **THEN** `alice`'s next read shows the current items in the current order

#### Scenario: Empty list
- **WHEN** the receiver has no items in this group
- **THEN** the response is 200 with an empty list

#### Scenario: Still readable after archive
- **WHEN** the group has been Archived
- **THEN** a participant can still read their receiver's wishlist, unchanged

#### Scenario: Not before activation
- **WHEN** a participant requests the receiver's wishlist of a Pending group
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Participants only
- **WHEN** someone who does not participate in the group, an admin who is only its owner, or a user asking about a Draft or unknown group requests it
- **THEN** the response is 404

#### Scenario: Only my own receiver's list
- **WHEN** any participant uses this endpoint
- **THEN** they receive their own receiver's wishlist for this group, never another participant's

#### Scenario: Different groups, different lists
- **WHEN** `bob` has one wishlist in a group where `alice` is his Santa and a different wishlist in another group where `carol` is his Santa
- **THEN** each Santa reads only the wishlist `bob` keeps for the group they share with him

### Requirement: Reading a wishlist reveals nothing about the reader
Reading a receiver's wishlist SHALL NOT be visible to the receiver in any form, so that the receiver cannot learn who their Santa is from who has looked.

#### Scenario: No trace of reads
- **WHEN** a Santa reads their receiver's wishlist, repeatedly
- **THEN** no response to the receiver, including their own wishlist, contains any record of reads, readers or read times
