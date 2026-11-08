# announcement-history Specification

## Purpose
Lets the people of a group read the announcements its owner has sent, after the notification or email is gone: what each said, when it was sent, and in what order, for the participants and for the owner.

## Requirements

### Requirement: A participant reads the group's announcements
The server SHALL expose `GET /groups/{id}/announcements` returning every announcement kept for the group, in the order they were sent, each with its number, title, message and the time it was sent. It SHALL be answered for a participant of a Pending, Active or Archived group, and a Draft group, a group the caller does not take part in and a missing group SHALL be answered with 404.

#### Scenario: Reading
- **WHEN** a participant of an Active group, to which the owner has sent two announcements, reads them
- **THEN** the response lists both in the order sent, with number 1 and 2, title, message and time sent

#### Scenario: Pending and Archived
- **WHEN** a participant of a Pending group, or of an Archived one, reads the announcements
- **THEN** the response lists them

#### Scenario: No announcements
- **WHEN** a participant of a group that has had no announcement reads them
- **THEN** the response is an empty list

#### Scenario: Not a participant, or a Draft
- **WHEN** someone who is not a participant reads a group's announcements, or a participant asks for those of a Draft group
- **THEN** the response is 404

#### Scenario: Someone who joined later
- **WHEN** a person is added to a group after the owner announced
- **THEN** they read every announcement of the group, including those sent before they joined

#### Scenario: Line breaks kept
- **WHEN** an announcement's message has three lines
- **THEN** the response carries all three lines

### Requirement: The owner reads the group's announcements
The server SHALL expose `GET /admin/groups/{id}/announcements` with the same answer, for the group's owner, whether or not the owner takes part in the group, in a Pending, Active or Archived group. A Draft group SHALL be answered with 409 and code `invalid_state`. A group that is not the caller's SHALL be answered as if it did not exist, and a caller who is not an administrator with 403.

#### Scenario: The owner outside the group
- **WHEN** the owner, who is not a participant, reads the announcements of their Active group
- **THEN** the response lists them

#### Scenario: Not the owner
- **WHEN** another administrator reads the announcements of the group
- **THEN** the response is 404

#### Scenario: Not an administrator
- **WHEN** a participant who is not an administrator uses this route
- **THEN** the response is 403

#### Scenario: A Draft group
- **WHEN** the owner reads the announcements of a Draft group
- **THEN** the response is 409 with code `invalid_state`

### Requirement: Announcements are read-only and go with the group
The server SHALL offer no way to edit or delete one announcement, for the owner or anyone else, and reading SHALL record nothing about who read. Deleting a group SHALL delete its announcements.

#### Scenario: No edit or delete
- **WHEN** the owner tries to change or delete a sent announcement through the API
- **THEN** there is no such route

#### Scenario: Group deleted
- **WHEN** the owner deletes a group that holds announcements
- **THEN** the announcements are gone with it
