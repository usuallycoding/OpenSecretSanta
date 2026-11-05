# Spec Delta

## MODIFIED Requirements

### Requirement: Group information is shown in a card
On the group page the group's description, date and time, location and budget SHALL be shown together in one card, in that order, below the name and the state with its note and above the pair rows. Parts the group does not have SHALL be left out, and a group with none of them SHALL show no card.

#### Scenario: All three present
- **WHEN** a group has a description, the date `2026-12-24`, the time `18:30`, the location "Grandma's house" and the budget 100 in `RON`
- **THEN** one card shows the description, "When? - 24 Dec 2026, 18:30", "Where? - Grandma's house" and "Budget? - 100 RON", in that order

#### Scenario: Parts left out
- **WHEN** a group has a budget and nothing else of the four
- **THEN** the card shows only "Budget? - " and the budget

#### Scenario: Nothing to show
- **WHEN** a group has no description, no date, no time, no location and no budget
- **THEN** no card is shown

## ADDED Requirements

### Requirement: The card labels the budget
In the card the budget SHALL be on a line reading "Budget? - " followed by the budget, a space and the currency, with the label in a semi-bold weight, below the location line. A group without a budget SHALL have no such line.

#### Scenario: Budget line
- **WHEN** a group has the budget 100 and the currency `EUR`
- **THEN** the card's budget line reads "Budget? - 100 EUR"

#### Scenario: No budget
- **WHEN** a group has no budget
- **THEN** the card has no budget line

#### Scenario: Screen reader
- **WHEN** a screen reader reads the card
- **THEN** it reads the budget line with its label, like the other lines

