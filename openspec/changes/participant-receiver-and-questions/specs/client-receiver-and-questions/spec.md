# Spec Delta

## ADDED Requirements

### Requirement: Your receiver
Choosing "Your receiver" in an Active or Archived group SHALL open a screen loaded with `GET /groups/{id}/receiver` showing the receiver's username and, for each of full name, address, phone and delivery information that the receiver has set, its value. Fields the receiver has not set SHALL be left out; when the receiver has set none of them the screen SHALL say that the receiver has not added postal details yet.

#### Scenario: Details shown
- **WHEN** the receiver has set a full name and an address but no phone or delivery information
- **THEN** the screen shows the username, the full name and the address and nothing for the others

#### Scenario: No details yet
- **WHEN** the receiver has set none of the four fields
- **THEN** the screen shows the username and says the receiver has not added postal details yet

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist
- **THEN** the screen says the group is no longer available and offers to go back

### Requirement: Your questions to the receiver
The "Your receiver" screen SHALL list the person's own questions to the receiver, loaded with `GET /groups/{id}/receiver/questions`, in the order asked and numbered, each with the receiver's answer when there is one and "Not answered yet" when there is not. A question SHALL NOT be editable or deletable.

#### Scenario: Questions and answers in order
- **WHEN** the person has asked two questions and the receiver answered the first
- **THEN** question 1 is shown with its answer and question 2 with "Not answered yet"

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the questions and answers are shown and there is no way to ask

### Requirement: Asking a question
While the group is Active the person SHALL be able to ask a question in a bottom sheet; a successful `POST /groups/{id}/receiver/questions` SHALL close the sheet and show the question at the end of the list. A question is 1 to 1000 characters after trimming and a person asks at most 30 per group; at 30 asking SHALL be unavailable with an explanation.

#### Scenario: Ask a question
- **WHEN** the person opens the ask sheet, enters a question and saves
- **THEN** the sheet closes and the question is the last one in the list

#### Scenario: Empty or too long
- **WHEN** the person saves a blank question or one over 1000 characters
- **THEN** nothing is sent and the sheet shows what is wrong next to the field

#### Scenario: Question limit
- **WHEN** the person has asked 30 questions
- **THEN** asking is unavailable and the screen says the limit is reached

#### Scenario: Group archived while asking
- **WHEN** the person saves a question and the server refuses because the group is no longer Active
- **THEN** the sheet closes, the screen reloads and says the thread can no longer change

#### Scenario: Server unreachable while asking
- **WHEN** the person saves a question and the server cannot be reached
- **THEN** the sheet stays open with what they typed and says so

### Requirement: Questions from your Santa
Choosing "Questions from your Santa" in an Active or Archived group SHALL open a screen loaded with `GET /groups/{id}/santa/questions` listing the questions the person's Santa asked, numbered and in order, each with the person's own answer when there is one. Nothing on the screen SHALL identify the Santa. When the Santa has asked nothing the screen SHALL say so.

#### Scenario: Questions shown
- **WHEN** the Santa has asked two questions and the person answered the second
- **THEN** question 1 is shown without an answer and an "Answer" action, and question 2 with the answer and a "Change answer" action

#### Scenario: Nothing asked
- **WHEN** the Santa has not asked anything
- **THEN** the screen says no questions have been asked yet

#### Scenario: No sign of the Santa
- **WHEN** the screen is shown in an Active group
- **THEN** it contains no name, username or other hint of who the Santa is

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the questions and the person's answers are shown and no answer can be given or changed

### Requirement: Answering a question
While the group is Active the person SHALL be able to answer a question, or change their answer, in a bottom sheet filled with the current answer; a successful `PUT /groups/{id}/santa/questions/{number}/answer` SHALL close the sheet and show the new answer. An answer is 1 to 2000 characters after trimming.

#### Scenario: Answer a question
- **WHEN** the person opens the answer sheet for a question, enters text and saves
- **THEN** the sheet closes and the question shows the answer

#### Scenario: Change an answer
- **WHEN** the person opens the sheet for an answered question
- **THEN** it is filled with the current answer and saving replaces it

#### Scenario: Blank or too long
- **WHEN** the person saves a blank answer or one over 2000 characters
- **THEN** nothing is sent and the sheet shows what is wrong next to the field

#### Scenario: Question gone or group archived while answering
- **WHEN** the server answers that the question does not exist, or refuses because the group is no longer Active
- **THEN** the sheet closes, the screen reloads and says what happened

### Requirement: Your Santa
In an Archived group the group page SHALL offer "Your Santa", which opens a screen loaded with `GET /groups/{id}/santa` showing the Santa's username and full name when it has one. The entry SHALL NOT exist while the group is Pending or Active.

#### Scenario: Reveal after archiving
- **WHEN** the group is Archived and the person opens "Your Santa"
- **THEN** the screen shows the Santa's username and full name

#### Scenario: No reveal before archiving
- **WHEN** the group is Active
- **THEN** the group page has no "Your Santa" entry

#### Scenario: Revealed in the meantime
- **WHEN** the server refuses because the group is not Archived
- **THEN** the screen says the Santa is revealed once the group is archived

### Requirement: Loading, errors and refreshing
Every screen of this capability SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached or answers with an error, and SHALL keep what it already shows while a refresh fails. Each screen SHALL offer Refresh and SHALL reload when the app returns to the foreground.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while a screen loads
- **THEN** the screen says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while questions are shown
- **THEN** the questions stay on screen with the error message
