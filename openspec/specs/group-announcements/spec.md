# group-announcements Specification

## Purpose
Lets the owner of a group send the participants an announcement of their own, such as a change of time or a reminder, by email, by push notification or both, with the channels, the content, the recipients and the results described here.

## Requirements

### Requirement: Announcing to a group
The server SHALL expose `POST /admin/groups/{id}/announcements` taking a title, a message and the channels to send as. Only the group's owner MAY use it, and only while the group is Pending or Active. A group that is Draft or Archived SHALL be refused with 409 and nothing SHALL be sent; a group that is not the caller's SHALL be answered as if it did not exist.

#### Scenario: Pending group
- **WHEN** the owner announces to a Pending group
- **THEN** the announcement is sent

#### Scenario: Active group
- **WHEN** the owner announces to an Active group
- **THEN** the announcement is sent

#### Scenario: Draft or Archived
- **WHEN** the owner announces to a Draft group, or to an Archived one
- **THEN** the answer is 409 and nothing is sent

#### Scenario: Not the owner
- **WHEN** another administrator, or a participant who is not an administrator, tries to announce to the group
- **THEN** the answer is 404 (or 403 for a non-administrator) and nothing is sent

### Requirement: Title and message
The title SHALL be one line of 1 to 100 characters and the message 1 to 2000 characters, each after leading and trailing spaces are removed. A title with a line break, an empty or too long title, and an empty or too long message SHALL be refused with 400 naming the field, and nothing SHALL be sent. Line breaks in the message SHALL be kept.

#### Scenario: Title with a line break
- **WHEN** the title contains a line break
- **THEN** the answer is 400 naming `title` and nothing is sent

#### Scenario: Too long
- **WHEN** the message is longer than 2000 characters
- **THEN** the answer is 400 naming `body` and nothing is sent

#### Scenario: Empty
- **WHEN** the title or the message is empty or only spaces
- **THEN** the answer is 400 naming that field

#### Scenario: Line breaks kept
- **WHEN** the message has three lines
- **THEN** the email carries all three lines

### Requirement: Channels
The request SHALL say whether to send as email and whether to send as a push notification, and at least one SHALL be true; otherwise the answer SHALL be 400 naming `channels`. A channel the server has not set up SHALL NOT be used: a request that names one SHALL be refused with 409 saying which channel is not set up, and nothing SHALL be sent on the other channel either.

#### Scenario: Email only
- **WHEN** the request asks for email only
- **THEN** only emails are sent and the answer has no push summary

#### Scenario: Both
- **WHEN** the request asks for email and push
- **THEN** both are sent and the answer has a summary for each

#### Scenario: Neither
- **WHEN** the request asks for neither
- **THEN** the answer is 400 naming `channels`

#### Scenario: A channel that is not set up
- **WHEN** the request asks for email while email is not configured
- **THEN** the answer is 409 saying email is not set up, and nothing is sent, not even a push that was also asked for

### Requirement: The announcement email
Each announcement email SHALL have the subject `<title>: <group name>` and greet the participant by username, then give the message, the group's description, date, time, location and budget with its currency (those that are set), the link to the group in the web app (the app address followed by `/#/groups/` and the group's id) when an app address is configured, and the closing line the draw email has. It SHALL NOT mention anyone's receiver or Santa.

#### Scenario: Layout
- **WHEN** an announcement with the title "Schedule change" and a message is sent for the group "Office Party" with a date, a time, a location and the budget 100 in `RON`
- **THEN** the subject is "Schedule change: Office Party" and the email holds the greeting, the message, the group's details, among them "Budget: 100 RON", and the group link

#### Scenario: Link to the group
- **WHEN** the app address is `https://santa.example.org` and an announcement is sent for group 7
- **THEN** the email contains `https://santa.example.org/#/groups/7`, with one slash before the `#` even when the address is configured with a trailing slash

#### Scenario: No app address
- **WHEN** no app address is configured
- **THEN** the email has no link

