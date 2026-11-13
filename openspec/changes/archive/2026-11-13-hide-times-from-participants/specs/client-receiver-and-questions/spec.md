# Spec Delta

## MODIFIED Requirements

### Requirement: Unanswered questions are marked
While the group is Active, a question from the Santa that has no answer SHALL show a pill reading "Unanswered" under the question and an accent bar along the start edge of its card, in the theme's tertiary colour. It SHALL keep "Not answered yet" in the answer's place. An answered question, and any question in a Pending or an Archived group, SHALL look as before. The Gifter page SHALL NOT mark anything.

#### Scenario: Unanswered question in an Active group
- **WHEN** the group is Active and the Santa's question has no answer
- **THEN** its card has the "Unanswered" pill, the accent bar on its start edge and "Not answered yet" as before

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
