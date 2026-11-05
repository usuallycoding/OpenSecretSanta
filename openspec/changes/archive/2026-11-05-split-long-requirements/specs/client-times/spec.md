## MODIFIED Requirements

### Requirement: Participants see when things were made
A participant SHALL see, in one small line under each item or question, when it was made: on their own wishlist items "Added" and, when the item was edited after it was added, "Changed"; on the receiver's wishlist on the Gifter page the same two; on every question on the Gifter and Receiver pages "Asked" and, under an answer, "Answered" and, when the answer was edited after it was first given, "Answer changed".

#### Scenario: Own item
- **WHEN** the person added an item and never edited it
- **THEN** it shows "Added" with the time and nothing about a change

#### Scenario: Own item edited
- **WHEN** the person edited an item after adding it
- **THEN** it shows both "Added" and "Changed" with their times

#### Scenario: The receiver's list
- **WHEN** the Gifter page shows the receiver's wishlist
- **THEN** each item shows when it was added and, if edited, changed

#### Scenario: A question
- **WHEN** a question was asked and has not been answered
- **THEN** it shows "Asked" with the time on both the Gifter and the Receiver page, and no answer time

#### Scenario: An answer
- **WHEN** a question was answered and the answer edited later
- **THEN** it shows "Asked", "Answered" and "Answer changed" with their times

#### Scenario: The owner's sheets
- **WHEN** the owner reads a participant's wishlist or a pair's thread
- **THEN** the same lines are shown in Bucharest time, without "UTC"

#### Scenario: Still the same behaviour
- **WHEN** the person taps an item or reads a cut note
- **THEN** it behaves as before; the time line does nothing

## ADDED Requirements

### Requirement: The owner sees the same times
The owner's wishlist and thread sheets SHALL show the same times in the same way.

#### Scenario: Thread sheet
- **WHEN** the owner opens a pair sheet
- **THEN** each question shows when it was asked and, when answered, when it was answered

### Requirement: The time lines do nothing
These lines SHALL NOT be tappable and SHALL NOT change what tapping the item does.

#### Scenario: Tapping a time line
- **WHEN** a person taps the line under an item
- **THEN** nothing happens
