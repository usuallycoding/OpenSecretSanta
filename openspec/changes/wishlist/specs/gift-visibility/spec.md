# Spec Delta

## ADDED Requirements

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
