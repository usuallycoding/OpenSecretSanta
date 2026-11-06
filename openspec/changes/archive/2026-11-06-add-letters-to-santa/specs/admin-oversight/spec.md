# Spec Delta

## ADDED Requirements

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
