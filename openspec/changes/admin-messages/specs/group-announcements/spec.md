# Spec Delta

## ADDED Requirements

### Requirement: An announcement is kept
An announcement that passes the checks (the owner, the state, the title and message, the channels and the limit) SHALL be kept with its title, its message with the line breaks, and the time it was sent, and numbered from 1 within the group. It SHALL be kept whatever becomes of the email and the push notifications, and SHALL be kept before anything is sent. An announcement that is refused SHALL NOT be kept.

#### Scenario: Kept and numbered
- **WHEN** the owner announces "Schedule change" to a group that has had no announcement
- **THEN** it is kept as announcement 1 with its title, message and time, and a second one is number 2

#### Scenario: Every email fails
- **WHEN** the owner announces by email and push and every email fails
- **THEN** the announcement is kept all the same

#### Scenario: Refused
- **WHEN** an announcement is refused because the group is a Draft, the title is empty or no channel is chosen
- **THEN** nothing is kept

#### Scenario: Numbering is per group
- **WHEN** two groups each get their first announcement
- **THEN** both are number 1

### Requirement: At most 200 announcements per group
A group SHALL hold at most 200 announcements. Announcing to a group that already holds 200 SHALL be refused with 409 and code `invalid_state`, nothing SHALL be kept, and nothing SHALL be sent on either channel. Announcements SHALL NOT be dropped to make room.

#### Scenario: The 201st
- **WHEN** the owner announces to a group that holds 200 announcements
- **THEN** the answer is 409 with code `invalid_state`, no email or push notification is sent and the 200 are unchanged

#### Scenario: The 200th
- **WHEN** the owner announces to a group that holds 199 announcements
- **THEN** it is sent and kept as number 200
