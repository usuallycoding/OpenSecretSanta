# Spec Delta

## MODIFIED Requirements

### Requirement: Participants are emailed their draw
When a group becomes Active the server SHALL send one email to each participant who has set an email in their delivery info for the group, addressed only to that person. The email SHALL give the group's name, description, date, time, location and budget with its currency (those that are set), say that the names have been drawn, and name the participant's own receiver (the username and, when set, the full name from their delivery info).

#### Scenario: Each person gets their own receiver
- **WHEN** a group of `alice`, `bob`, `carol` and `dave` with email addresses is drawn so that `alice` gives to `bob`
- **THEN** `alice`'s email names `bob` as the person she is Secret Santa for, and no email names anyone else's receiver

#### Scenario: Group details
- **WHEN** the group has a description, a date, a time, a location and the budget 100 in `RON`
- **THEN** each email carries them, the budget as "Budget: 100 RON" after the location, and a group without a date does not show an empty date

#### Scenario: Nothing private in the subject or the body
- **WHEN** the receiver has set an address, a phone and delivery information
- **THEN** none of them appears in the email, and the subject does not contain the receiver's name

#### Scenario: Link to the app
- **WHEN** the app address is `https://santa.example.org` and group 7 is drawn
- **THEN** each email contains `https://santa.example.org/#/groups/7`

#### Scenario: Address with a trailing slash
- **WHEN** the app address is configured as `https://santa.example.org/`
- **THEN** the link is `https://santa.example.org/#/groups/7`, with one slash before the `#`

#### Scenario: No app address
- **WHEN** no app address is configured
- **THEN** the email has no link

#### Scenario: No budget
- **WHEN** the group has no budget
- **THEN** the email has no budget line

### Requirement: Only people with an address are emailed
A participant without an account, or who has set no email in their delivery info for the group, SHALL NOT be emailed, SHALL NOT stop the others from being emailed and SHALL be counted as having no address.

#### Scenario: A participant without an address
- **WHEN** one of four participants has no email address
- **THEN** three emails are sent and the summary counts one participant without an address

