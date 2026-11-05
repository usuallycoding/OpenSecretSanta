# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: The card labels the date and the location`
- TO: `### Requirement: The card marks the date and the location with icons`
- FROM: `### Requirement: The card labels the budget`
- TO: `### Requirement: The card marks the budget with an icon`

## MODIFIED Requirements

### Requirement: Group information is shown in a card
On the group page the group's date and time, location and budget SHALL be shown together in one card, in that order, below the name, the state with its note and the description and above the pair rows. Parts the group does not have SHALL be left out, and a group with none of them SHALL show no card.

#### Scenario: All three present
- **WHEN** a group has a description, the date `2026-12-24`, the time `18:30`, the location "Grandma's house" and the budget 100 in `RON`
- **THEN** the description is above the card, and one card shows the date and time, the location and the budget, in that order

#### Scenario: Parts left out
- **WHEN** a group has a budget and nothing else of the three
- **THEN** the card shows only the budget line

#### Scenario: Nothing to show
- **WHEN** a group has no date, no time, no location and no budget, with or without a description
- **THEN** no card is shown

### Requirement: The card marks the date and the location with icons
In the card the date and time SHALL be on a line that starts with a calendar icon, followed by the date and time as `Dates and times are shown as the group has them` describes, and the location on a line that starts with a map pin icon, followed by the location. The icons SHALL take the place of the words "When?" and "Where?", which SHALL NOT be shown.

#### Scenario: Date without a time
- **WHEN** a group has a date and no time
- **THEN** the card's date line is the calendar icon followed by the date alone

#### Scenario: No words
- **WHEN** a group has a date and a location
- **THEN** the card shows no "When?" or "Where?" text, only the icons and the values

#### Scenario: Screen reader
- **WHEN** a screen reader reads the card
- **THEN** it reads the lines as "When? - " and "Where? - " followed by their values, and the icons announce nothing of their own

### Requirement: The card marks the budget with an icon
In the card the budget SHALL be on a line that starts with a bank icon, followed by the budget, a space and the currency, below the location line. The icon SHALL take the place of the word "Budget?", which SHALL NOT be shown. A group without a budget SHALL have no such line.

#### Scenario: Budget line
- **WHEN** a group has the budget 100 and the currency `EUR`
- **THEN** the card's budget line is the bank icon followed by "100 EUR"

#### Scenario: No budget
- **WHEN** a group has no budget
- **THEN** the card has no budget line

#### Scenario: Screen reader
- **WHEN** a screen reader reads the card
- **THEN** it reads the budget line as "Budget? - " followed by the budget and the currency, and the icon announces nothing of its own

### Requirement: Only the information is in the card
The name, the state and its note, the description, the pair rows and the participant chips SHALL NOT be in the card.

#### Scenario: Name and state stay outside
- **WHEN** a group page is shown
- **THEN** the name, the state and its note and the description are above the card and the pair rows and chips are below it, none of them inside it

## ADDED Requirements

### Requirement: The description is shown outside the card
The group's description SHALL be shown above the card, under the name and the state with its note, as plain text and not in a card, labelled "Description" for screen readers. A group without a description SHALL show nothing in its place.

#### Scenario: Description above the card
- **WHEN** a group has a description and a location
- **THEN** the description is shown as text above the card and is not inside it

#### Scenario: Description alone
- **WHEN** a group has a description and no date, time, location or budget
- **THEN** the description is shown and no card is shown

#### Scenario: No description
- **WHEN** a group has no description
- **THEN** nothing is shown between the state and the card
