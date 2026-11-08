# client-receiver-and-questions Specification

## Purpose

How a participant asks their receiver questions anonymously, reads the answers, and answers the questions their own Santa has asked, without revealing who the Santa is.

## Requirements

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

### Requirement: Loading, errors and refreshing
Every page section and sheet of this capability SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached or answers with an error, and SHALL keep what it already shows while a refresh fails. Each page SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and SHALL reload when the app returns to the foreground.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while a section loads
- **THEN** the section says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while questions are shown
- **THEN** the questions stay on screen with the error message

### Requirement: Questions are numbered in a circle
Wherever a question is shown on the Gifter and Receiver pages, its number SHALL be shown in a small filled circle at the start of the question, and the question's text SHALL follow the circle without repeating the number.

#### Scenario: Numbered question
- **WHEN** the second question of a thread is shown
- **THEN** a circle holding 2 is at its start and its text follows without "2."

### Requirement: Answers are indented under their question
Wherever a question is shown with its answer on the Gifter and Receiver pages, the answer SHALL be indented under the question, in line with the question's text, and joined to the question's circle by a thin vertical line that turns toward the answer.

#### Scenario: Answered question
- **WHEN** a question has an answer
- **THEN** the answer is indented under the question and a line runs from the question's circle to the start of the answer

#### Scenario: Long question
- **WHEN** a question's text takes several lines
- **THEN** the line still starts at the circle and runs down past the whole question to the answer

#### Scenario: Long answer
- **WHEN** an answer takes several lines
- **THEN** the line runs down to the answer's first line only and the whole answer stays inside the card's width

### Requirement: Unanswered questions keep the line
A question without an answer SHALL show the same indent and line, with "Not answered yet" in a muted colour in the answer's place.

#### Scenario: Unanswered question
- **WHEN** a question has no answer
- **THEN** it shows the line from its circle and "Not answered yet" in a muted colour, indented as an answer would be

### Requirement: Actions stay below the thread
The "Answer" and "Change answer" actions SHALL stay at the bottom of the question's card, outside the indented answer, so the layout does not move when a question gets its answer.

#### Scenario: Answer action
- **WHEN** a question the person can answer has no answer
- **THEN** the "Answer" action is at the bottom of the card, not in the indented place

### Requirement: Question and answer stay named for screen readers
The question and the answer SHALL keep their "Question" and "Answer" labels for screen readers, a screen reader SHALL announce the question's number with the question, and the line SHALL be decoration with no label of its own.

#### Scenario: Screen reader
- **WHEN** a screen reader reads an answered question
- **THEN** it announces the question's number and text and the answer with their labels, and announces nothing for the line

### Requirement: Unanswered questions are marked
While the group is Active, a question from the Santa that has no answer SHALL show a pill reading "Unanswered" beside the time it was asked and an accent bar along the start edge of its card, in the theme's tertiary colour. It SHALL keep "Not answered yet" in the answer's place. An answered question, and any question in a Pending or an Archived group, SHALL look as before. The Gifter page SHALL NOT mark anything.

#### Scenario: Unanswered question in an Active group
- **WHEN** the group is Active and the Santa's question has no answer
- **THEN** its card has the "Unanswered" pill beside the asked time, the accent bar on its start edge and "Not answered yet" as before

#### Scenario: Answering removes the marks
- **WHEN** the person saves an answer to a marked question
- **THEN** the pill and the bar are gone and the question shows the answer as any answered question

#### Scenario: Answered question
- **WHEN** a question has an answer
- **THEN** it has no pill and no bar

#### Scenario: Archived group
- **WHEN** the group is Archived and a question never got an answer
- **THEN** it shows "Not answered yet" with no pill and no bar

#### Scenario: Not by colour alone
- **WHEN** a screen reader reads a marked question
- **THEN** it reads that the question is unanswered, and the bar, which is only colour, is not read

#### Scenario: Light and dark
- **WHEN** the app is light, then dark
- **THEN** the pill's text can be read on the pill and the pill and the bar can be seen in both

#### Scenario: The person's own questions
- **WHEN** the Gifter page shows a question the person asked that has no answer
- **THEN** it shows "Not answered yet" and nothing else marks it
