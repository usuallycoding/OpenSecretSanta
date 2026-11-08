# letters Specification

## Purpose
Lets a gift receiver write one-way letters to their Secret Santa while a group is Active, with a title and a text, and lets that Santa read them, without either side learning anything the draw keeps hidden.

## Requirements

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

### Requirement: Read the letters you sent
The server SHALL expose `GET /groups/{id}/santa/letters` returning the letters the participant has sent their Santa in an Active or Archived group, in the order they were sent, each with its number, title, text and time sent. It SHALL return nothing about the Santa.

#### Scenario: Own letters
- **WHEN** `bob` has sent two letters and reads his letters
- **THEN** the response lists both, in order, with number, title, text and time

#### Scenario: Nothing sent
- **WHEN** `bob` has sent none
- **THEN** the response is 200 with an empty list

#### Scenario: Archived
- **WHEN** the group is Archived
- **THEN** the letters can still be read, unchanged

#### Scenario: Before the draw
- **WHEN** a participant of a Pending group reads their letters
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: No sign of the Santa
- **WHEN** `bob` reads his letters
- **THEN** the response contains no username and nothing that identifies his Santa

### Requirement: Read the letters from your receiver
The server SHALL expose `GET /groups/{id}/receiver/letters` returning the letters the participant's receiver has sent them in an Active or Archived group, in the order they were sent, with the same fields as the receiver's own view and no username.

#### Scenario: Inbox
- **WHEN** `bob` has sent two letters to his Santa `alice` and `alice` reads her letters from her receiver
- **THEN** the response lists both, in order, with number, title, text and time

#### Scenario: Only your receiver's
- **WHEN** `carol`, who is neither `bob`'s Santa nor in a pair with him, reads her letters from her receiver
- **THEN** she sees only what her own receiver sent

#### Scenario: Nothing yet
- **WHEN** the receiver has sent none
- **THEN** the response is 200 with an empty list

#### Scenario: Archived
- **WHEN** the group is Archived
- **THEN** the letters can still be read

#### Scenario: Before the draw
- **WHEN** a participant of a Pending group reads their letters from their receiver
- **THEN** the response is 409 with code `invalid_state`

### Requirement: Letters are one way and final
A letter SHALL NOT be changed, deleted or answered through any route. A Santa SHALL have no way to reply to a letter, and the receiver SHALL have no way to learn who their Santa is from one.

#### Scenario: No change route
- **WHEN** a receiver, a Santa or the owner looks for a way to edit or delete a letter, or to reply to one
- **THEN** none exists

#### Scenario: The Santa stays hidden
- **WHEN** a receiver sends letters and reads them back
- **THEN** nothing they receive says who their Santa is

### Requirement: Letters are private to the pair and the owner
A letter SHALL be readable only by the receiver who sent it, by that receiver's Santa and, read-only through the oversight route of `admin-oversight`, by the owner of the group. No other participant and no other admin SHALL be able to read one, and nobody but the sender SHALL be able to write one. Letters SHALL go with the group when it is deleted.

#### Scenario: Another participant
- **WHEN** a participant who is neither the sender nor their Santa asks for the letters
- **THEN** each route answers only about that person's own pair

#### Scenario: The owner reads
- **WHEN** the group's owner reads the letters through the oversight route
- **THEN** they see every pair's letters, and the participant routes still show them only their own pair's

#### Scenario: Another admin
- **WHEN** an admin who does not own the group asks for its letters through the oversight route
- **THEN** the response is 404 and carries no letter

#### Scenario: Group deleted
- **WHEN** the owner deletes a group that has letters
- **THEN** the letters are gone with it
