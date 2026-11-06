# admin-oversight Specification

## Purpose

What the owner of a group can read about it on the server: each participant's wishlist, every question thread and the whole draw, only for the owner, never writing and leaving no trace that participants can see.

## Requirements

### Requirement: Owner reads a participant's wishlist
The server SHALL expose `GET /admin/groups/{id}/participants/{username}/wishlist`, usable only by the group's owner while the group is Pending, Active or Archived, returning the wishlist that participant keeps for that group in their chosen order, with each item's title, note, link, creation time and last-changed time and without item ids.

#### Scenario: Owner reads a list
- **WHEN** the owner requests the wishlist of participant `bob` in an Active group
- **THEN** the response lists `bob`'s items for that group in his order with title, note, link and times, and no ids

#### Scenario: Pending group
- **WHEN** the owner requests a participant's wishlist while the group is Pending
- **THEN** the response lists the items the participant has added so far

#### Scenario: Empty list
- **WHEN** the participant has no items in this group
- **THEN** the response is 200 with an empty list

#### Scenario: Draft group
- **WHEN** the owner requests a wishlist of a Draft group
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Not a participant
- **WHEN** the owner requests the wishlist of a username that is not a participant of the group
- **THEN** the response is 404

#### Scenario: Still readable after archive
- **WHEN** the group has been Archived
- **THEN** the owner can still read every participant's wishlist, unchanged

### Requirement: Owner reads all question threads
The server SHALL expose `GET /admin/groups/{id}/questions`, usable only by the group's owner while the group is Active or Archived, returning one thread per Santa and receiver pair of the group. Each thread SHALL name the Santa and the receiver and list every question the Santa asked in order, each with its number, text, time asked and, when answered, the answer, the time it was first given and the time it was last changed.

#### Scenario: Owner sees every thread
- **WHEN** the owner requests the questions of an Active group of 4
- **THEN** the response has 4 threads, each naming its Santa and its receiver, with that Santa's questions and the receiver's answers

#### Scenario: Thread without questions
- **WHEN** a Santa has asked nothing yet
- **THEN** their thread is present with an empty list of questions

#### Scenario: Answers and edits visible
- **WHEN** a receiver has answered or changed an answer
- **THEN** the thread shows the current answer with its first-given and last-changed times

#### Scenario: Owner who participates
- **WHEN** the owner is also a participant of the group
- **THEN** the response includes the thread of their own Santa and the thread they themselves are part of, like every other pair

#### Scenario: Before activation
- **WHEN** the owner requests the questions of a Draft or Pending group
- **THEN** the response is 409 with code `invalid_state`

### Requirement: Oversight is owner-only
Both oversight endpoints SHALL be usable only by the owner of the named group. An admin who does not own the group, or any group that does not exist, SHALL be answered with 404, and a user who is not an admin SHALL be refused with 403. A user who must still change a temporary password SHALL be refused with `password_change_required`.

#### Scenario: Another admin
- **WHEN** an admin who does not own the group requests either endpoint
- **THEN** the response is 404

#### Scenario: Not an admin
- **WHEN** a participant who is not an admin requests either endpoint
- **THEN** the response is 403

#### Scenario: Not signed in
- **WHEN** either endpoint is requested without a valid token
- **THEN** the response is 401

### Requirement: Oversight is read-only
The oversight endpoints SHALL only read. No admin endpoint SHALL change or answer a wishlist item, a question or an answer, and the only way an administrator removes them is by deleting the whole group, which removes everything of it at once.

#### Scenario: No write endpoint
- **WHEN** an owner looks for a way to change a participant's wishlist, question or answer through admin endpoints
- **THEN** none exists

#### Scenario: Only with the group
- **WHEN** an owner looks for a way to delete one wishlist item, question or answer of a participant
- **THEN** none exists; deleting the group is the only way they go

### Requirement: Oversight leaves no trace
Reading through the oversight endpoints SHALL NOT be visible to participants in any form: no response a participant receives, including their own wishlist, their own threads, group views and timestamps, SHALL differ because the owner has read, and nothing SHALL record that or when the owner read.

#### Scenario: Participant sees no change
- **WHEN** the owner reads a participant's wishlist and the question threads repeatedly
- **THEN** that participant's own wishlist, receiver wishlist, question threads and group view are byte-for-byte unchanged

### Requirement: The owner sees the whole draw, including their own part
For a group they own, the owner SHALL be able to learn every Santa and receiver pair, including the pair in which they are the Santa and the pair in which they are the receiver, through the oversight and assignments endpoints, whether or not they also participate. This SHALL NOT widen what any participant can learn through participant endpoints.

#### Scenario: Owner learns their own Santa early
- **WHEN** the owner, who is also a participant of an Active group, requests the question threads or the assignments
- **THEN** the response names their own Santa, although the participant endpoint `/groups/{id}/santa` still refuses until the group is Archived

#### Scenario: Participant endpoints unchanged
- **WHEN** the owner uses the participant endpoints of the same group
- **THEN** they behave exactly as for any other participant

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

### Requirement: Owner reads all letters
The server SHALL expose `GET /admin/groups/{id}/letters`, usable only by the group's owner while the group is Active or Archived, returning for every Santa and receiver pair the letters the receiver has sent their Santa, in the order sent, each with its number, title, text and time. It SHALL only read, and nothing about the read SHALL be visible to participants or recorded.

#### Scenario: Owner reads the letters
- **WHEN** the owner requests the letters of an Active group in which `bob`, whose Santa is `alice`, has sent two letters
- **THEN** the response has the pair `alice` and `bob` with both letters, in order, with number, title, text and time

#### Scenario: Pairs without letters
- **WHEN** a pair's receiver has sent none
- **THEN** the pair is listed with an empty list of letters

#### Scenario: Archived group
- **WHEN** the group has been Archived
- **THEN** the owner can still read every pair's letters, unchanged

#### Scenario: Not drawn yet
- **WHEN** the owner requests the letters of a Draft or a Pending group
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Not the owner
- **WHEN** an admin who does not own the group, a participant who is not an admin, or no one signed in requests it
- **THEN** the responses are 404, 403 and 401, as for the other oversight endpoints

#### Scenario: Read-only
- **WHEN** an owner looks for a way to send, change or delete a letter through admin endpoints
- **THEN** none exists

#### Scenario: Participant sees no change
- **WHEN** the owner reads the letters repeatedly
- **THEN** every response a participant gets, their own letters included, is byte-for-byte unchanged, and no notification is sent

#### Scenario: The owner's own part
- **WHEN** the owner is also a participant and reads the letters through this route
- **THEN** they see their own Santa's pair as for any other, while the participant routes still show them only their own letters
