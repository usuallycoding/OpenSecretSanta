# Spec Delta

## MODIFIED Requirements

### Requirement: Ask a question
The server SHALL expose `POST /groups/{id}/receiver/questions` letting a participant of an Active group ask their receiver a question of 1 to 1000 characters. Each Santa SHALL be able to ask any number of questions up to 200 per group, and each question SHALL get a number counting up from 1 within that Santa's own thread.

#### Scenario: First and second question
- **WHEN** a Santa asks two questions in an Active group
- **THEN** they are numbered 1 and 2 and each is returned with its text and time asked

#### Scenario: Numbering is per Santa
- **WHEN** two different Santas in the same group each ask their first question
- **THEN** both questions are number 1

#### Scenario: Blank or too long
- **WHEN** the text is blank or longer than 1000 characters
- **THEN** the response is 400 naming `text` and nothing is stored

#### Scenario: Question limit
- **WHEN** a Santa who has asked 200 questions in a group asks another
- **THEN** the response is 409 with code `invalid_state` and no question is added
