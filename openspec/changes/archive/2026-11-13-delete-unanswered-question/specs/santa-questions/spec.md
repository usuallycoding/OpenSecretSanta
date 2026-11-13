## MODIFIED Requirements

### Requirement: Ask a question
The server SHALL expose `POST /groups/{id}/receiver/questions` letting a participant of an Active group ask their receiver a question of 1 to 1000 characters. Each Santa SHALL be able to have up to 200 questions per group, and each question SHALL get a number higher than every number that Santa's thread has ever used.

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
- **WHEN** a Santa who has 200 questions in a group asks another
- **THEN** the response is 409 with code `invalid_state` and no question is added

#### Scenario: Numbers are not reused
- **WHEN** a Santa deletes their highest-numbered question and then asks a new one
- **THEN** the new question gets the next number after the deleted one, never the deleted number

## REMOVED Requirements

### Requirement: Questions are not editable
**Reason**: A Santa may withdraw a question that has not been answered; see "Santa deletes an unanswered question". Questions still cannot be edited, and an answered question still cannot be removed.
**Migration**: None; no endpoint changes the text of a question.

## ADDED Requirements

### Requirement: Santa deletes an unanswered question
The server SHALL expose `DELETE /groups/{id}/receiver/questions/{number}` letting a participant of an Active group delete one of their own questions that has no answer, answering 204. The question SHALL disappear from the Santa's thread and from the receiver's. The path and the answer SHALL name no user.

#### Scenario: Delete an unanswered question
- **WHEN** a Santa deletes question 2, which has no answer
- **THEN** the response is 204 and neither the Santa's nor the receiver's list contains it

#### Scenario: Other questions keep their numbers
- **WHEN** a Santa with questions 1, 2 and 3 deletes question 2
- **THEN** the thread holds questions 1 and 3 and their numbers are unchanged

#### Scenario: Answered question
- **WHEN** a Santa deletes a question that has an answer
- **THEN** the response is 409 with code `invalid_state` and the question and its answer are unchanged

#### Scenario: Unknown question
- **WHEN** a Santa deletes a number that is not in their thread, including one deleted before
- **THEN** the response is 404

#### Scenario: Only their own questions
- **WHEN** a participant deletes a number
- **THEN** only a question in the thread they asked as a Santa can be deleted, never one they received

#### Scenario: Group not Active
- **WHEN** a Santa deletes a question in a Pending or an Archived group
- **THEN** the response is 409 with code `invalid_state` and nothing changes

#### Scenario: Frees a place
- **WHEN** a Santa who has 200 questions deletes an unanswered one
- **THEN** they can ask one more

### Requirement: Deleting and answering do not interleave
A question SHALL be either deleted or answered, never both: when a delete and an answer to the same question arrive together, the one the server handles second SHALL be refused.

#### Scenario: Answer arrives after the delete
- **WHEN** the receiver answers a question the Santa has just deleted
- **THEN** the response is 404 and no answer is stored

#### Scenario: Delete arrives after the answer
- **WHEN** the Santa deletes a question the receiver has just answered
- **THEN** the response is 409 with code `invalid_state` and the question keeps its answer
