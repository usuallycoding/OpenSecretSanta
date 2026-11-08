# Spec Delta

## MODIFIED Requirements

### Requirement: Only the information is in the card
The name, the state and its note, the description, the Secret Santa and Your stuff cards and the participant chips SHALL NOT be in the card. The card SHALL hold the three information lines and, at its end, the Announcements icon, and nothing else.

#### Scenario: Name and state stay outside
- **WHEN** a group page is shown
- **THEN** the name, the state and its note and the description are above the card and the two cards and the chips are below it, none of them inside it

#### Scenario: The icon is inside
- **WHEN** a Pending, an Active or an Archived group's page is shown
- **THEN** the Announcements icon is in the card, at its end, and the three lines are in it too

## ADDED Requirements

### Requirement: A group's entry shows its new announcements
An entry of the Groups tab SHALL show, at the end of its name row and before the state, a small bell icon followed by the number of announcements the person has not seen, as `GET /groups` says it: the number, "9+" above 9. A group with none new, and a group whose number is not known, SHALL show no bell. A screen reader SHALL hear the entry's new announcements, for example "3 new announcements". The number SHALL be gone once the person has seen the announcements and the list has been shown again.

#### Scenario: New announcements
- **WHEN** a group has three announcements the person has not seen
- **THEN** its entry shows the bell and 3 before its state

#### Scenario: Nothing new
- **WHEN** the person has seen every announcement of a group
- **THEN** its entry shows no bell

#### Scenario: More than nine
- **WHEN** twelve announcements of a group are new
- **THEN** its entry shows the bell and "9+"

#### Scenario: Back from reading them
- **WHEN** the person reads a group's announcements and goes back to the list
- **THEN** its entry no longer shows the bell

#### Scenario: Name and state keep their places
- **WHEN** an entry has a long name and new announcements
- **THEN** the name still takes one row with an ellipsis, and the bell and the state stay visible
