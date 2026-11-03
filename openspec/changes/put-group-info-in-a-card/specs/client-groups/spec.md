## ADDED Requirements

### Requirement: Group information is shown in a card
On the group page the group's description, date and time and location SHALL be shown together in one card, in that order, below the name and the state with its note and above the pair rows. Parts the group does not have SHALL be left out, and a group with none of them SHALL show no card.

#### Scenario: All three present
- **WHEN** a group has a description, the date `2026-12-24`, the time `18:30` and the location "Grandma's house"
- **THEN** one card shows the description, "When? - 24 Dec 2026, 18:30" and "Where? - Grandma's house", in that order

#### Scenario: Parts left out
- **WHEN** a group has a location and nothing else of the three
- **THEN** the card shows only "Where? - " and the location

#### Scenario: Nothing to show
- **WHEN** a group has no description, no date, no time and no location
- **THEN** no card is shown

### Requirement: The card labels the date and the location
In the card the date and time SHALL be on a line reading "When? - " followed by the date and time as `Dates and times are shown as the group has them` describes, and the location on a line reading "Where? - " followed by the location, each label in a semi-bold weight.

#### Scenario: Date without a time
- **WHEN** a group has a date and no time
- **THEN** the card's date line reads "When? - " followed by the date alone

#### Scenario: Screen reader
- **WHEN** a screen reader reads the card
- **THEN** it reads the description with its "Description" label and the two lines with their labels

### Requirement: Only the information is in the card
The name, the state and its note, the pair rows and the participant chips SHALL NOT be in the card.

#### Scenario: Name and state stay outside
- **WHEN** a group page is shown
- **THEN** the name, the state and its note are above the card and the pair rows and chips are below it, none of them inside it
