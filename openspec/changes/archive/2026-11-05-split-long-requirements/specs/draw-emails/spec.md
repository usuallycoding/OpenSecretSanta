## MODIFIED Requirements

### Requirement: Participants are emailed their draw
When a group becomes Active the server SHALL send one email to each participant whose account has an email address, addressed only to that person. The email SHALL give the group's name, description, date, time and location (those that are set), say that the names have been drawn, and name the participant's own receiver (the username and, when set, the full name).

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
- **WHEN** the app address is `https://santa.example.org` and group 7 is drawn
- **THEN** each email contains `https://santa.example.org/#/groups/7`

#### Scenario: Address with a trailing slash
- **WHEN** the app address is configured as `https://santa.example.org/`
- **THEN** the link is `https://santa.example.org/#/groups/7`, with one slash before the `#`

#### Scenario: No app address
- **WHEN** no app address is configured
- **THEN** the email has no link

## ADDED Requirements

### Requirement: The draw email names only the person's own receiver
The draw email SHALL NOT name anyone else's receiver or Santa and SHALL NOT include the receiver's address, phone or delivery information. The subject SHALL NOT name the receiver.

#### Scenario: Another pair
- **WHEN** `alice`'s email is read
- **THEN** no other pair of the draw appears in it

### Requirement: The draw email links to the group
The email SHALL link to the group in the web app, as the app address followed by `/#/groups/` and the group's id, when an app address is configured.

#### Scenario: Link opens the group page
- **WHEN** the app address is `https://santa.example.org` and group 7 is drawn
- **THEN** the link goes to group 7's page and not to the front page
