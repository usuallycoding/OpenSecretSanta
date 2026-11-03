## ADDED Requirements

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
