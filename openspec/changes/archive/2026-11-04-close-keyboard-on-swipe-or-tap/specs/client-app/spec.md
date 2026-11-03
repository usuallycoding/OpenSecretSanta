## ADDED Requirements

### Requirement: Swiping anywhere closes the keyboard
On every screen that is not a bottom sheet or a dialog, a swipe or drag in any direction outside the keyboard SHALL close the keyboard, wherever on the screen it starts, including on a list that scrolls under it. A drag that a text field uses for its own text, moving the cursor, selecting or scrolling it, SHALL NOT close it.

#### Scenario: Swipe a list
- **WHEN** the keyboard is open for the search field of the Groups tab and the person swipes up on the list
- **THEN** the list scrolls and the keyboard closes

#### Scenario: Swipe sideways
- **WHEN** the keyboard is open for the users search and the person swipes sideways on empty space
- **THEN** the keyboard closes

#### Scenario: Search of participants
- **WHEN** the keyboard is open for the participants search of a group and the person swipes anywhere on the page
- **THEN** the keyboard closes and the page scrolls as the swipe asks

#### Scenario: Drag in the field
- **WHEN** the person drags inside the search field to move the cursor or select text
- **THEN** the keyboard stays open

#### Scenario: Pull to reload
- **WHEN** the keyboard is open and the person pulls a list down to reload it
- **THEN** the list reloads and the keyboard closes

### Requirement: Tapping empty space closes the keyboard
A tap on anything that does not handle it, such as empty space or plain text, SHALL close the keyboard. A tap on a text field SHALL focus it, and a tap on a control SHALL do what the control does.

#### Scenario: Tap beside the field
- **WHEN** the keyboard is open for a search field and the person taps empty space on the screen
- **THEN** the keyboard closes

#### Scenario: Tap another field
- **WHEN** the keyboard is open for one field and the person taps another text field
- **THEN** the other field has the focus and the keyboard stays open

#### Scenario: Tap a control
- **WHEN** the keyboard is open and the person taps a button, a chip or a card
- **THEN** it does its own action

### Requirement: The same rule on the sign-in screens
The sign-in and password-change screens SHALL close the keyboard on a swipe or a tap outside a text field exactly as the screens of the signed-in app do.

#### Scenario: Sign-in
- **WHEN** the keyboard is open for the sign-in fields and the person taps empty space or swipes
- **THEN** the keyboard closes
