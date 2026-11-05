## MODIFIED Requirements

### Requirement: The announcement email
Each announcement email SHALL have the subject `<title>: <group name>` and greet the participant by username, then give the message, the group's description, date, time and location (those that are set), the link to the group in the web app (the app address followed by `/#/groups/` and the group's id) when an app address is configured, and the closing line the draw email has. It SHALL NOT mention anyone's receiver or Santa.

#### Scenario: Layout
- **WHEN** an announcement with the title "Schedule change" and a message is sent for the group "Office Party" with a date, a time and a location
- **THEN** the subject is "Schedule change: Office Party" and the email holds the greeting, the message, the group's details and the group link

#### Scenario: Link to the group
- **WHEN** the app address is `https://santa.example.org` and an announcement is sent for group 7
- **THEN** the email contains `https://santa.example.org/#/groups/7`, with one slash before the `#` even when the address is configured with a trailing slash

#### Scenario: No app address
- **WHEN** no app address is configured
- **THEN** the email has no link

#### Scenario: Nothing about the draw
- **WHEN** the group is Active and has been drawn
- **THEN** no email names anyone's receiver or Santa
