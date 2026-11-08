# Spec Delta

## Purpose

Keeps, for each participant and group, the newest announcement the person has seen, so that the app can show how many announcements are new to them, without any of it being visible to anyone else.

## ADDED Requirements

### Requirement: What a participant has seen of the announcements
The server SHALL keep, for each participant and group, the newest time the person has seen of the group's announcements. `GET /groups/{id}/announcements/reads` SHALL return the number of announcements sent after that time, and that time, null when the person never marked it, for a participant of a Pending, Active or Archived group. With no time seen, every announcement SHALL count.

#### Scenario: Nothing marked yet
- **WHEN** `alice` has never looked at a group to which the owner has sent three announcements and reads her mark
- **THEN** the response has 3 unread and no time seen up to

#### Scenario: Some seen
- **WHEN** `alice` has marked the time of the second of three announcements as seen
- **THEN** the response has 1 unread and that time

#### Scenario: Each person on their own
- **WHEN** `alice` has marked everything seen and `bob` has not
- **THEN** `alice` has 0 unread and `bob` has all of them

#### Scenario: Pending and Archived
- **WHEN** a participant of a Pending group, or of an Archived one, reads their mark
- **THEN** the response is given

#### Scenario: Not readable
- **WHEN** someone who is not a participant, a participant of a Draft group, or the group's owner who does not take part, reads the mark, or the group does not exist
- **THEN** the response is 404

### Requirement: Marking announcements as seen
The server SHALL expose `PUT /groups/{id}/announcements/reads` taking the time seen up to as an ISO-8601 instant. It SHALL keep the later of that time and the one stored, never a time later than now, and return the mark as reading it does. A missing time or one that is not an instant SHALL be refused with 400 naming `seenUpTo`. It SHALL work for a participant of a Pending, Active or Archived group, and answer as reading does for everyone else.

#### Scenario: Marked
- **WHEN** `alice` marks the time of the newest of three announcements
- **THEN** the response has 0 unread and that time, and her next read says the same

#### Scenario: A mark never goes back
- **WHEN** `alice` marks a time earlier than the one she marked before
- **THEN** the stored time is the later one and the response says so

#### Scenario: Not after now
- **WHEN** `alice` marks a time a day in the future
- **THEN** the stored time is now, and an announcement sent a moment later still counts as unread

#### Scenario: Bad time
- **WHEN** the time is missing, `yesterday` or a date without a time
- **THEN** the response is 400 naming `seenUpTo` and nothing is stored

#### Scenario: Archived group
- **WHEN** a participant of an Archived group marks the announcements
- **THEN** the mark is kept: it is the person's own state, not the group's

### Requirement: The marks are the person's own and stay with the group
A person SHALL only be able to read and change their own mark, which SHALL NOT be visible to the owner or to anyone else. A mark SHALL stay when its person is removed from a Pending group, so that someone added again keeps it, and SHALL be deleted with the group.

#### Scenario: Private
- **WHEN** `bob` reads his mark after `alice` marked everything seen
- **THEN** his response says nothing of hers

#### Scenario: Removed and added again
- **WHEN** `alice` marked everything seen, is removed from the Pending group and is added again
- **THEN** her mark is as it was

#### Scenario: Group deleted
- **WHEN** the owner deletes a group in which people have marks
- **THEN** the marks are gone with it
