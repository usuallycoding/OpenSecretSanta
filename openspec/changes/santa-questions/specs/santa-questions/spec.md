# Spec Delta

## Purpose

Defines the conversation between a Secret Santa and the person they give a gift to: the Santa asks questions, the receiver answers them without learning who is asking, and both can read the whole exchange.

## ADDED Requirements

### Requirement: Ask a question
The server SHALL expose `POST /groups/{id}/receiver/questions` letting a participant of an Active group ask their receiver a question of 1 to 1000 characters. Each Santa SHALL be able to ask any number of questions up to 30 per group, and each question SHALL get a number counting up from 1 within that Santa's own thread.

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
- **WHEN** a Santa who has asked 30 questions in a group asks another
- **THEN** the response is 409 with code `invalid_state` and no question is added

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

### Requirement: Questions are not editable
Once asked, a question SHALL NOT be changed or deleted by anyone.

#### Scenario: No way to change a question
- **WHEN** a Santa wants to correct or remove a question they asked
- **THEN** no endpoint allows it and the question stays exactly as asked

### Requirement: Threads are private to the pair
A thread SHALL be readable and writable only by its Santa (for questions) and its receiver (for answers). No other participant, and no admin including the group's owner, SHALL have any way to read the content.

#### Scenario: Other participants
- **WHEN** a participant who is neither the Santa nor the receiver of a thread uses the question endpoints
- **THEN** they only ever see and change their own two threads

#### Scenario: Owner has no access
- **WHEN** the group's owner, who is not a participant, uses the question endpoints
- **THEN** the response is 404 and no admin endpoint returns question or answer text
