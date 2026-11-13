## MODIFIED Requirements

### Requirement: Your questions to the receiver
The Gifter page SHALL list the person's own questions to the receiver, loaded with `GET /groups/{id}/receiver/questions`, newest first and numbered, each with the receiver's answer when there is one and "Not answered yet" when there is not. A question SHALL NOT be editable, and one with an answer SHALL NOT be deletable.

#### Scenario: Questions and answers in order
- **WHEN** the person has asked two questions and the receiver answered the first
- **THEN** question 2 is shown first with "Not answered yet", and question 1 below it with its answer

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the questions and answers are shown and there is no way to ask

#### Scenario: Numbers keep their gaps
- **WHEN** the person deleted question 2 of three
- **THEN** the list shows questions 3 and 1, each with its own number

## ADDED Requirements

### Requirement: Deleting an unanswered question
While the group is Active, each of the person's questions without an answer SHALL have a "Delete" action at the bottom of its card, outside the indented answer. It SHALL ask for a confirmation that names the question, and confirming SHALL send `DELETE /groups/{id}/receiver/questions/{number}` and show the list without that question. A question with an answer, and every question in a group that is not Active, SHALL have no such action.

#### Scenario: Delete with confirmation
- **WHEN** the person chooses "Delete" on an unanswered question and confirms
- **THEN** the question is gone from the list and the other questions keep their numbers

#### Scenario: Cancel
- **WHEN** the person chooses "Delete" and then cancels the confirmation
- **THEN** nothing is sent and the question stays

#### Scenario: Answered question
- **WHEN** a question has an answer
- **THEN** its card has no "Delete" action

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** no question has a "Delete" action

#### Scenario: Answered meanwhile
- **WHEN** the person confirms the delete and the server refuses because the receiver has just answered
- **THEN** the screen reloads, the question shows its answer, and a message says it could not be deleted because it was answered

#### Scenario: Already gone or group archived
- **WHEN** the server answers that the question does not exist, or refuses because the group is no longer Active
- **THEN** the screen reloads and says what happened

#### Scenario: Server unreachable while deleting
- **WHEN** the person confirms the delete and the server cannot be reached
- **THEN** the question stays in the list and a message says the server could not be reached

#### Scenario: Frees a place at the limit
- **WHEN** the person is at 200 questions and deletes one
- **THEN** asking is available again

### Requirement: Deleting is named for screen readers
The "Delete" action SHALL be announced as deleting the question, with its number, and the confirmation SHALL name the question's number and text.

#### Scenario: Screen reader
- **WHEN** a screen reader reaches the "Delete" action of question 3
- **THEN** it announces deleting question 3
