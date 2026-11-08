# Spec Delta

## MODIFIED Requirements

### Requirement: Admin group details are shown in a card
On the admin group screen the group's name SHALL be centered horizontally, the group's description SHALL be shown in a card of its own above the card, and the date and time, location and budget in the card, with the same icons, as `client-groups` describes for the group page, below the name and the state with its note. The card SHALL always be shown, with `TBD` in place of a value the group does not have, and, outside a Draft group, SHALL have the Group Notifications icon at its end. The actions of the group (edit, announce, the next step and delete) SHALL be below the card and SHALL NOT be in it.

#### Scenario: Details in the card
- **WHEN** an administrator opens a group that has a description, a date and time, a location and a budget
- **THEN** the description is in a card of its own above the card, and the card shows the calendar icon with the date and time, the map pin icon with the location and the bank icon with the budget and its currency

#### Scenario: Actions stay below
- **WHEN** the screen shows the card and the group has actions
- **THEN** the buttons are below the card and none is inside it

#### Scenario: Nothing to show
- **WHEN** a group has no description, date, time, location or budget
- **THEN** the card is shown with its three lines, each with its icon and `TBD`, and the actions follow the card

#### Scenario: The messages icon
- **WHEN** the owner opens a Pending, an Active or an Archived group
- **THEN** the card has the Group Notifications icon at its end, and a Draft group's card has none
