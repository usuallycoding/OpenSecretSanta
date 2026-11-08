# Spec Delta

## MODIFIED Requirements

### Requirement: Your questions to the receiver
The Gifter page SHALL list the person's own questions to the receiver, loaded with `GET /groups/{id}/receiver/questions`, newest first and numbered, each with the receiver's answer when there is one and "Not answered yet" when there is not. A question SHALL NOT be editable or deletable.

#### Scenario: Questions and answers in order
- **WHEN** the person has asked two questions and the receiver answered the first
- **THEN** question 2 is shown first with "Not answered yet", and question 1 below it with its answer

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the questions and answers are shown and there is no way to ask

### Requirement: Asking a question
While the group is Active the person SHALL be able to ask a question with the floating "Ask a question" button, in a bottom sheet; a successful `POST /groups/{id}/receiver/questions` SHALL close the sheet and show the question at the top of the list. A question is 1 to 1000 characters after trimming and a person asks at most 200 per group; at 200 asking SHALL be unavailable with an explanation.

#### Scenario: Ask a question
- **WHEN** the person opens the ask sheet, enters a question and saves
- **THEN** the sheet closes and the question is the first one in the list

#### Scenario: Empty or too long
- **WHEN** the person saves a blank question or one over 1000 characters
- **THEN** nothing is sent and the sheet shows what is wrong next to the field

#### Scenario: Question limit
- **WHEN** the person has asked 200 questions
- **THEN** asking is unavailable and the screen says the limit is reached

#### Scenario: Group archived while asking
- **WHEN** the person saves a question and the server refuses because the group is no longer Active
- **THEN** the sheet closes, the screen reloads and says the thread can no longer change

#### Scenario: Server unreachable while asking
- **WHEN** the person saves a question and the server cannot be reached
- **THEN** the sheet stays open with what they typed and says so

### Requirement: Questions from your Santa
In an Active or Archived group the Receiver page SHALL list, below the wishlist, the questions the person's Santa asked, loaded with `GET /groups/{id}/santa/questions`, numbered and newest first, each with the person's own answer when there is one. Nothing on the page SHALL identify the Santa. When the Santa has asked nothing the section SHALL say so.

#### Scenario: Questions shown
- **WHEN** the Santa has asked two questions and the person answered the second
- **THEN** question 2 is shown first with the answer and a "Change answer" action, and question 1 below it without an answer and an "Answer" action

#### Scenario: Nothing asked
- **WHEN** the Santa has not asked anything
- **THEN** the section says no questions have been asked yet

#### Scenario: No sign of the Santa
- **WHEN** the page is shown in an Active group
- **THEN** it contains no name, username or other hint of who the Santa is

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the questions and the person's answers are shown and no answer can be given or changed
