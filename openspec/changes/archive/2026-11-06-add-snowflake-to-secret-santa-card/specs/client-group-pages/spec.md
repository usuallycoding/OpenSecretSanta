# Spec Delta

## ADDED Requirements

### Requirement: The Secret Santa card has a snowflake
The "You're Secret Santa for:" card SHALL have a snowflake in its top right corner, in a soft colour that suits the light and the dark theme. It SHALL be purely decorative: it SHALL NOT be read by a screen reader or take a tap, SHALL NOT cover any text of the card, and SHALL NOT change what the card says or how tall it is. The "Your stuff:" card SHALL NOT have one.

#### Scenario: Every state
- **WHEN** the card is shown in a Pending, an Active or an Archived group
- **THEN** a snowflake is in its top right corner

#### Scenario: In the corner whatever the content
- **WHEN** the card is narrow because it has no buttons, as while Pending, and when it has buttons
- **THEN** the snowflake is at the card's right edge in both, not at the edge of its text

#### Scenario: Whatever the snow setting
- **WHEN** "Make it snow" is on, and when it is off
- **THEN** the snowflake is on the card either way

#### Scenario: Light and dark
- **WHEN** the app is light, then dark
- **THEN** the snowflake can be seen in both and does not compete with the name

#### Scenario: A long username
- **WHEN** the receiver's username is too long for one line
- **THEN** it wraps or is cut before it reaches the snowflake, and the snowflake covers none of it

#### Scenario: Screen reader and touch
- **WHEN** a screen reader moves through the card, or the person taps the snowflake
- **THEN** it reads nothing about the snowflake and the tap does nothing

#### Scenario: The other card
- **WHEN** the group page shows both cards
- **THEN** only the "You're Secret Santa for:" card has a snowflake
