## ADDED Requirements

### Requirement: Admin group details are shown in a card
On the admin group screen the group's description, date and time and location SHALL be shown in the same card, with the same "When? - " and "Where? - " lines, as `client-groups` describes for the group page, below the name and the state with its note. The actions of the group (edit, announce, the next step and delete) SHALL be below the card and SHALL NOT be in it.

#### Scenario: Details in the card
- **WHEN** an administrator opens a group that has a description, a date and time and a location
- **THEN** the card shows the description, "When? - " with the date and time, and "Where? - " with the location

#### Scenario: Actions stay below
- **WHEN** the screen shows the card and the group has actions
- **THEN** the buttons are below the card and none is inside it

#### Scenario: Nothing to show
- **WHEN** a group has no description, date, time or location
- **THEN** no card is shown and the actions follow the state directly
