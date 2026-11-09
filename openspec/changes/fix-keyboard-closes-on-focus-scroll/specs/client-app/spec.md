## MODIFIED Requirements

### Requirement: Dragging dismisses the keyboard
A drag by the person that scrolls a form or a bottom sheet SHALL close the keyboard. Scrolling the app does itself to bring a field into view SHALL NOT close it, whatever the size of the screen, and a drag that only scrolls the text inside a multi-line field SHALL NOT close it.

#### Scenario: Drag a form
- **WHEN** the keyboard is open on the Delivery Info page and the person drags the form
- **THEN** the form scrolls and the keyboard closes

#### Scenario: Field brought into view
- **WHEN** the person taps a field that is partly under the keyboard and the form scrolls to show it
- **THEN** the keyboard stays open

#### Scenario: Short screen
- **WHEN** the person taps any field of the Delivery Info page on a short screen, such as a phone in landscape, where the keyboard leaves room for little more than the field
- **THEN** the form scrolls to show the field and the keyboard stays open

#### Scenario: Drag inside a field
- **WHEN** the person drags inside a multi-line field that is scrolling its own text
- **THEN** the keyboard stays open

### Requirement: Swiping anywhere closes the keyboard
On every screen that is not a bottom sheet or a dialog, a swipe or drag in any direction outside the keyboard SHALL close the keyboard, wherever on the screen it starts, including on a list that scrolls under it. A drag that a text field uses for its own text, moving the cursor, selecting or scrolling it, SHALL NOT close it. A scroll the app does itself, with no finger on the screen, SHALL NOT close it.

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

#### Scenario: Scroll the app does itself
- **WHEN** the keyboard opens for a field and the screen scrolls on its own to keep the field in view, with no finger on the screen
- **THEN** the keyboard stays open and the field keeps the focus
