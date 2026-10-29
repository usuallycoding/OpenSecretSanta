# Spec Delta

## Purpose
Lets the owner of a group send the participants an announcement of their own, such as a change of time or a reminder, by email, by push notification or both, with the channels, the content, the recipients and the results described here.

## ADDED Requirements

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
Each announcement email SHALL have the subject `<title>: <group name>` and greet the participant by username, then give the message, the group's description, date, time and location (those that are set), the link to the app when an app address is configured, and the closing line the draw email has. It SHALL NOT mention anyone's receiver or Santa.

#### Scenario: Layout
- **WHEN** an announcement with the title "Schedule change" and a message is sent for the group "Office Party" with a date, a time and a location
- **THEN** the subject is "Schedule change: Office Party" and the email holds the greeting, the message, the group's details and the app link

#### Scenario: Nothing about the draw
- **WHEN** the group is Active and has been drawn
- **THEN** no email names anyone's receiver or Santa

### Requirement: Email recipients
Each participant whose account has an email address SHALL receive one email addressed only to them. Participants without an address SHALL be counted and SHALL NOT stop the others from being emailed.

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
