# Spec Delta

## ADDED Requirements

### Requirement: Respond to Santa counts unanswered questions
While the group is Active and at least one question from the Santa has no answer, the "Respond to Santa" button of the "Your stuff:" card SHALL carry a badge on its corner in the theme's tertiary colour with the number of such questions, "9+" when there are more than 9. With none, or in a Pending or an Archived group, it SHALL carry no badge. A screen reader SHALL hear the number with the button's name and SHALL NOT read the badge on its own.

#### Scenario: Some unanswered
- **WHEN** the group is Active and the Santa has asked three questions of which the person answered one
- **THEN** the "Respond to Santa" button has a badge showing 2

#### Scenario: Back from answering
- **WHEN** the person answers a question on the Receiver page and returns to the group page
- **THEN** the badge shows the lower number, and is gone when none is left

#### Scenario: More than nine
- **WHEN** twelve questions have no answer
- **THEN** the badge shows "9+"

#### Scenario: None, Pending or Archived
- **WHEN** every question is answered, or the group is Pending or Archived
- **THEN** the button has no badge

#### Scenario: Screen reader
- **WHEN** a screen reader reads the button while two questions are unanswered
- **THEN** it reads "Respond to Santa, 2 unanswered" and the badge is not read on its own

#### Scenario: Layout
- **WHEN** the badge is shown on the button
- **THEN** the buttons on the row keep their size and place, and the badge is not cut off by the card

### Requirement: The group page counts from the Santa's questions
The group page SHALL count the unanswered questions from `GET /groups/{id}/santa/questions` while the group is Active, and SHALL reload them whenever the page is shown again, is pulled to refresh or returns to the foreground. While they load, or when loading them fails, the count is unknown: no badge SHALL be shown and the page SHALL show no message about it.

#### Scenario: Not loaded or failed
- **WHEN** the questions are still loading, or the request to load them failed
- **THEN** no badge is shown and the page shows no message about it

#### Scenario: Reloaded when shown again
- **WHEN** the person returns to the group page after the Santa asked another question
- **THEN** the badge counts it once the questions have reloaded
