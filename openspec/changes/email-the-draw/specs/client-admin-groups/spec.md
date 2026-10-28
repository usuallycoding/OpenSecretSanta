## ADDED Requirements

### Requirement: Saying who was emailed
When the answer to "Draw the names" carries an email summary, the group's page SHALL say once how many participants were emailed their draw and, when there are any, how many emails failed and how many participants have no email address, until it is dismissed or the page is left. When the answer has no summary, the page SHALL say nothing about email.

#### Scenario: All emailed
- **WHEN** the draw is made for 4 participants and 4 emails are sent
- **THEN** the page says that emails were sent to all 4 participants

#### Scenario: Some failed or have no address
- **WHEN** 11 participants are drawn, 8 emails are sent, 1 fails and 2 have no address
- **THEN** the page says 8 of 11 were emailed, 1 failed and 2 have no email address

#### Scenario: Email not configured
- **WHEN** the answer has no email summary
- **THEN** nothing about email is shown

#### Scenario: Dismissed
- **WHEN** the administrator dismisses the note
- **THEN** it is gone until another draw
