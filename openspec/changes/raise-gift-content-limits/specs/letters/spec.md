# Spec Delta

## MODIFIED Requirements

### Requirement: Send a letter
The server SHALL expose `POST /groups/{id}/santa/letters` letting a participant of an Active group send their Santa a letter with a title of 1 to 100 characters and a text of 1 to 2000 characters, both after trimming. Each receiver SHALL be able to send up to 200 letters per group, numbered from 1, and the response SHALL be 201 with the letter. The route and its body SHALL contain no username.

#### Scenario: A letter is sent
- **WHEN** `bob`, whose Santa is `alice`, sends the title "Hello" and the text "Thank you for the gifts" in an Active group
- **THEN** the response is 201 with letter number 1, its title, text and time

#### Scenario: Numbering
- **WHEN** `bob` sends a second letter
- **THEN** it is number 2

#### Scenario: Title and text are both required
- **WHEN** `bob` sends a blank title, a blank text, a title of 101 characters or a text of 2001 characters
- **THEN** the response is 400 with code `validation_failed` naming `title` or `text`, and nothing is stored

#### Scenario: Limit
- **WHEN** `bob` has sent 200 letters in the group and sends another
- **THEN** the response is 409 with code `invalid_state` and nothing is stored

#### Scenario: Only while Active
- **WHEN** a participant sends a letter in a Pending or an Archived group
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Not a participant
- **WHEN** someone who is not a participant of the group sends a letter
- **THEN** the response is 404
