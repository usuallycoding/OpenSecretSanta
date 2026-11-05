# Spec Delta

## MODIFIED Requirements

### Requirement: Creating a group
"New group" SHALL open a form with name, description, location, budget, currency, date and time. A successful `POST /admin/groups` SHALL open the new group, which is a Draft. A field the server refuses SHALL show its message next to the field.

#### Scenario: Create a group
- **WHEN** the administrator enters a name and saves
- **THEN** the group is created as a Draft and its detail opens

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while saving
- **THEN** the form keeps what was typed and says so

### Requirement: The group form checks what the server checks
Before anything is sent the form SHALL check every field exactly as the server does: a name of 1 to 100 characters, a description of up to 2000, a location of up to 200, a budget that is a whole number from 50 to 10000, a currency of up to 20, a real calendar date and a real time of day, with blanks taken as empty. A failed check SHALL show its message next to the field.

#### Scenario: Name required
- **WHEN** the administrator saves with a blank name
- **THEN** nothing is sent and the form says the name is required

#### Scenario: Invalid date or time
- **WHEN** the form holds a date or time that is not real, such as `2026-13-45` or `25:99`, which no picker can produce
- **THEN** nothing is sent and the form says what format is expected next to that field

#### Scenario: Blank means empty
- **WHEN** the administrator leaves description, location, budget, date and time blank
- **THEN** the group is saved without them

#### Scenario: Budget out of range
- **WHEN** the form holds a budget of 49, of 10001 or of `abc`
- **THEN** nothing is sent and the form says next to the budget field that it must be a whole number from 50 to 10000

### Requirement: Duplicating a group
Choosing "Duplicate" on a group SHALL ask for confirmation, naming the group. Confirming SHALL create a Draft with `POST /admin/groups` having the original's description, location, budget, currency, date, time and participants and the name "Copy of " followed by the original's name, and SHALL open the new group.

#### Scenario: Duplicate an Active group
- **WHEN** the administrator confirms "Duplicate" on an Active group "Party" with participants `alice` and `bob`
- **THEN** a Draft "Copy of Party" with the same details and participants `alice` and `bob` is created and its detail opens, and "Party" is still Active

#### Scenario: Cancel
- **WHEN** the administrator cancels the confirmation
- **THEN** nothing is created

#### Scenario: A long name
- **WHEN** the original's name is 100 characters long
- **THEN** the copy's name is 100 characters long, starts with "Copy of " and ends with an ellipsis

#### Scenario: Participants without an account
- **WHEN** the original has a participant who has no account yet
- **THEN** the copy lists that username too

#### Scenario: Failure
- **WHEN** the server cannot be reached, or refuses the new group
- **THEN** the dialog says so, nothing is created and the list stays as it was

#### Scenario: Budget is copied
- **WHEN** the administrator confirms "Duplicate" on a group with the budget 100 and the currency `EUR`
- **THEN** the copy has the budget 100 and the currency `EUR`

### Requirement: Admin group details are shown in a card
On the admin group screen the group's description, date and time, location and budget SHALL be shown in the same card, with the same "When? - ", "Where? - " and "Budget? - " lines, as `client-groups` describes for the group page, below the name and the state with its note. The actions of the group (edit, announce, the next step and delete) SHALL be below the card and SHALL NOT be in it.

#### Scenario: Details in the card
- **WHEN** an administrator opens a group that has a description, a date and time, a location and a budget
- **THEN** the card shows the description, "When? - " with the date and time, "Where? - " with the location and "Budget? - " with the budget and its currency

#### Scenario: Actions stay below
- **WHEN** the screen shows the card and the group has actions
- **THEN** the buttons are below the card and none is inside it

#### Scenario: Nothing to show
- **WHEN** a group has no description, date, time, location or budget
- **THEN** no card is shown and the actions follow the state directly

## ADDED Requirements

### Requirement: The group form has a budget
The group form SHALL have a "Budget (optional)" field that takes digits only and a "Currency" field that starts with `RON` on a new group and with the group's own currency when editing one that has a budget. Clearing the budget SHALL save the group without a budget and without a currency.

#### Scenario: New group
- **WHEN** the administrator opens the form for a new group
- **THEN** the budget is empty and the currency reads `RON`

#### Scenario: Edit a budget
- **WHEN** the administrator opens an Active group with the budget 100 and the currency `EUR`, sets the budget to 150 and saves
- **THEN** the detail shows "Budget? - 150 EUR"

#### Scenario: Clear the budget
- **WHEN** the administrator empties the budget field and saves
- **THEN** the group has no budget and the detail shows no budget line

#### Scenario: Letters are not accepted
- **WHEN** the administrator types letters in the budget field
- **THEN** the field keeps only the digits

