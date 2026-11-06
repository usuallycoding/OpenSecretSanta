# Spec Delta

## ADDED Requirements

### Requirement: Cards have a snowflake pattern
Every card in the app SHALL have a faint pattern of snowflakes behind its content, in the signed-in screens, the sheets and the owner's screens alike, whether or not the card can be pressed. The pattern SHALL be purely decorative: it SHALL NOT be read by a screen reader or take a tap, and SHALL NOT change what a card says, how tall or wide it is, or how its content is laid out. It SHALL NOT depend on "Make it snow".

#### Scenario: Every card
- **WHEN** a group row, an admin row, the "You're Secret Santa for:" card, the "Your stuff:" card, a wishlist item, a question or a card in a sheet is shown
- **THEN** it has the pattern behind its content

#### Scenario: Whatever the snow setting
- **WHEN** "Make it snow" is on, and when it is off
- **THEN** the cards have the pattern either way

#### Scenario: Behind the content
- **WHEN** a flake lies under a heading, a name, a line of text or a button
- **THEN** the content is drawn over it, a button hides it, and the text is as readable as without the pattern

#### Scenario: Layout unchanged
- **WHEN** a card is shown with and without the pattern in the same state
- **THEN** it is the same size, and its content is at the same place

#### Scenario: Screen reader and touch
- **WHEN** a screen reader moves through a card, or the person taps a flake
- **THEN** no flake is read, and the tap does what it does anywhere else on the card

### Requirement: The pattern looks like scattered snow
The snowflakes of a card SHALL be of several different shapes, of different sizes and turned at different angles, spread over the whole card rather than bunched, and some SHALL be cut off by the card's edge. A larger card SHALL have more of them, so that the density is about the same on every card. They SHALL be drawn in a soft colour of the theme, faint enough not to compete with the content, on the light and the dark theme.

#### Scenario: Variety
- **WHEN** a large card is looked at
- **THEN** it shows more than one snowflake shape, in more than one size and angle

#### Scenario: Cut off by the edge
- **WHEN** a card is looked at
- **THEN** some snowflakes may be only partly inside it, cut off at its edge or corner, and none is drawn outside the card

#### Scenario: Small and large cards
- **WHEN** a thin list row and a tall card are looked at
- **THEN** the row has a few snowflakes and the tall card more, at about the same density

#### Scenario: Light and dark
- **WHEN** the app is light, then dark
- **THEN** the pattern can be seen in both and does not compete with the text

### Requirement: A card keeps its pattern
A card SHALL show the same pattern every time it is shown for the same thing, including after it scrolls out of view and back, when the list around it is refreshed and when the theme changes, while two different cards SHALL NOT show the same pattern. The pattern SHALL NOT change as the card is redrawn.

#### Scenario: Scrolling
- **WHEN** a card in a list scrolls out of view and back
- **THEN** its snowflakes are where they were

#### Scenario: Refreshing
- **WHEN** a list is refreshed and a card in it is unchanged
- **THEN** its pattern does not change

#### Scenario: Different cards
- **WHEN** two cards in a list are looked at side by side
- **THEN** their patterns differ
