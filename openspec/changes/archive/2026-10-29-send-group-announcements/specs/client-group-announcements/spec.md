# Spec Delta

## Purpose
Gives the owner of a Pending or Active group a way to write an announcement in the app, choose how it is sent, confirm it and see how many people it reached.

## ADDED Requirements

### Requirement: Send announcement button
The administrator's page for a Pending or Active group SHALL show a "Send announcement" button that opens the announcement sheet. Draft and Archived groups SHALL NOT show it.

#### Scenario: Pending group
- **WHEN** the owner opens a Pending group
- **THEN** the button is shown

#### Scenario: Active group
- **WHEN** the owner opens an Active group
- **THEN** the button is shown

#### Scenario: Draft or Archived
- **WHEN** the owner opens a Draft or Archived group
- **THEN** the button is not shown

### Requirement: Announcement sheet
The sheet SHALL have a one-line title field, a multi-line message field, a "Send as" choice of "Email" and "Push notification", and a Send button. It SHALL show the email subject as it will read (`<title>: <group name>`) and, when the message is longer than 300 characters, a hint that push notifications show only the first 300. Send SHALL be enabled only with a title, a message and at least one channel chosen, and the fields SHALL stop accepting characters past 100 and 2000.

#### Scenario: Ready to send
- **WHEN** a title, a message and one channel are filled in
- **THEN** Send is enabled

#### Scenario: Missing part
- **WHEN** the title or the message is empty, or no channel is chosen
- **THEN** Send is disabled

#### Scenario: Subject preview
- **WHEN** the title is "Schedule change" in the group "Office Party"
- **THEN** the sheet shows "Schedule change: Office Party"

#### Scenario: Long message hint
- **WHEN** the message is 400 characters long
- **THEN** the sheet says push notifications show only the first 300

#### Scenario: Limits
- **WHEN** the person types past 100 characters in the title
- **THEN** the extra characters are not accepted

### Requirement: Channels that are not set up
The sheet SHALL read from the account which channels the server can use. A channel the server cannot use SHALL be shown greyed out, unchecked and unselectable, with the reason beside it ("Email is not set up on this server" or "Push notifications are not set up on this server"). With neither available, Send SHALL be disabled. Channels that can be used SHALL start checked.

#### Scenario: Email not set up
- **WHEN** the server cannot send email
- **THEN** Email is greyed out with its reason and Push notification is still choosable

#### Scenario: Neither set up
- **WHEN** the server can send neither
- **THEN** both are greyed out with their reasons and Send is disabled

#### Scenario: Both set up
- **WHEN** the server can send both
- **THEN** both start checked

### Requirement: Confirm before sending
Pressing Send SHALL ask for confirmation, naming the group and the chosen channels, and nothing SHALL be sent until the person confirms. Cancelling SHALL return to the sheet with everything still filled in.

#### Scenario: Confirm
- **WHEN** the person presses Send and then confirms
- **THEN** the announcement is sent

#### Scenario: Cancel
- **WHEN** the person presses Send and then cancels
- **THEN** nothing is sent and the sheet is as it was

### Requirement: Result and errors
After sending, the sheet SHALL show one line per channel used: for email, how many were sent, failed and had no address; for push, how many were reached and not reached. While sending, the fields and Send SHALL be disabled. A rejected or failed send SHALL show the server's message in the sheet and keep what was typed. Closing the sheet SHALL discard everything and keep no history.

#### Scenario: Both channels
- **WHEN** an announcement is sent as email and push
- **THEN** the sheet shows an email line and a push line

#### Scenario: Only one channel
- **WHEN** only push was chosen
- **THEN** the sheet shows only the push line

#### Scenario: Sending
- **WHEN** the request is in flight
- **THEN** the fields and Send are disabled

#### Scenario: Rejected
- **WHEN** the server refuses the announcement
- **THEN** the message is shown and the title and message are kept

#### Scenario: Closed
- **WHEN** the sheet is closed and opened again
- **THEN** it is empty
