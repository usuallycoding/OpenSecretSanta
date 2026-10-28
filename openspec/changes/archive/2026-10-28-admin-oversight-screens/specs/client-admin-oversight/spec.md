# Spec Delta

## ADDED Requirements

### Requirement: The draw
The detail of an Active or Archived group SHALL list every Santa and receiver pair, loaded with `GET /admin/groups/{id}/assignments`, and SHALL mark the administrator's own username with "you" wherever it appears. A group that is Draft or Pending SHALL show no such section and the app SHALL NOT request it.

#### Scenario: Pairs shown
- **WHEN** an Active group has `alice` giving to `bob` and `bob` giving to `carol`
- **THEN** both pairs are listed

#### Scenario: Owner's own pair
- **WHEN** the administrator is a participant in the group
- **THEN** their username is marked "you" in the pair where they are the Santa and in the pair where they are the receiver

#### Scenario: Not yet drawn
- **WHEN** the group is Pending
- **THEN** there is no draw section and no assignments request is made

#### Scenario: Draw cannot be loaded
- **WHEN** the assignments request fails while the group detail is shown
- **THEN** the section shows the problem and the rest of the detail stays

### Requirement: Participant wishlist
Choosing a participant on the detail of a Pending, Active or Archived group SHALL open that person's wishlist for the group, loaded with `GET /admin/groups/{id}/participants/{username}/wishlist`, read-only and in the order the participant chose, showing for each item its title and, when set, its note and link, and when it was added or last changed. A link SHALL open in the browser. The screen SHALL offer no way to add, change, delete or reorder items.

#### Scenario: List shown in order
- **WHEN** the participant has three items in a chosen order
- **THEN** the three are shown in that order

#### Scenario: Empty list
- **WHEN** the participant has no items
- **THEN** the screen says the list is empty

#### Scenario: Read-only
- **WHEN** the wishlist is shown
- **THEN** there is no control that adds, edits, deletes or moves an item

#### Scenario: Link opens
- **WHEN** the owner chooses an item's link
- **THEN** it opens in the browser

### Requirement: All question threads
An Active or Archived group's detail SHALL offer a Questions screen, loaded with `GET /admin/groups/{id}/questions`, showing one thread per Santa and receiver pair that names both people, and lists every question in order with its number, text and time asked and, when answered, the answer with when it was first given and when it was last changed, or "Not answered yet". It SHALL offer no way to ask, answer or change anything. A Draft or Pending group SHALL NOT offer it.

#### Scenario: Threads shown
- **WHEN** the group has threads for three pairs
- **THEN** three threads are shown, each naming its Santa and receiver

#### Scenario: Unanswered question
- **WHEN** a question has no answer
- **THEN** it shows "Not answered yet"

#### Scenario: Changed answer
- **WHEN** an answer was changed after it was first given
- **THEN** both the first and the last-changed times are shown

#### Scenario: Thread with no questions
- **WHEN** a Santa has asked nothing yet
- **THEN** the thread is shown with a note that nothing has been asked

#### Scenario: Read-only
- **WHEN** the Questions screen is shown, whatever the group's state
- **THEN** there is no control that asks, answers or changes a question

### Requirement: Reading is not recorded
The oversight screens SHALL only read from the server and SHALL keep nothing of what they show once they are left.

#### Scenario: Only reads
- **WHEN** the owner opens the draw, a wishlist and the questions
- **THEN** the app sends only GET requests for them

### Requirement: Oversight errors and refreshing
Each oversight screen SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached, SHALL keep what it shows while a refresh fails, offer Refresh and reload when the app returns to the foreground. When the server answers 404 the screen SHALL say the group or participant is no longer available and offer to go back; when it answers 409 `invalid_state` it SHALL say the group is not in a state where this can be read and offer to go back.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while the questions load
- **THEN** the screen says so and offers "Try again"

#### Scenario: Participant removed
- **WHEN** the server answers 404 for a participant's wishlist
- **THEN** the screen says the participant is no longer available and offers to go back

#### Scenario: State changed
- **WHEN** the server answers 409 `invalid_state` because the group is no longer in a readable state
- **THEN** the screen says so and offers to go back
