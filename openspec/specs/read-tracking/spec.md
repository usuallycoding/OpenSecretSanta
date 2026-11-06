# read-tracking Specification

## Purpose
Keeps, per Santa and group, what the Santa has already seen of their receiver's wishlist, of the answers to their questions and of the letters they were sent, so that the app can show what is new, without any of it being visible to anyone but that Santa.

## Requirements

### Requirement: Read marks of a Santa
The server SHALL keep, for each participant as the Santa of their receiver in a group, the newest time they have seen of each of three kinds: the receiver's wishlist, the answers to their questions and the letters from the receiver. `GET /groups/{id}/receiver/reads` SHALL return, for each kind, the number of unread changes and the time seen up to (null when never marked), for an Active or Archived group.

#### Scenario: Nothing marked yet
- **WHEN** `alice`, whose receiver `bob` has added three wishlist items and sent one letter, reads her marks without ever having marked anything
- **THEN** the wishlist has 3 unread and no time seen up to, the letters 1 unread and no time, and the answers 0

#### Scenario: Each kind on its own
- **WHEN** `alice` has marked the wishlist as seen and not the letters
- **THEN** her wishlist shows its own time and the letters still show none

#### Scenario: Before the draw
- **WHEN** a participant of a Pending group reads their marks
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the marks can still be read

#### Scenario: Not a participant
- **WHEN** someone who is not a participant, or the group's owner who is not in the draw, reads the marks
- **THEN** the response is 404

### Requirement: What counts as unread
A wishlist item SHALL count as unread when it was created or changed after the time seen up to, once however often it changed. An answer SHALL count when it was given or changed after it, and a letter when it was sent after it. With no time seen up to everything SHALL count. An item that was deleted SHALL NOT count.

#### Scenario: New and edited items
- **WHEN** the receiver adds one item and edits another after the time seen up to
- **THEN** the wishlist has 2 unread

#### Scenario: Edited many times
- **WHEN** the receiver edits one item three times after the time seen up to
- **THEN** it counts once

#### Scenario: An answer given or changed
- **WHEN** one question is answered and another answer is changed after the time seen up to
- **THEN** the answers have 2 unread

#### Scenario: Letters
- **WHEN** the receiver sends two letters after the time seen up to
- **THEN** the letters have 2 unread

#### Scenario: Deleted item
- **WHEN** the receiver adds an item and deletes it again before the Santa looks
- **THEN** it counts for nothing

### Requirement: Marking as seen
`PUT /groups/{id}/receiver/reads/{kind}`, with `kind` one of `wishlist`, `answers` and `letters` and a body naming a time `seenUpTo`, SHALL record that the Santa has seen everything up to that time. The server SHALL keep the later of the old and the new time and never one later than its own clock, and SHALL answer with the kind's current marks. It SHALL work for an Active or Archived group.

#### Scenario: Marked
- **WHEN** `alice` marks the letters as seen up to the time of the newest letter she saw
- **THEN** the response shows that time and 0 unread, and a later letter counts as 1

#### Scenario: Never goes back
- **WHEN** `alice` marks a time earlier than the one stored
- **THEN** the stored time stays, and the response shows it

#### Scenario: Never in the future
- **WHEN** `alice` marks a time later than the server's own
- **THEN** the server records its own time instead

#### Scenario: Repeating
- **WHEN** the same time is sent twice
- **THEN** both answer 200 with the same marks

#### Scenario: Bad requests
- **WHEN** the time is missing or is not a valid instant, or the kind is unknown
- **THEN** the response is 400 naming `seenUpTo` for the first and 404 for the second, and nothing changes

#### Scenario: Before the draw
- **WHEN** a participant of a Pending group marks anything
- **THEN** the response is 409 with code `invalid_state`

### Requirement: Read marks are private to their Santa
A Santa's read marks SHALL be returned only to that Santa. No response to their receiver, to any other participant or to the group's owner SHALL contain a mark, a count or a time of reading, and marking SHALL NOT change any response they receive.

#### Scenario: The receiver sees nothing
- **WHEN** a Santa reads their receiver's wishlist, questions and letters and marks them seen, repeatedly
- **THEN** every response to the receiver, their own wishlist, questions, letters and group view included, is byte-for-byte unchanged

#### Scenario: Not for the owner
- **WHEN** the group's owner uses the oversight routes after a Santa marked things seen
- **THEN** no response contains any mark

#### Scenario: Only their own marks
- **WHEN** a participant uses the reads route
- **THEN** they get their own marks as the Santa of their own receiver, never another participant's

### Requirement: Read marks go with the group
Read marks SHALL belong to the Santa of one group and SHALL be removed when the group is deleted.

#### Scenario: Group deleted
- **WHEN** the owner deletes a group in which Santas have marked things seen
- **THEN** the marks are gone with it
