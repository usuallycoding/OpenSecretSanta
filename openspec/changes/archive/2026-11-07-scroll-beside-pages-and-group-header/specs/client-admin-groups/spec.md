## MODIFIED Requirements

### Requirement: Admin group details are shown in a card
On the admin group screen the group's name SHALL be centered horizontally, the group's description SHALL be shown in a card of its own above the card, and the date and time, location and budget in the card, with the same icons, as `client-groups` describes for the group page, below the name and the state with its note. The actions of the group (edit, announce, the next step and delete) SHALL be below the card and SHALL NOT be in it.

#### Scenario: Details in the card
- **WHEN** an administrator opens a group that has a description, a date and time, a location and a budget
- **THEN** the description is in a card of its own above the card, and the card shows the calendar icon with the date and time, the map pin icon with the location and the bank icon with the budget and its currency

#### Scenario: Actions stay below
- **WHEN** the screen shows the card and the group has actions
- **THEN** the buttons are below the card and none is inside it

#### Scenario: Nothing to show
- **WHEN** a group has no description, date, time, location or budget
- **THEN** no card is shown and the actions follow the state directly