#### Scenario: Nothing about the draw
- **WHEN** the group is Active and has been drawn
- **THEN** no email names anyone's receiver or Santa

#### Scenario: No budget
- **WHEN** the group has no budget
- **THEN** the email has no budget line

### Requirement: Email recipients
Each participant who has set an email in their delivery info for the group SHALL receive one email addressed only to them. Participants without an address SHALL be counted and SHALL NOT stop the others from being emailed.

#### Scenario: One email each
- **WHEN** a group of four participants with addresses is announced to
- **THEN** four emails are sent, each addressed to one person only

#### Scenario: No address
- **WHEN** one of four participants has no address
- **THEN** three emails are sent and the summary counts one participant without an address

### Requirement: The announcement push notification
Each participant other than the person sending, who has at least one registered device and has the "Announcements" switch on, SHALL be sent a push notification titled `<title>: <group name>` whose text is the message only, with no greeting, details or link, cut to its first 300 characters with an ellipsis when longer. Tapping it SHALL open the group. It SHALL be sent in the background, like every other push notification.

#### Scenario: Text is the message only
- **WHEN** an announcement is sent as a push notification
- **THEN** its title is `<title>: <group name>` and its text is the message, and it holds none of the email's greeting, details or link

#### Scenario: Long message
- **WHEN** the message is 800 characters long
- **THEN** the push text is its first 300 characters followed by an ellipsis

#### Scenario: Switch off
- **WHEN** a participant has turned "Announcements" off
- **THEN** they are not sent the push notification

#### Scenario: The sender
- **WHEN** the owner is also a participant
- **THEN** they are not sent the push notification

#### Scenario: Tap target
- **WHEN** the notification is delivered
- **THEN** it carries the group and the category "announcements"

### Requirement: Each channel reports its own results
The answer SHALL carry, for each channel that was used, a summary, and none for a channel that was not. The email summary SHALL give the number sent, the number that failed and the number of participants without an address. The push summary SHALL give the number of participants the notification was queued for (those with a device and the switch on) and the number of other participants, the sender not counted, who have notifications off or no device.

#### Scenario: Email summary
- **WHEN** an eleven-person group with nine addresses is emailed and one email fails
- **THEN** the answer says 8 sent, 1 failed and 2 without an address

#### Scenario: Push summary
- **WHEN** a group of five, sent by one of its members, has two people with a device and the switch on and two without
- **THEN** the push summary says 2 reached and 2 not reached

#### Scenario: Channel not asked for
- **WHEN** only email was asked for
- **THEN** the answer has an email summary and no push summary

### Requirement: Failures never stop the other channel and are kept private
A failure to send an email, including the email service being unreachable or refusing the request, SHALL be counted in the summary, SHALL NOT fail the request and SHALL NOT be retried, and SHALL NOT stop the push notification from being queued. Logs SHALL NOT contain an address, a title, a message or a key.

#### Scenario: Email service down
- **WHEN** the email service cannot be reached while an announcement is sent as email and push
- **THEN** the answer is a success, the email summary counts every email as failed and the push is still queued

#### Scenario: Logs
- **WHEN** an announcement is sent and an email fails
- **THEN** the log names the kind of failure and holds no address, title or message

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

### Requirement: The sender has seen what they announce
When an announcement is kept, its sender SHALL be marked as having seen it, if the sender takes part in the group, so that the sender's own announcements are never new to them. A sender who does not take part in the group SHALL have no mark.

#### Scenario: The owner is a participant
- **WHEN** the owner, who is also a participant, announces to the group
- **THEN** the announcement is not counted as new for the owner, and is for the other participants

#### Scenario: After a hand-over
- **WHEN** a group's owner hands the administrator role over and the new owner, a participant, announces
- **THEN** the announcement is new to the previous owner, if they take part, and not to the new one

#### Scenario: The owner is not a participant
- **WHEN** the owner, who does not take part in the group, announces
- **THEN** no mark is kept for the owner

#### Scenario: A refused announcement
- **WHEN** an announcement is refused
- **THEN** the sender's mark is as it was
