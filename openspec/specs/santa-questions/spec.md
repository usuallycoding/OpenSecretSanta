# santa-questions Specification

## Purpose

Defines the conversation between a Secret Santa and the person they give a gift to: the Santa asks questions, the receiver answers them without learning who is asking, and both can read the whole exchange.

## Requirements

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

### Requirement: Questions only while Active
Questions and answers SHALL be written only while the group is Active. In an Archived group the thread SHALL stay readable but SHALL NOT change.

#### Scenario: Archived group
- **WHEN** a Santa asks a question, or a receiver answers one, in an Archived group
- **THEN** the response is 409 with code `invalid_state` and nothing changes

#### Scenario: Not yet Active
- **WHEN** a participant asks a question in a Pending group
- **THEN** the response is 409 with code `invalid_state`

### Requirement: Santa reads their thread
The server SHALL expose `GET /groups/{id}/receiver/questions` returning every question the participant has asked their receiver, in order, each with its number, text, time asked and, when answered, the answer, the time it was first given and the time it was last changed.

#### Scenario: Answers appear
- **WHEN** the receiver has answered question 1 but not question 2
- **THEN** the Santa sees question 1 with its answer and question 2 without one

#### Scenario: Answer edits appear
- **WHEN** the receiver changes their answer
- **THEN** the Santa sees the new text and an updated last-changed time

### Requirement: Receiver reads the questions
The server SHALL expose `GET /groups/{id}/santa/questions` returning the questions asked by the participant's Santa, in order, with the same fields as the Santa's view and with no identification of the Santa.

#### Scenario: Receiver sees questions
- **WHEN** a Santa has asked two questions
- **THEN** the receiver sees both, numbered 1 and 2, and their own answers where given

#### Scenario: No Santa identity
- **WHEN** the response is inspected
- **THEN** it contains no username, name or other identifier of the Santa

#### Scenario: Both threads
- **WHEN** one participant is both a Santa and a receiver
- **THEN** their questions to their receiver and the questions from their Santa are two separate threads that never mix

### Requirement: Answer and edit answers
The server SHALL expose `PUT /groups/{id}/santa/questions/{number}/answer` letting the receiver set or replace the answer to one of their Santa's questions with text of 1 to 2000 characters. The answer MAY be changed any number of times while the group is Active.

#### Scenario: First answer
- **WHEN** the receiver answers question 1
- **THEN** the answer is stored with the time it was given

#### Scenario: Editing
- **WHEN** the receiver answers question 1 again with different text
- **THEN** the answer is replaced, the original time is kept and the last-changed time is updated

#### Scenario: Unknown question
- **WHEN** the receiver answers a question number that does not exist in their thread
- **THEN** the response is 404

#### Scenario: Blank or too long
- **WHEN** the answer is blank or longer than 2000 characters
- **THEN** the response is 400 naming `text` and the previous answer is unchanged

### Requirement: Threads are private to the pair
A thread SHALL be readable and writable only by its Santa (for questions) and its receiver (for answers), with one exception: the owner of the group SHALL be able to read every thread through `GET /admin/groups/{id}/questions` as described by the admin-oversight capability. No other participant and no other admin SHALL have any way to read the content, and no admin, including the owner, SHALL have any way to write it.

#### Scenario: Other participants
- **WHEN** a participant who is neither the Santa nor the receiver of a thread uses the question endpoints
- **THEN** they only ever see and change their own two threads

#### Scenario: Owner has no access
- **WHEN** the group's owner, who is not a participant, uses the participant question endpoints
- **THEN** the response is 404, and only the admin-oversight endpoint returns question or answer text

#### Scenario: Owner reads through oversight
- **WHEN** the group's owner uses the admin-oversight questions endpoint
- **THEN** the response contains every thread with its questions and answers, read-only

#### Scenario: Other admins
- **WHEN** an admin who does not own the group requests its questions through any admin endpoint
- **THEN** the response is 404 and contains no question or answer text

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
