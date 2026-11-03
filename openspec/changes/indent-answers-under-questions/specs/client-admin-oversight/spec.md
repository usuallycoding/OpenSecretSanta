## ADDED Requirements

### Requirement: Pair thread uses the same layout
In the pair thread each question SHALL show its number in a small filled circle, and each answer SHALL be indented under its question and joined to the circle by a line, and an unanswered question SHALL show the line with "Not answered yet" in a muted colour, as the Gifter and Receiver pages show them.

#### Scenario: Answered question in the thread
- **WHEN** the thread has a question with an answer
- **THEN** the question's number is in a circle and the answer is indented under the question and joined to the circle by a line

#### Scenario: Unanswered question in the thread
- **WHEN** the thread has a question with no answer
- **THEN** it shows the line and "Not answered yet" in a muted colour
