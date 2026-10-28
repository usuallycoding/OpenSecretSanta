# Spec Delta

## ADDED Requirements

### Requirement: Forms stay usable with the keyboard open
A form that has a primary button (Profile, Change password, Sign in, and the group form) SHALL lift its content above the keyboard and SHALL scroll, so that the primary button and the field being edited can be reached while the keyboard is open.

#### Scenario: Save above the keyboard
- **WHEN** the person is typing in a field of the Profile tab
- **THEN** the Save button can be scrolled into view above the keyboard

#### Scenario: Small screen
- **WHEN** the Sign in form is taller than the space left by the keyboard
- **THEN** it scrolls and the Sign in button can be reached

### Requirement: Dragging dismisses the keyboard
A drag by the person that scrolls a form or a bottom sheet SHALL close the keyboard. Scrolling the app does itself to bring a field into view SHALL NOT close it, and a drag that only scrolls the text inside a multi-line field SHALL NOT close it.

#### Scenario: Drag a form
- **WHEN** the keyboard is open on the Profile tab and the person drags the form
- **THEN** the form scrolls and the keyboard closes

#### Scenario: Field brought into view
- **WHEN** the person taps a field that is partly under the keyboard and the form scrolls to show it
- **THEN** the keyboard stays open

#### Scenario: Drag inside a field
- **WHEN** the person drags inside a multi-line field that is scrolling its own text
- **THEN** the keyboard stays open

### Requirement: Multi-line fields stop growing at six rows
Every multi-line text field SHALL keep its minimum height while its text is short, grow with its text up to six rows and then stop growing and scroll inside itself.

#### Scenario: Short text
- **WHEN** a multi-line field holds one line
- **THEN** it keeps its minimum height

#### Scenario: Long text
- **WHEN** the text of a multi-line field is longer than six rows
- **THEN** the field is six rows tall and its text scrolls inside it

#### Scenario: Same everywhere
- **WHEN** the address, the delivery information, a wishlist note, a question, an answer and a group description are edited
- **THEN** each stops growing at six rows

### Requirement: Typing in a sheet keeps the cursor
In a text field of a bottom sheet the cursor SHALL stay after the text just typed, on every platform, so that typing "abcd" gives "abcd".

#### Scenario: First character
- **WHEN** the person types "abcd" into an empty field of a bottom sheet
- **THEN** the field holds "abcd" with the cursor after the d

#### Scenario: Every sheet field
- **WHEN** the person types into any field of the wishlist item, question, answer or participants sheet
- **THEN** the cursor behaves the same in each
