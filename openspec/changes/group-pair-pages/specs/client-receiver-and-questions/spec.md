# Spec Delta

## MODIFIED Requirements

### Requirement: Your questions to the receiver
The Gifter page SHALL list the person's own questions to the receiver, loaded with `GET /groups/{id}/receiver/questions`, in the order asked and numbered, each with the receiver's answer when there is one and "Not answered yet" when there is not. A question SHALL NOT be editable or deletable.

#### Scenario: Questions and answers in order
- **WHEN** the person has asked two questions and the receiver answered the first
- **THEN** question 1 is shown with its answer and question 2 with "Not answered yet"

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the questions and answers are shown and there is no way to ask

### Requirement: Questions from your Santa
In an Active or Archived group the Receiver page SHALL list, below the wishlist, the questions the person's Santa asked, loaded with `GET /groups/{id}/santa/questions`, numbered and in order, each with the person's own answer when there is one. Nothing on the page SHALL identify the Santa. When the Santa has asked nothing the section SHALL say so.

#### Scenario: Questions shown
- **WHEN** the Santa has asked two questions and the person answered the second
- **THEN** question 1 is shown without an answer and an "Answer" action, and question 2 with the answer and a "Change answer" action

#### Scenario: Nothing asked
- **WHEN** the Santa has not asked anything
- **THEN** the section says no questions have been asked yet

#### Scenario: No sign of the Santa
- **WHEN** the page is shown in an Active group
- **THEN** it contains no name, username or other hint of who the Santa is

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the questions and the person's answers are shown and no answer can be given or changed

### Requirement: Loading, errors and refreshing
Every page section and sheet of this capability SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached or answers with an error, and SHALL keep what it already shows while a refresh fails. Each page SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and SHALL reload when the app returns to the foreground.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while a section loads
- **THEN** the section says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while questions are shown
- **THEN** the questions stay on screen with the error message

## REMOVED Requirements

### Requirement: Your receiver
**Reason**: "Your receiver" is no longer a screen; the receiver's postal details are in the sheet opened from the receiver's username, and the questions are on the Gifter page.
**Migration**: See `User details sheets` and `Gifter page` in `client-group-pages`.

### Requirement: Your Santa
**Reason**: "Your Santa" is no longer an entry or a screen; the reveal is a sheet opened from "Secret Santa" once the group is Archived.
**Migration**: See `User details sheets` in `client-group-pages`.
