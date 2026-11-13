# Spec Delta

## MODIFIED Requirements

### Requirement: New answers are marked
On the questions page, pane or column of the Gifter side a question whose answer was given or changed after the baseline SHALL show a "New" pill with the answer and its card SHALL have the accent bar, in the theme's tertiary colour. Unanswered questions and answers seen before SHALL look as they do now.

#### Scenario: New answer
- **WHEN** the receiver answered a question after the Santa last looked
- **THEN** the question shows "New" with the answer and the card has the bar

#### Scenario: Changed answer
- **WHEN** the receiver changed an answer the Santa had read
- **THEN** it shows "New" again

#### Scenario: Seen answer
- **WHEN** an answer is older than the Santa's last look
- **THEN** it has no pill and no bar

### Requirement: New letters are marked
On the Inbox page a letter sent after the baseline SHALL show a "New" pill and its card SHALL have the accent bar, in the theme's tertiary colour.

#### Scenario: New letter
- **WHEN** the receiver sent a letter after the Santa last looked
- **THEN** the letter shows "New" and has the accent bar

#### Scenario: Seen letters
- **WHEN** a letter was sent before the Santa's last look
- **THEN** it has no pill and no bar
