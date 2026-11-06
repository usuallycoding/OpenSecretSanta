# Spec Delta

## ADDED Requirements

### Requirement: The pair sheet shows the letters
Below the thread of questions the pair sheet SHALL show the letters the pair's receiver has sent their Santa, taken from `GET /admin/groups/{id}/letters` for that pair only, newest first, each in a card with its title, its text and when it was sent, under the heading "Letters from" and the receiver's username. With none it SHALL say none have been sent. It SHALL load, fail and retry on its own, apart from the other parts, and SHALL offer no way to send, change, delete or reply.

#### Scenario: Letters shown
- **WHEN** the owner opens the pair sheet of `alice` and `bob` and `bob` has sent two letters
- **THEN** below the questions the sheet shows both letters, the newer first, under "Letters from bob"

#### Scenario: Only that pair
- **WHEN** the group has letters from four receivers
- **THEN** the sheet shows only the letters of the chosen pair's receiver

#### Scenario: None sent
- **WHEN** the receiver has sent no letter
- **THEN** the letters part says none have been sent

#### Scenario: Failure of one part
- **WHEN** the letters cannot be loaded but the wishlist and the questions did
- **THEN** the other parts stay shown and the letters part says what went wrong with "Try again"

#### Scenario: Read-only
- **WHEN** the pair sheet is shown, whatever the group's state
- **THEN** there is no control that sends, changes, deletes or replies to a letter
