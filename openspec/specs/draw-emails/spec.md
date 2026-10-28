# draw-emails Specification

## Purpose
Tells each participant of a group, by email, that the names have been drawn and who they are Secret Santa for, as soon as the owner makes the group Active.

## Requirements

### Requirement: Participants are emailed their draw
When a group becomes Active the server SHALL send one email to each participant whose account has an email address, addressed only to that person. The email SHALL give the group's name, description, date, time and location (those that are set), say that the names have been drawn, and name the participant's own receiver (the username and, when set, the full name). It SHALL NOT name anyone else's receiver or Santa and SHALL NOT include the receiver's address, phone or delivery information. The subject SHALL NOT name the receiver. The email SHALL link to the web app when an app address is configured.

#### Scenario: Each person gets their own receiver
- **WHEN** a group of `alice`, `bob`, `carol` and `dave` with email addresses is drawn so that `alice` gives to `bob`
- **THEN** `alice`'s email names `bob` as the person she is Secret Santa for, and no email names anyone else's receiver

#### Scenario: Group details
- **WHEN** the group has a description, a date, a time and a location
- **THEN** each email carries them, and a group without a date does not show an empty date

#### Scenario: Nothing private in the subject or the body
- **WHEN** the receiver has set an address, a phone and delivery information
- **THEN** none of them appears in the email, and the subject does not contain the receiver's name

#### Scenario: Link to the app
- **WHEN** an app address is configured
- **THEN** each email contains it

### Requirement: Only people with an address are emailed
A participant without an account, or whose account has no email address, SHALL NOT be emailed, SHALL NOT stop the others from being emailed and SHALL be counted as having no address.

#### Scenario: A participant without an address
- **WHEN** one of four participants has no email address
- **THEN** three emails are sent and the summary counts one participant without an address

### Requirement: The draw does not depend on the email
The emails SHALL be sent only after the assignments are stored. A failure to send, including the email service being unreachable, refusing the request or timing out, SHALL NOT undo or fail the activation, SHALL be counted in the summary and logged without the API key or any email address's content, and SHALL NOT be retried by a later request. An activation that fails SHALL send no email.

#### Scenario: Service unreachable
- **WHEN** the email service cannot be reached while a group is activated
- **THEN** the group is Active with its assignments, the response is a success and the summary counts every email as failed

#### Scenario: Activation refused
- **WHEN** an activation is refused (too few participants, or the group is no longer Pending)
- **THEN** no email is sent

#### Scenario: No second round
- **WHEN** a second activation of the same group arrives
- **THEN** it is refused with 409 and sends no email

### Requirement: The activation reports the emails
The response to `POST /admin/groups/{id}/activate` SHALL include an `emails` summary with the number of emails sent, the number that failed and the number of participants without an address, and SHALL have no summary there (the field absent or null) when email is not configured. A request that is not an activation SHALL NOT include it.

#### Scenario: Summary
- **WHEN** an eleven-person group with nine addresses is activated and one email fails
- **THEN** the response says 8 sent, 1 failed and 2 without an address

#### Scenario: Not configured
- **WHEN** email is not configured and a group is activated
- **THEN** the group becomes Active, no email is attempted and the response has no `emails`

### Requirement: Email is optional and the key stays secret
Email SHALL be off unless an API key and a sender address are configured. The API key SHALL be sent only to the email service, SHALL NOT be written to logs or returned by any endpoint, and SHALL NOT be committed.

#### Scenario: Off by default
- **WHEN** the server starts without the email settings
- **THEN** it works as before and never contacts the email service

#### Scenario: Key not exposed
- **WHEN** any endpoint is called or the server logs a failure to send
- **THEN** the API key appears in no response and no log line
